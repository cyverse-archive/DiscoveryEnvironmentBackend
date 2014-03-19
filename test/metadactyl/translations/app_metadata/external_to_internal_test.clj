(ns metadactyl.translations.app-metadata.external-to-internal-test
  (:use [clojure.test]
        [metadactyl.translations.app-metadata.external-to-internal]))

(defn mark-default-arg
  [default n]
  (if (and (not (nil? default)) (= default n))
    {:isDefault true :id n :name n :value n :display n}
    {:id n :name n :value n :display n}))

(defn- external-selection-args
  [& [default]]
  (mapv (partial mark-default-arg default)
        ["foo" "bar" "baz"]))

(defn- internal-selection-args
  ([]
     (internal-selection-args nil))
  ([default]
     (external-selection-args default)))

(deftest build-validator-for-property-test
  (is (= {:required true :rules []}
         (build-validator-for-property {:required true}))))

(deftest build-validator-for-property-with-rules-test
  (is (= {:required false :rules [{:IntAbove [0]}]}
         (build-validator-for-property {:validators [{:type "IntAbove" :params [0]}]}))))

(deftest build-validator-for-property-with-args-test
  (is (= {:required true :rules [{:MustContain (internal-selection-args)}]}
         (build-validator-for-property {:required true :arguments (external-selection-args)}))))

(deftest build-validator-for-property-with-args-and-rules-test
  (is (= {:required false
          :rules    [{:IntAbove    [0]}
                     {:MustContain (internal-selection-args)}]}
         (build-validator-for-property {:validators [{:type "IntAbove" :params [0]}]
                                        :arguments  (external-selection-args)}))))

(deftest build-validator-for-optional-property-with-args-test
  (is (= {:required     false
          :rules        [{:MustContain (internal-selection-args "foo")}]}
         (build-validator-for-property
          {:arguments    (external-selection-args "foo")
           :defaultValue {:id "foo" :name "foo" :value "foo" :display "foo"}}))))

(deftest translate-property-test
  (is (= {:omit_if_blank false :value "foo" :validator nil :data_object nil :type nil}
         (translate-property {:defaultValue "foo"}))))

(deftest translate-empty-property-test
  (is (= {:omit_if_blank false :value nil :validator nil :data_object nil :type nil}
         (translate-property {}))))

(deftest translate-required-property-test
  (is (= {:omit_if_blank false
          :value         nil
          :validator     {:required true
                          :rules    []}
          :data_object   nil
          :type          nil
          :required      true}
         (translate-property
          {:required true}))))

(deftest translate-property-with-rules-test
  (is (= {:omit_if_blank false
          :value         nil
          :validator     {:required false
                          :rules    [{:IntAbove [42]}]}
          :data_object    nil
          :type           nil}
         (translate-property
          {:validators [{:type   "IntAbove"
                         :params [42]}]}))))

(deftest translate-property-with-args-test
  (is (= {:omit_if_blank false
          :value         nil
          :validator     {:required false
                          :rules    [{:MustContain (internal-selection-args)}]}
          :data_object   nil
          :type          nil}
         (translate-property
          {:arguments (external-selection-args)}))))

(defn- internal-prop
  [multiplicity]
  {:data_object   {:cmdSwitch      "da-name"
                   :data_source    "da-data-source"
                   :description    "da-description"
                   :file_info_type "da-info-type"
                   :format         "da-format"
                   :id             "da-id"
                   :is_implicit    false
                   :multiplicity   multiplicity
                   :name           "da-label"
                   :order          42
                   :required       false
                   :retain         true}
   :description   "da-description"
   :id            "da-id"
   :isVisible     true
   :label         "da-label"
   :name          "da-name"
   :omit_if_blank false
   :order         42
   :required      false
   :type          "Input"
   :value         nil
   :validator     nil})

(defn- external-prop
  [type]
  {:data_object   {:data_source    "da-data-source"
                   :file_info_type "da-info-type"
                   :format         "da-format"
                   :is_implicit    false
                   :retain         true}
   :description   "da-description"
   :id            "da-id"
   :isVisible     true
   :label         "da-label"
   :name          "da-name"
   :omit_if_blank false
   :order         42
   :required      false
   :type          type})

(deftest translate-property-with-data-object-test
  (is (= (internal-prop "One")
         (translate-property (external-prop "FileInput")))))

(deftest folder-input-translation-test
  (is (= (internal-prop "Folder")
         (translate-property (external-prop "FolderInput")))))

(deftest multi-file-selector-translation-test
  (is (= (internal-prop "Many")
         (translate-property (external-prop "MultiFileSelector")))))
