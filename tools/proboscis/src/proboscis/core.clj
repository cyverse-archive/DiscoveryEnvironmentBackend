(ns proboscis.core
  (:gen-class)
  (:use [clojure.pprint :only [pprint]]
        [clojure.tools.cli :only [cli]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]))

(def ^:private mapping-files ["mappings/data/file.json" "mappings/data/folder.json"])
(def ^:private settings-file "settings.json")

(def ^:private index "data")

(defmacro ^:private trap
  [[banner] & body]
  `(try+
     (do ~@body)
     (catch (every-pred map? :message) {msg# :message}
       (binding [*out* *err*] (println msg#))
       (System/exit 1))
     (catch (every-pred map? :body) {body# :body}
       (binding [*out* *err*] (println body#))
       (System/exit 1))
     (catch map? m#
       (binding [*out* *err*] (pprint m#))
       (System/exit 1))
     (catch Throwable e#
       (binding [*out* *err*]
         (println "Unexpected Exception: " (str e#))
         (.printStackTrace e#))
       (System/exit 1))))

(defn- to-int
  [s]
  (Integer. s))

(defn- parse-args
  [args]
  (cli args
       ["-h" "--host" "The host where ElasticSearch is running." :default "localhost"]
       ["-p" "--port" "The port that ElasticSearch is listening to." :parse-fn to-int :default 9200]
       ["-?" "--[no-]help" "Display this help text."]))

(defn- load-json
  [file]
  (json/parse-string (slurp file)))

(defn- load-mappings
  []
  (map (comp load-json io/resource) mapping-files))

(defn- fmt-mappings
  [mappings]
  (letfn [(mk-entry [mapping] [(key (first mapping)) mapping])]
    (into {} (map mk-entry mappings))))

(defn- init-es
  [{:keys [host port]}]
  (esr/connect! (str "http://" host ":" port))
  (when (esi/exists? index) (esi/delete index))
  (esi/create index
              :settings (load-json (io/resource settings-file))
              :mappings (fmt-mappings (load-mappings))))

(defn -main
  [& args]
  (let [[opts args banner] (parse-args args)]
    (when (:help opts)
      (println banner)
      (System/exit 0))
    (trap [banner]
      (init-es opts))))

;; Local Variables:
;; mode: clojure
;; eval: (define-clojure-indent (trap (quote defun)))
;; End:
