(ns metadactyl.analyses.params
  (:use [kameleon.uuids :only [uuidify]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [me.raynes.fs :as fs]
            [metadactyl.analyses.util :as util]
            [metadactyl.util.config :as config]))

(def ^:private irods-home-pattern
  (memoize #(re-pattern (str "\\A\\Q" (string/replace (config/irods-home) #"/+\z") ""))))

(defn- remove-irods-home
  [path]
  (string/replace path (irods-home-pattern) ""))

(def ^:private log-output
  {:multiplicity "collection"
   :name         "logs"
   :property     "logs"
   :type         "File"
   :retain       true})

(defn- build-input
  "Builds a single input for a step in an app. The current implementation performs the analysis
  configuration lookup twice (once in build-input and once in build-inputs), but the code seems
  clearest that way."
  [config param]
  (let [path     (config (util/param->qual-key param))
        filename (when-not (nil? path) (fs/base-name path))]
    {:id           (:id param)
     :multiplicity (util/input-multiplicities (:type param))
     :name         filename
     :property     filename
     :retain       (:retain param)
     :type         (:type param)
     :value        path}))

(defn build-inputs
  "Builds the list of inputs for a step in an app. The current implementation performs the
  analysis configuration lookup twice, but the code seems clearest that way."
  [config params]
  (->> (filter util/input? params)
       (filter (comp config util/param->qual-key))
       (map (partial build-input config))))

(defn- missing-output-filename
  [{step-id :step_id id :id}]
  (throw+ {:error_code ce/ERR_BAD_REQUEST
           :reason     ("no filename found for output " id " in step " step-id)}))

(defn- get-output-filename
  "Obtains the name of an output filename from either the app config or the default values
  for the job submission."
  [config default-values param]
  (let [qual-id  (util/param->qual-id param)
        qual-key (keyword qual-id)]
    (cond (contains? config qual-key)        (config qual-key)
          (contains? default-values qual-id) (default-values qual-id)
          :else                              (missing-output-filename param))))

(defn- build-output
  [config default-values param]
  (let [filename (get-output-filename config default-values param)]
    {:multiplicity (util/output-multiplicities (:type param))
     :name         filename
     :property     filename
     :qual-id      (util/param->qual-id param)
     :retain       (:retain param)
     :type         (:info_type param)}))

(defn build-outputs
  [config default-values params]
  (conj (mapv (partial build-output config default-values)
              (filter util/output? params))
        log-output))

(defn value-for-param
  ([config io-maps output-value-map default-values param]
     (let [qual-id  (util/param->qual-id param)
           qual-key (keyword qual-id)]
       (cond (contains? io-maps qual-id)        (output-value-map qual-id)
             (contains? config qual-key)        (config qual-key)
             (contains? default-values qual-id) (default-values qual-id)
             :else                              nil)))
  ([config default-values param]
     (value-for-param config {} {} default-values param)))

(defn- build-arg
  ([param param-name param-value]
     {:id    (:id param)
      :name  param-name
      :order (:order param 0)
      :value (if (nil? param-value) "" param-value)})
  ([param param-value]
     (build-arg param (or (:name param) "") param-value)))

(defn- generic-args
  [param param-value]
  (if (or (util/not-blank? param-value) (not (:omit_if_blank param)))
    [(build-arg param param-value)]
    []))

(defn- is-selection-arg?
  [param-value]
  (and (map? param-value)
       (or (seq (:name param-value))
           (seq (:value param-value)))))

(defn- selection-args
  [param param-value]
  (if (is-selection-arg? param-value)
    [(build-arg param (or (:name param-value) "") (or (:value param-value) ""))]
    []))

(defn- tree-selection-args
  [param param-value]
  (let [selections (if (seq? param-value) param-value [])]
    (mapcat (partial selection-args param) selections)))

(defn- flag-args
  [param param-value]
  (let [selected?      (Boolean/parseBoolean (string/trim param-value))
        values         (string/split (:name param) #"\s*,\s*" 2)
        selected-value (if selected? (first values) (second values))]
    (if (util/not-blank? selected-value)
      [(build-arg param selected-value)]
      [])))

(defn- input-args
  [param param-value]
  (let [values (if (seq? param-value) param-value [param-value])]
    (mapv (comp (partial build-arg param) (fnil fs/base-name ""))
          (if (:omit_if_blank param) (remove string/blank? values) values))))

(defn- output-args
  [param param-value]
  (if (and (not (:is_implicit param)) (= (:data_source param) "file"))
    (generic-args param param-value)
    []))

(defn- build-reference-genome-args
  [filenames param param-value]
  (if-let [path (:path param-value)]
    [(->> (map #(str (fs/file path %)) filenames)
          (string/join " ")
          (build-arg param))]
    []))

(def ^:private reference-genome-args
  ;; FIXME: this is functionally a reimplementation of the code that resolves reference
  ;; genomes in the old metadactyl code, which is probably broken. When time permits,
  ;; look for uses of the 'ReferenceGenome' property type to see if and how it's being
  ;; used and whether or not this implementation of the command-line formatting code will
  ;; work.
  (partial build-reference-genome-args ["annotation.gtf" "genome.fas"]))

(def ^:private reference-sequence-args
  (partial build-reference-genome-args ["genome.fas"]))

(def ^:private reference-annotation-args
  (partial build-reference-genome-args ["annotation.gtf"]))

(defn- args-for-param
  [config io-maps output-value-map default-values param]
  (let [param-value (value-for-param config io-maps output-value-map default-values param)
        param-type  (:type param)]
    (cond
     (= "TreeSelection" param-type)
     (tree-selection-args param param-value)

     (re-find #"Selection\z" param-type)
     (selection-args param param-value)

     (= "Flag" param-type)
     (flag-args param param-value)

     (util/input-types param-type)
     (input-args param param-value)

     (util/output-types param-type)
     (output-args param param-value)

     (= "ReferenceGenome" param-type)
     (reference-genome-args param param-value)

     (= "ReferenceSequence" param-type)
     (reference-sequence-args param param-value)

     (= "ReferenceAnnotation" param-type)
     (reference-annotation-args param param-value)

     :else
     (generic-args param param-value))))

(defn build-params
  [config io-maps outputs default-values params]
  (let [output-value-map (into {} (map (juxt :qual-id :name) outputs))]
    (mapcat (partial args-for-param config io-maps output-value-map default-values)
            (remove (comp util/ignored-param-types :type) params))))

(def ^:private generated-param-ids
  {:archive      (uuidify "06F127BB-0599-4343-89CB-DD15BD0163DE")
   :archive-path (uuidify "21039F17-3D4B-4E33-BD10-2904408826F8")
   :command      (uuidify "3046E5B2-F617-49BE-9C26-57D257F2331B")
   :job-name     (uuidify "0D1DB50C-C930-4CDA-8FEC-2E7326B26849")
   :proxy-user   (uuidify "616D8815-C42A-4E53-885B-E7A594D4BDBD")})

(defn- generate-extra-arg
  [order name value id-key]
  {:id    (generated-param-ids id-key)
   :name  name
   :order order
   :value value})

(defn build-extra-fapi-args
  [user job-name output-dir]
  [(generate-extra-arg 0 "run" "" :command)
   (generate-extra-arg 1 "--proxy_user=" user :proxy-user)
   (generate-extra-arg 1 "--jobName=" job-name :job-name)
   (generate-extra-arg 1 "--archive" "" :archive)
   (generate-extra-arg 1 "--archivePath=" (remove-irods-home output-dir) :archive-path)])

(defprotocol ParamFormatter
  "A protocol for formatting parameters in JEX job requests."
  (buildParams [_ config io-maps outputs defaults params]))

(deftype DeParamFormatter []
  ParamFormatter

  (buildParams [_ submission io-maps outputs defaults params]
    (build-params (:config submission) io-maps outputs defaults params)))

(deftype FapiParamFormatter [user]
  ParamFormatter

  (buildParams [_ submission io-maps outputs defaults params]
    (concat (build-extra-fapi-args user (:name submission) (:output_dir submission))
            (build-params (:config submission) io-maps outputs defaults params))))
