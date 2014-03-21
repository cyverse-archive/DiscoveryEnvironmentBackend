(use '[clojure.java.io :only [file]]
     '[clojure.pprint :only [pprint]]
     '[leiningen.exec :only [deps]])

(deps '[[me.raynes/fs "1.4.4"]])

(require '[clojure.string :as string]
         '[clojure.xml :as xml]
         '[me.raynes.fs :as fs])

(def project-prop-tags [:groupId :artifactId :version])
(def project-dep-tags project-prop-tags)
(def project-dirs ["databases" "lein-plugins" "libs" "misc" "services" "tools"])

(defn find-tags
  [parent tag]
  (filter #(= tag (:tag %)) (:content parent)))

(defn get-tag-content
  [parent tag]
  (:content (first (find-tags parent tag))))

(defn build-mvn-project-prop
  [project tag]
  [(keyword (str "project." (name tag)))
   (first (get-tag-content project tag))])

(defn get-mvn-project-properties
  [project]
  (into {} (map (partial build-mvn-project-prop project) project-prop-tags)))

(defn get-mvn-user-properties
  [project]
  (into {} (map (juxt :tag (comp first :content))
                (get-tag-content project :properties))))

(defn get-mvn-properties
  [project]
  (merge (get-mvn-project-properties project)
         (get-mvn-user-properties project)))

(defn substitute-mvn-properties
  [props s]
  (string/replace s #"\$\{([^}]+)\}"
                  (fn [[_ p]] (or ((keyword p) props) (str "${" p "}")))))

(defn build-dependency
  [props raw-dep]
  (let [sub-prop   (partial substitute-mvn-properties props)
        get-val    (comp sub-prop first (partial get-tag-content raw-dep))
        build-dep  (juxt identity get-val)
        dep-props  (into {} (map build-dep project-dep-tags))
        dep-sym    (symbol (string/join "/" ((juxt :groupId :artifactId) dep-props)))
        version    (:version dep-props)]
    [dep-sym version]))

(defn add-parent-pom-dep
  [deps props pom]
  (if-let [parent-pom (first (find-tags pom :parent))]
    (conj deps (build-dependency props parent-pom))
    deps))

(defn get-mvn-deps
  [path]
  (let [pom         (xml/parse path)
        props       (get-mvn-properties pom)
        group-id    (:project.groupId props)
        artifact-id (:project.artifactId props)
        version     (:project.version props)
        artifact    (symbol (str group-id "/" artifact-id))
        dep-tags    (get-tag-content pom :dependencies)
        deps        (mapv (partial build-dependency props) dep-tags)
        deps        (add-parent-pom-dep deps props pom)]
    [artifact (fs/parent path) version deps]))

(defn get-lein-deps
  [path]
  (let [[_ proj ver & rest] (read-string (slurp path))]
    [proj (fs/parent path) ver (:dependencies (apply hash-map rest))]))

(defn get-sh-deps
  [path]
  [(symbol (str "org.iplantc/" (fs/base-name path))) path "" []])

(defn unknown-project-type
  [project-dir]
  (binding [*out* *err*]
    (println "unknown project type:" (str project-dir))
    (System/exit 1)))

(defn get-deps
  [project-dir]
  (let [project-clj  (fs/file project-dir "project.clj")
        pom-xml      (fs/file project-dir "pom.xml")
        build-sh     (fs/file project-dir "build.sh")]
    (cond (fs/exists? project-clj) (get-lein-deps project-clj)
          (fs/exists? pom-xml)     (get-mvn-deps pom-xml)
          (fs/exists? build-sh)    (get-sh-deps project-dir)
          :else                    (unknown-project-type project-dir))))

(defn get-all-deps
  []
  (let [list-subdirs (fn [dir] (map (partial fs/file dir) (fs/list-dir dir)))]
    (mapv get-deps (mapcat list-subdirs project-dirs))))

(defn map-elements
  [key-index value-index s]
  (into {} (map #(vector (nth % key-index) (nth % value-index)) s)))

(defn eligible-for-build?
  [our-projects selected-projects [proj deps]]
  (let [selected? (partial contains? selected-projects)
        not-ours? (complement (partial contains? our-projects))]
    (and (not (selected? proj))
         (empty? (remove (comp (some-fn selected? not-ours?) first) deps)))))

(defn get-build-order
  [all-deps]
  (let [deps-of      (map-elements 0 3 all-deps)
        our-projects (set (map first all-deps))]
    (loop [selected #{} build-order []]
      (let [eligible? (partial eligible-for-build? our-projects selected)]
        (if-not (= (count selected) (count our-projects))
         (let [eligible-projects (mapv first (filter eligible? deps-of))]
           (recur (apply conj selected eligible-projects)
                  (conj build-order eligible-projects)))
         build-order)))))

(pprint (get-build-order (get-all-deps)))
