(ns metadactyl.service.apps.de.jobs.params
  (:use [kameleon.uuids :only [uuidify]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [me.raynes.fs :as fs]
            [metadactyl.service.apps.de.jobs.util :as util]
            [metadactyl.util.config :as config]))

(def ^:private irods-home-pattern
  (memoize #(re-pattern (str "\\A\\Q" (string/replace (config/irods-home) #"/+\z" "")))))

(defn remove-irods-home
  [path]
  (when-not (string/blank? path)
    (string/replace path (irods-home-pattern) "")))

(defn log-output
  [retain?]
  {:multiplicity "collection"
   :name         "logs"
   :property     "logs"
   :type         "File"
   :retain       retain?})

(defn- build-input
  "Builds a single input for a step in an app. The current implementation performs the analysis
  configuration lookup twice (once in build-input and once in build-inputs), but the code seems
  clearest that way."
  [retain? param path]
  (let [filename (when-not (nil? path) (fs/base-name path))]
    {:id           (:id param)
     :multiplicity (util/input-multiplicities (:type param))
     :name         filename
     :property     filename
     :retain       (or retain? (:retain param))
     :type         (:type param)
     :value        path}))

(defn- build-inputs-for-param
  [config retain? param]
  (let [param-value (config (util/param->qual-key param))
        paths       (if (sequential? param-value) param-value [param-value])]
    (map (partial build-input retain? param) paths)))

(defn build-inputs
  "Builds the list of inputs for a step in an app. The current implementation performs the
  analysis configuration lookup twice, but the code seems clearest that way."
  [{config :config retain? :debug} params]
  (->> (filter util/input? params)
       (filter (comp config util/param->qual-key))
       (mapcat (partial build-inputs-for-param config retain?))))

(defn- missing-output-filename
  [{step-id :step_id id :id}]
  (throw+ {:type  :clojure-commons.exception/bad-request-field
           :error (str "no filename found for output " id " in step " step-id)}))

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
     :data_source  (:data_source param)
     :type         (:info_type param)}))

(defn build-outputs
  [config default-values params]
  (mapv (partial build-output config default-values)
        (filter util/output? params)))

(defn find-redirect-output-filename
  "Finds the filename for the given data-source in the given list of outputs."
  [outputs data-source]
  ((comp :name first)
   (filter #(= data-source (:data_source %)) outputs)))

(defn value-for-param
  ([config io-maps output-value-map default-values param]
     (let [qual-id  (util/param->qual-id param)
           qual-key (keyword qual-id)]
       (cond (contains? io-maps qual-id)        (output-value-map (io-maps qual-id))
             (contains? config qual-key)        (config qual-key)
             (contains? default-values qual-id) (default-values qual-id)
             :else                              nil)))
  ([config default-values param]
     (value-for-param config {} {} default-values param)))

(defn- determine-opt-arg
  [param-name param-value]
  (let [param-value (if (nil? param-value) "" param-value)]
    (if (re-find #"=\z" param-name)
      [""         (str param-name param-value)]
      [param-name param-value])))

(defn- build-arg
  ([param param-name param-value]
     (let [param-name        (string/trim param-name)
           param-value       (string/trim (str param-value))
           [opt-arg opt-val] (determine-opt-arg param-name param-value)]
       {:id    (:id param)
        :name  opt-arg
        :order (:order param 0)
        :value opt-val}))
  ([param param-value]
     (build-arg param (or (:name param) "") param-value)))

(defn generic-args
  [param param-value]
  (if (or (util/not-blank? param-value) (not (:omit_if_blank param)))
    [(build-arg param param-value)]
    []))

(defn- is-selection-arg?
  [param-value]
  (and (map? param-value)
       (or (util/not-blank? (:name param-value))
           (util/not-blank? (:value param-value)))))

(defn selection-args
  [param param-value]
  (if (is-selection-arg? param-value)
    [(build-arg param (or (:name param-value) "") (or (:value param-value) ""))]
    []))

(defn tree-selection-args
  [param param-value]
  (let [selections (if (seq param-value) param-value [])]
    (mapcat (partial selection-args param) selections)))

(defn- parse-boolean
  [param-value]
  (if-not (instance? Boolean param-value)
    (Boolean/parseBoolean (string/trim param-value))
    param-value))

(defn- build-flag-arg
  [param selected-value]
  (let [[param-name param-value] (string/split selected-value #" " 2)]
    (build-arg param param-name param-value)))

(defn flag-args
  [param param-value]
  (let [selected?      (parse-boolean param-value)
        values         (string/split (:name param) #"\s*,\s*" 2)
        selected-value (if selected? (first values) (second values))]
    (if (util/not-blank? selected-value)
      [(build-flag-arg param (string/trim selected-value))]
      [])))

(defn- build-input-arg
  [{:keys [repeat_option_flag name] :as param} preprocessor index value]
  (let [name (if (and (pos? index) (not repeat_option_flag)) "" name)]
    (build-arg
      (assoc param :name name)
      ((fnil preprocessor "") value))))

(defn input-args
  [{:keys [is_implicit omit_if_blank] :as param} param-value preprocessor]
  (if-not is_implicit
    (let [values (if (sequential? param-value) param-value [param-value])]
      (vec (map-indexed (partial build-input-arg param preprocessor)
                        (if omit_if_blank (remove string/blank? values) values))))
    []))

(defn output-args
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

(def reference-genome-args
  ;; FIXME: this is functionally a reimplementation of the code that resolves reference
  ;; genomes in the old metadactyl code, which is probably broken. When time permits,
  ;; look for uses of the 'ReferenceGenome' property type to see if and how it's being
  ;; used and whether or not this implementation of the command-line formatting code will
  ;; work.
  (partial build-reference-genome-args ["annotation.gtf" "genome.fas"]))

(def reference-sequence-args
  (partial build-reference-genome-args ["genome.fas"]))

(def reference-annotation-args
  (partial build-reference-genome-args ["annotation.gtf"]))

(defn- args-for-param
  [formatter config io-maps output-value-map default-values param]
  (let [param-value (value-for-param config io-maps output-value-map default-values param)
        param-type  (:type param)]
    (cond
     (= "TreeSelection" param-type)
     (.buildTreeSelectionArgs formatter param param-value)

     (re-find #"Selection\z" param-type)
     (.buildSelectionArgs formatter param param-value)

     (= "Flag" param-type)
     (.buildFlagArgs formatter param param-value)

     (util/input-types param-type)
     (.buildInputArgs formatter param param-value)

     (util/output-types param-type)
     (.buildOutputArgs formatter param param-value)

     (= "ReferenceGenome" param-type)
     (.buildReferenceGenomeArgs formatter param param-value)

     (= "ReferenceSequence" param-type)
     (.buildReferenceSequenceArgs formatter param param-value)

     (= "ReferenceAnnotation" param-type)
     (.buildReferenceAnnotationArgs formatter param param-value)

     :else
     (.buildGenericArgs formatter param param-value))))

(defn build-params
  [formatter config io-maps outputs default-values params]
  (let [output-value-map (into {} (map (juxt :qual-id :name) outputs))]
    (mapcat (partial args-for-param formatter config io-maps output-value-map default-values)
            (remove util/ignored-param? params))))

(def ^:private generated-param-ids
  {:archive      (uuidify "06F127BB-0599-4343-89CB-DD15BD0163DE")
   :archive-path (uuidify "21039F17-3D4B-4E33-BD10-2904408826F8")
   :command      (uuidify "3046E5B2-F617-49BE-9C26-57D257F2331B")
   :job-name     (uuidify "0D1DB50C-C930-4CDA-8FEC-2E7326B26849")
   :proxy-user   (uuidify "616D8815-C42A-4E53-885B-E7A594D4BDBD")})

(defn- generate-extra-arg
  [order param-name param-value id-key]
  (let [[opt-arg opt-val] (determine-opt-arg param-name param-value)]
    {:id    (generated-param-ids id-key)
     :name  opt-arg
     :order order
     :value opt-val}))

(defn build-extra-fapi-args
  [user job-name output-dir]
  [(generate-extra-arg 0 "run" "" :command)
   (generate-extra-arg 1 "--proxy_user=" user :proxy-user)
   (generate-extra-arg 1 "--jobName=" job-name :job-name)
   (generate-extra-arg 1 "--archive" "" :archive)
   (generate-extra-arg 1 "--archivePath=" (remove-irods-home output-dir) :archive-path)])
