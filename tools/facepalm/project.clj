;; IMPORTANT NOTE: Both an RPM and a tarball are generated for this project.
;; Because the release number is not recorded anywhere in the tarball, minor
;; changes need to be recorded in the version number.  Please increment the
;; minor version number rather than the release number for minor changes.
(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))


(defproject org.iplantc/facepalm "4.1.9"
  :description "Command-line utility for DE database managment."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.0"]
                 [cheshire "5.3.1"]
                 [com.cemerick/pomegranate "0.3.0"]
                 [fleet "0.10.1"]
                 [korma "0.4.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.iplantc/clj-jargon "4.1.9"]
                 [org.iplantc/clojure-commons "4.1.9"]
                 [org.iplantc/kameleon "4.1.9"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [slingshot "0.10.3"]
                 [clj-http "1.0.0"]]
  :plugins [[org.iplantc/lein-iplant-cmdtar "4.1.9"]
            [org.iplantc/lein-iplant-rpm "4.1.9"]
            [lein-marginalia "0.7.1"]]
  :iplant-rpm {:summary "Facepalm"
               :type :command}
  :aot :all
  :main facepalm.core
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
