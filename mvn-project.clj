(use '[clojure.pprint :only [pprint]]
     '[leiningen.exec :only [deps]])

(require '[clojure.string :as string]
         '[clojure.xml :as xml])

(def project-prop-tags [:groupId :artifactId :version])
(def project-dep-tags project-prop-tags)

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

(defn get-mvn-deps
  [path]
  (let [pom   (xml/parse path)
        props (get-mvn-properties pom)]
    (mapv (partial build-dependency props)
          (get-tag-content pom :dependencies))))
