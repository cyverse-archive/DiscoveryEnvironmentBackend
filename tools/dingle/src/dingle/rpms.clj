(ns dingle.rpms
  (:use dingle.scripting
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(defn exec-list-rpms
  "Uses (remote-execute) to return a listing of RPM information from yum-path."
  [host port user rpm-name yum-path]
  (let [search-str   (str rpm-name "*.rpm")
        query-format "\"%{name}\\t%{version}\\t%{release}\\t%{arch}\\n\""
        remote-exec  (comp :out first (partial remote-execute host port user))]
    (remote-exec
      (scriptify
        (cd ~yum-path)
        (rpm -qp --qf ~query-format ~search-str)))))

(defn list-rpms
  "Access 'host' on 'port' as 'user' and returns a listing of RPM information
   for RPMs that start with 'rpm-name' in the 'yum-path' directory.

   Params:
     host - String containing the hostname to ssh to.
     port - The ssh port for the host. Usually 22. Should be an integer.
     user - The user to ssh in as. Must have passwordless ssh set up. String.
     rpm-name - String containing the name of the rpm to look for.
     yum-path - The path to the directory on host that contains RPMs. String.

   Returns:
     A sequence of maps in the format:
     [
         {
             :name \"RPM name\"
             :version \"RPM version\"
             :release \"RPM release number\"
             :arch \"RPM architecture\"
         }
     ]
  "
  [host port user rpm-name yum-path]
  (let [rpm-listing (exec-list-rpms host port user rpm-name yum-path)]
    (mapv
      #(zipmap [:name :version :release :arch] (string/split % #"\t"))
      (map
        string/trim
        (filter
          #(not (string/blank? %))
          (string/split rpm-listing #"\n"))))))

(defn version-splitter
  [version-str]
  (mapv #(Integer/parseInt %) (string/split version-str #"\.")))

(defn >-version
  "Is version a greater than version b? Maps should be in the format returned
   by (list-rpms)."
  [rpm1 rpm2]
  (let [version1 (conj (version-splitter (:version rpm1))
                       (Integer/parseInt (:release rpm1)))
        version2 (conj (version-splitter (:version rpm2))
                       (Integer/parseInt (:release rpm2)))]
    (loop [v1 version1
           v2 version2]
      (let [e1 (first v1)
            e2 (first v2)]
        (cond
          (and (not e1) e2)  false   ;e1 is shorter and is otherwise =
          (and (not e2) e1)  true    ;e2 is shorter and otherwise =
          (and (not e1)
               (not e2))     false   ;e1 and e1 are same length and =
          (> e1 e2)          true    ;e1 is > than e2
          (< e1 e2)          false   ;duh
          (= e1 e2)          (recur (rest v1) (rest v2)))))))

(defn latest-rpm
  "Uses (list-rpms) to get a list of RPM versions and then returns the map
   containing info on the latest version.

   The params are the same as for (list-rpms).

   The return value is a hash-map in this format:

   {
       :name \"RPM name\"
       :version \"RPM version\"
       :release \"RPM release\"
       :arch \"RPM arch\"
   }
  "
  [host port user rpm-name yum-path]
  (let [rpm-listing (list-rpms host port user rpm-name yum-path)]
    (first (sort >-version rpm-listing))))

(defn rpm-map->rpm-name
  "Takes in a map representing an RPM and turns it into an RPM name."
  [rpm-map]
  (str
    (:name rpm-map)
    "-"
    (:version rpm-map)
    "-"
    (:release rpm-map)
    "."
    (:arch rpm-map)
    ".rpm"))

(defn copy-rpm
  "Copies an RPM from one directory to another on a remote machine.

   Params:
     host - The hostname of the machine to connect to. String.
     port - Integer for the port to connect to.
     user - The username to connect as. String.
     sudo-pass - Sudo password for the user on the remote machine. String.
     rpm-map - Hash map returned from (list-rpms) representing an RPM.
     from-dir - Directory to copy the RPM from. Full path. String.
     to-dir - Directory to copy the RPM to. Full path. String."
  [host port user sudo-pass rpm-map from-dir to-dir]
  (let [rpm-filename (rpm-map->rpm-name rpm-map)
        rpm-from     (ft/path-join from-dir rpm-filename)]
    (remote-execute
      host
      port
      user
      (sudo-cmd
        (scriptify
          (cp ~rpm-from ~to-dir))
        sudo-pass))))

(defn createrepo
  "Runs createrepo on a directory on the remote machine.

   Params:
     host - The hostname of the machine to connect to. String.
     port - Integer for the port to connect to.
     user - The user to connect as. String.
     sudo-pass - Sudo password for the user on the remote machine. String.
     repo-dir - Repo directory. Full-path string.
     user-groups - String containing the user:group to pass to chmod."
  [host port user sudo-pass repo-dir]
  (remote-execute
    host
    port
    user
    (scriptify
        (createrepo "--update" ~repo-dir))))

(defn chown-remote-dir
  [host port user sudo-pass repo-dir user-groups]
  (remote-execute
    host
    port
    user
    (scriptify
        (chown "-R" ~user-groups ~repo-dir))))

(defn latest-rpms-in-repo
  "Lists the latest version of all RPMs that are in the specified repo."
  [host port user repo-name]
  (remote-execute
   host
   port
   user
   (scriptify
    (list_packages_in_repo.py ~repo-name))))

(defn list-latest-rpms-in-repo
  "Runs (latest-rpms-in-repo) and parses the list into a vector of RPM filenames."
  [host port user repo-name]
  (let [cmd-output (first (latest-rpms-in-repo host port user repo-name))
        yum-filter #(and (not (.startsWith % "Loading mirror"))
                         (not (.startsWith % "Loaded plugins")))]
    (if-not (= 0 (:exit cmd-output))
      (throw+ cmd-output))

    (filterv yum-filter (mapv string/trim (string/split (:out cmd-output) #"\n")))))

(defn all-rpms-in-repo
  "Lists all versions of all RPMs that are in the specified repo."
  [host port user repo-name]
  (remote-execute
   host
   port
   user
   (scriptify
    (list_packages_in_repo.py "--all" ~repo-name))))

(defn list-all-rpms-in-repo
  "Runs (all-rpms-in-repo) and parses the list into a vector of RPM filenames."
  [host port user repo-name]
  (let [cmd-output (first (all-rpms-in-repo host port user repo-name))
        yum-filter #(and (not (.startsWith % "Loading mirror"))
                         (not (.startsWith % "Loaded plugins")))]
    (if-not (= 0 (:exit cmd-output))
      (throw+ cmd-output))

    (filterv yum-filter (mapv string/trim (string/split (:out cmd-output) #"\n")))))
