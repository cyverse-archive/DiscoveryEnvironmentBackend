(ns dingle.core
  (:use [dingle.scripting]
        [dingle.git]
        [dingle.services]
        [dingle.jenkins]
        [dingle.scm]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure-commons.file-utils :as ft]
            [dingle.rpms :as rpms]
            [clojure.set :as set]))

(defn err
  [err-str]
  {:error err-str})

(def config (atom nil))

(defn load-configuration
  [config-file]
  (when-not (.exists (io/file config-file))
    (throw+ (err (str "Config " config-file " doesn't exist."))))

  (try+
   (load-file config-file)
   (catch Exception e
     (throw (Exception. "Error loading config file."))))

  (let [config (resolve 'dingle.config/config)]
    (when-not config
      (throw+
       (err
        (str "Couldn't resolve 'dingle.config/config from " config-file))))
    config))

(defn configure
  ([]
     (configure
      (ft/path-join (System/getProperty "user.home") ".dingle/config.clj")))
  ([config-file]
     (reset! config @(load-configuration config-file))
     nil))

(defn full-repo-string
  "Prepends git URL to the name of the repo. Use with list-of-repos."
  [repo]
  (str (:github-base-url @config) repo))

(defn report
  [cmd-map]
  (println (string/join (repeat 80 "-")))
  (println (str "Exit Code: " (:exit cmd-map)))
  (println (str "Stdout: "))
  (println (:out cmd-map))
  (println (str "Stderr: "))
  (println (:err cmd-map)))

(defn report-all
  [cmd-maps]
  (doseq [cmd-map cmd-maps]
    (report cmd-map)))

(defn restart-services
  "Restarts the backend services, one-by-one"
  [host port]
  (remote-execute
   host
   port
   (:ssh-user @config)
   (service-restart "iplant-services" (:sudo-password @config))))

(defn update-services
  "Updates the backend service."
  [host port]
  (let [yu-part (partial yum-update (:sudo-password @config))]
    (remote-execute
     host
     port
     (:ssh-user @config)
     (apply yu-part (:list-of-services @config)))))

(defn merge-workflow
  [repo]
  (execute
   (git-clone repo)
   (git-checkout repo "master")
   (git-merge repo "master" "remotes/origin/dev")
   (git-push repo)))

(defn release-tag-workflow
  [repo tag]
  (execute
    (git-clone repo)
    (git-checkout repo "master")
    (git-tag repo tag)
    (git-push-tags repo)))

(defn tagging-workflow
  "Checks out the repo, merges the dev branch into master, pushes up the
   merged changes, tags the repo with the value in tag, and finally
   pushes up the tags."
  [repo tag]
  (execute
   (git-tag repo tag)
   (git-push-tags repo)))

(defn update-tag-workflow
  [repo tag]
  (execute
   (git-update-tag repo tag)))

(defn merge-prereqs
  []
  (for [repo (mapv full-repo-string (:prereq-repos @config))]
    (merge-workflow repo)))

(defn tag-prereqs
  [tag]
  (for [repo (mapv full-repo-string (:prereq-repos @config))]
    (tagging-workflow repo tag)))

(defn tag-prereqs-with-release
  [tag]
  (let [prereqs (mapv full-repo-string (:prereq-repos @config))
        presult (for [prereq prereqs] (release-tag-workflow prereq tag))]
    (clojure.pprint/pprint presult)))

(defn tag-repos-with-release
  [tag]
  (let [repos   (mapv full-repo-string  (:list-of-repos @config))
        rresult  (for  [repo repos]  (release-tag-workflow repo tag))]
    (clojure.pprint/pprint rresult)))

(defn build-prereqs
  "Build the prereq jobs."
  []
  (for [job (:prereq-jobs @config)]
    (trigger-job
     (:jenkins-url @config)
     job
     (:jenkins-token @config))))

(defn update-tag
  ([tag]
     (update-tag tag (mapv full-repo-string (:list-of-repos @config))))
  ([tag repos]
     (for [repo repos]
       (update-tag-workflow repo tag))))

(defn merge-repos
  ([]
     (merge-repos (mapv full-repo-string (:list-of-repos @config))))
  ([repos]
     (execute (clean))
     (for [repo repos]
       (merge-workflow repo))))

(defn tag-repos
  ([tag]
     (tag-repos tag (mapv full-repo-string (:list-of-repos @config))))
  ([tag repos]
     (for [repo repos]
       (tagging-workflow repo tag))))

(defn list-latest-rpms
  "Lists the filename of the latest version of the RPMs in the :rpm-names
   list in the config.

   Params:
     host - The host to connect to to do the listing. String.
     port - The port to connect on. Integer.
     user - The user to connect as. String.
     rpm-dir - The config key of the directory to look in on the remote machine

   Returns a list of the RPM filenames."
  [rpm-dir]
  (let [full-dir (ft/path-join (:rpm-base-dir @config) (get @config rpm-dir))
        host     (:rpm-host @config)
        port     (:rpm-host-port @config)
        user     (:rpm-host-user @config)]
    (mapv
     #(rpms/latest-rpm host port user %1 full-dir)
     (:rpm-names @config))))

(defn new-repo-rpms
  [rpm-source-dir rpm-dest-dir]
  (into []
    (set/difference
      (set (list-latest-rpms rpm-source-dir))
      (set (list-latest-rpms rpm-dest-dir)))))

(defn rpm-json
  "Returns a string containing JSON for latest RPMs in the QA yum repo.
   WARNING: This will NOT include any RPMs that are needed but not included
   in the yum repo (i.e. haproxy)."
  [dir-key]
  (json/encode
   {:rpm_files
    (dissoc (apply merge
     (mapv
      #(hash-map (:name %1) (rpms/rpm-map->rpm-name %1))
      (list-latest-rpms dir-key)))
     nil)}
   {:pretty true}))

(defn rpm-json-for-qa
  []
  (rpm-json :rpm-qa-dir))

(defn rpm-json-for-stage
  []
  (rpm-json :rpm-stage-dir))

(defn rpm-json-for-prod
  []
  (rpm-json :rpm-prod-dir))

(defn new-qa-rpms
  "Returns a list of rpm maps representing the rpms that are in the dev repo
   but aren't in the qa rpm repo. You shouldn't need to call this directly
   unless you're doing some debugging."
  []
  (new-repo-rpms :rpm-dev-dir :rpm-qa-dir))

(defn new-stage-rpms
  "Returns a list of rpm maps representing the rpms that are in the qa repo
   but aren't in the stage rpm repo. You shouldn't need to call this directly
   unless you're doing some debugging."
  []
  (new-repo-rpms :rpm-qa-dir :rpm-stage-dir))

(defn new-prod-rpms
  "Returns a list of rpm maps representing the rpms that are in the stage repo
   but aren't in the prod rpm repo. You shouldn't need to call this directly
   unless you're doing some debugging."
  []
  (new-repo-rpms :rpm-stage-dir :rpm-prod-dir))

(defn print-new-rpms
  [new-rpm-fn]
  (doseq [rpm-name (for [rpm-map (new-rpm-fn)]
                     (rpms/rpm-map->rpm-name rpm-map))]
    (println rpm-name)))

(defn print-new-qa-rpms
  "Prints off a list of RPMs that are in the dev yum repo but aren't in the QA
   yum repo. You should run this before copying the RPMs over."
  []
  (print-new-rpms new-qa-rpms))

(defn print-new-stage-rpms
  "Prints off a list of RPMs that are in the QA RPMs directory but aren't in the
   Stage RPM directory. You should run this before copying the RPMs over."
  []
  (print-new-rpms new-stage-rpms))

(defn print-new-prod-rpms
  "Prints off a list of RPMs that are in the Stage RPMs directory but aren't in
   the Prod RPM directory. You should run this before copying the RPMs over."
  []
  (print-new-rpms new-prod-rpms))

(defn copy-rpms
  [rpm-list from-dir-sym to-dir-sym]
  (let [host      (:rpm-host @config)
        port      (:rpm-host-port @config)
        user      (:rpm-host-user @config)
        sudo-pass (:sudo-password @config)
        from-dir   (ft/path-join (:rpm-base-dir @config)
                                 (from-dir-sym @config))
        to-dir    (ft/path-join (:rpm-base-dir @config)
                                (to-dir-sym @config))]
    (mapv
     #(rpms/copy-rpm host port user sudo-pass % from-dir to-dir)
     rpm-list)))

(defn copy-rpms-to-qa
  "Copies the RPMs that are in the Dev RPM directory but aren't in the QA RPM
   directory over to the QA RPM directory."
  []
  (copy-rpms (new-qa-rpms) :rpm-dev-dir :rpm-qa-dir))

(defn copy-rpms-to-stage
  "Copies the RPMs that are in the QA RPM directory but aren't in the Stage RPM
   directory over to the Stage RPM directory."
  []
  (copy-rpms (new-stage-rpms) :rpm-qa-dir :rpm-stage-dir))

(defn copy-rpms-to-prod
  "Copies the RPMs that are in the Stage RPM directory but aren't in the Prod
   RPM directory over to the Prod RPM directory."
  []
  (copy-rpms (new-prod-rpms) :rpm-stage-dir :rpm-prod-dir))

(defn update-yum-repo
  [rpm-dir]
  (let [host      (:rpm-host @config)
        port      (:rpm-host-port @config)
        user      (:rpm-host-user @config)
        sudo-pass (:sudo-password @config)
        work-dir  (ft/path-join (:rpm-base-dir @config)
                                (rpm-dir @config))]
    (report-all
     (rpms/createrepo host port user sudo-pass work-dir))

    (report-all
     (rpms/chown-remote-dir host port user sudo-pass work-dir "buildnanny"))))

(defn update-qa-repo
  "Run's 'createrepo --update' on the QA yum repo."
  []
  (update-yum-repo :rpm-qa-dir))

(defn update-stage-repo
  "Run's 'createrepo --update' on the Stage yum repo."
  []
  (update-yum-repo :rpm-stage-dir))

(defn update-prod-repo
  "Run's 'createrepo --update' on the Prod yum repo."
  []
  (update-yum-repo :rpm-prod-dir))

(defn setup-scm
  "Downloads and extracts the scm bundle into the working directory."
  [scm-url]
  (clj-execute
   (script-setup-scm scm-url (:scm-working-dir @config))))

(defn run-export-tool
  "Runs export tools from the latest built scm tarball against a deployed
   version of the DE.

   When this function exits, you should have a screen session named
   'export-tool' running in the background. When that session is complete
   you can run the (run-import-tool) function safely.

   Params:
     scm-url - Full URL to the scm tarball to use.
     source - Version of the source DE."
  [source]
  (future
    (let [de-host     (:de-host @config)
          de-port     (:de-port @config)
          working-dir (:scm-working-dir @config)]
      (clj-execute
       (script-run-export-tool working-dir de-host de-port source)))))

(defn run-import-tool
  "Runs import-tools.py from the latest built scm tarball against a
   deployed version of the DE."
  [dest]
  (let [de-host     (:de-host @config)
        de-port     (:de-port @config)
        working-dir (:scm-working-dir @config)]
    (clj-execute
     (script-run-import-tool working-dir de-host de-port dest))))

(defn run-export-analyses
  "Runs export-analyses.py from the latest built scm tarball against a
   deployed version of the DE."
  [dest]
  (let [de-host     (:de-host @config)
        de-port     (:de-port @config)
        working-dir (:scm-working-dir @config)]
    (clj-execute
     (script-run-export-analyses working-dir de-host de-port dest))))

(defn run-import-analyses
  "Runs import-analyses.py against a deployed version of the DE."
  [dest]
  (let [de-host     (:de-host @config)
        de-port     (:de-port @config)
        working-dir (:scm-working-dir @config)]
    (clj-execute
     (script-run-import-analyses working-dir de-host de-port dest))))

(defn core-list-latest-rpms
  [yum-repo]
  (let [yum-host  (:yum-host @config)
        yum-port  (:yum-port @config)
        yum-user  (:yum-user @config)
        rpm-names (:rpm-names @config)
        all-rpms  (rpms/list-latest-rpms-in-repo yum-host yum-port yum-user yum-repo)]
    (doseq [single-rpm all-rpms]
      (println single-rpm))))

(defn list-latest-rpms-in-dev [] (core-list-latest-rpms (:yum-dev-repo @config)))
(defn list-latest-rpms-in-qa [] (core-list-latest-rpms (:yum-qa-repo @config)))
(defn list-latest-rpms-in-uat [] (core-list-latest-rpms (:yum-uat-repo @config)))
(defn list-latest-rpms-in-prod [] (core-list-latest-rpms (:yum-prod-repo @config)))

(defn core-list-all-rpms
  [yum-repo]
  (let [yum-host  (:yum-host @config)
        yum-port  (:yum-port @config)
        yum-user  (:yum-user @config)
        rpm-names (:rpm-names @config)
        all-rpms  (rpms/list-all-rpms-in-repo yum-host yum-port yum-user yum-repo)]
    (doseq [single-rpm all-rpms]
      (println single-rpm))))

(defn list-all-rpms-in-dev [] (core-list-all-rpms (:yum-dev-repo @config)))
(defn list-all-rpms-in-qa [] (core-list-all-rpms (:yum-qa-repo @config)))
(defn list-all-rpms-in-uat [] (core-list-all-rpms (:yum-uat-repo @config)))
(defn list-all-rpms-in-prod [] (core-list-all-rpms (:yum-prod-repo @config)))
