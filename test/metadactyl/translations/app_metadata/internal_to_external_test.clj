(ns metadactyl.translations.app-metadata.internal-to-external-test
  (:use [clojure.test]
        [metadactyl.translations.app-metadata.internal-to-external]))

(defn- remove-ids
  [v]
  (cond
   (sequential? v) (map #(dissoc % :id) v)
   (map? v)        (dissoc v :id)
   :else           v))

(defn- remove-ids-from-prop
  [m]
  (-> m
      (update-in [:arguments] remove-ids)
      (update-in [:defaultValue] remove-ids)))

(deftest validators-from-rules-test
  (is (= [{:type "foo" :params ["bar" "baz"]}
          {:type "bar" :params ["baz" "quux"]}]
         (validators-from-rules
          [{:foo ["bar" "baz"]}
           {:bar ["baz" "quux"]}]))))

(deftest validators-from-nil-rules-test
  (is (= [] (validators-from-rules nil))))

(deftest validators-from-empty-rules-test
  (is (= [] (validators-from-rules []))))

(deftest get-property-arguments-from-nil-rules-test
  (is (= [] (get-property-arguments nil))))

(deftest get-property-arguments-from-empty-rules-test
  (is (= [] (get-property-arguments []))))

(deftest get-property-arguments-no-must-contain-test
  (is (= [] (get-property-arguments [{:IntAbove [0]}]))))

(deftest get-property-arguments-test
  (is (= [{:isDefault "false"
           :name      "foo"
           :value     "foo"
           :display   "foo"}]
         (->> (get-property-arguments
               [{:MustContain
                 [{:isDefault "false"
                   :name      "foo"
                   :value     "foo"
                   :display   "foo"}]}])
              (remove-ids)))))

(deftest get-empty-default-value-test
  (is (= nil (get-default-value {} []))))

(deftest get-default-value-from-prop-test
  (is (= "testing" (get-default-value {:value "testing"} []))))

(deftest get-default-value-test
  (is (= {:isDefault "true"
          :name      "foo"
          :value     "foo"
          :display   "foo"}
         (get-default-value
          {}
          [{:isDefault "true"
            :name      "foo"
            :value     "foo"
            :display   "foo"}
           {:isDefault "false"
            :name      "bar"
            :value     "bar"
            :display   "bar"}]))))

(deftest translate-property-test
  (is (= {:name         "prop-name"
          :arguments    []
          :required     false
          :validators   []
          :defaultValue nil}
         (translate-property {:name "prop-name"}))))

(deftest translate-required-property-test
  (is (= {:name         "prop-name"
          :arguments    []
          :required     true
          :validators   []
          :defaultValue nil}
         (translate-property
          {:name      "prop-name"
           :validator {:required true}}))))

(deftest translate-property-with-default-value-test
  (is (= {:name         "prop-name"
          :arguments    []
          :required     false
          :validators   []
          :defaultValue "default-value"}
         (translate-property
          {:name  "prop-name"
           :value "default-value"}))))

(deftest translate-property-with-rules-test
  (is (= {:name         "prop-name"
          :arguments    []
          :required     false
          :validators   [{:type   "IntAbove"
                          :params [42]}]
          :defaultValue nil}
         (translate-property
          {:name      "prop-name"
           :validator {:rules [{:IntAbove [42]}]}}))))

(deftest translate-property-with-args-test
  (is (= {:name         "prop-name"
          :arguments    [{:isDefault "false"
                          :name      "foo"
                          :value     "foo"
                          :display   "foo"}]
          :required     false
          :validators   []
          :defaultValue nil}
         ((comp remove-ids-from-prop translate-property)
          {:name      "prop-name"
           :validator {:rules [{:MustContain [{:isDefault "false"
                                               :name      "foo"
                                               :value     "foo"
                                               :display   "foo"}]}]}}))))

(deftest translate-property-with-default-arg-test
  (is (= {:name         "prop-name"
          :arguments    [{:isDefault "true"
                          :name      "foo"
                          :value     "foo"
                          :display   "foo"}
                         {:isDefault "false"
                          :name      "bar"
                          :value     "bar"
                          :display   "bar"}]
          :required     true
          :validators   []
          :defaultValue {:isDefault "true"
                         :name      "foo"
                         :value     "foo"
                         :display   "foo"}}
         ((comp remove-ids-from-prop translate-property)
          {:name      "prop-name"
           :validator {:required true
                       :rules    [{:MustContain [{:isDefault "true"
                                                  :name      "foo"
                                                  :value     "foo"
                                                  :display   "foo"}
                                                 {:isDefault "false"
                                                  :name      "bar"
                                                  :value     "bar"
                                                  :display   "bar"}]}]}}))))

(deftest translate-property-with-args-and-rules-test
  (is (= {:name         "prop-name"
          :arguments    [{:isDefault "true"
                          :name      "foo"
                          :value     "foo"
                          :display   "foo"}
                         {:isDefault "false"
                          :name      "bar"
                          :value     "bar"
                          :display   "bar"}]
          :required     true
          :validators   [{:type   "IntAbove"
                          :params [42]}]
          :defaultValue {:isDefault "true"
                         :name      "foo"
                         :value     "foo"
                         :display   "foo"}}
         ((comp remove-ids-from-prop translate-property)
          {:name      "prop-name"
           :validator {:required true
                       :rules    [{:MustContain [{:isDefault "true"
                                                  :name      "foo"
                                                  :value     "foo"
                                                  :display   "foo"}
                                                 {:isDefault "false"
                                                  :name      "bar"
                                                  :value     "bar"
                                                  :display   "bar"}]}
                                  {:IntAbove [42]}]}}))))

(defn- external-prop
  [type]
  {:arguments    []
   :data_object  {:data_source    "data-obj-source"
                  :file_info_type "data-obj-info-type"
                  :format         "data-obj-format"
                  :is_implicit    false
                  :retain         true}
   :defaultValue nil
   :description  "data-obj-description"
   :id           "data-obj-id"
   :label        "data-obj-name"
   :name         "data-obj-cmd-switch"
   :order        42
   :required     true
   :validators   []
   :type         type})

(defn- internal-prop
  [multiplicity]
  {:data_object {:cmdSwitch      "data-obj-cmd-switch"
                 :data_source    "data-obj-source"
                 :description    "data-obj-description"
                 :file_info_type "data-obj-info-type"
                 :format         "data-obj-format"
                 :id             "data-obj-id"
                 :is_implicit    false
                 :multiplicity   multiplicity
                 :label          "data-obj-label"
                 :name           "data-obj-name"
                 :order          42
                 :required       true
                 :retain         true}
   :description "prop-description"
   :id          "prop-id"
   :label       "prop-label"
   :name        "prop-name"
   :order       27
   :required    false
   :type        "Input"})

(deftest translate-property-with-data-object-test
  (is (= (external-prop "FileInput")
         (translate-property (internal-prop "One")))))

(deftest folder-property-type-translation
  (is (= (external-prop "FolderInput")
         (translate-property (internal-prop "Folder")))))

(deftest multi-file-selector-property-type-translation
  (is (= (external-prop "MultiFileSelector")
         (translate-property (internal-prop "Many")))))

(deftest translate-property-group-test
  (is (= {:name       "group-name"
          :properties [{:name         "prop-name"
                        :arguments    []
                        :required     false
                        :validators   []
                        :defaultValue nil}]}
         (translate-property-group
          {:name       "group-name"
           :properties [{:name "prop-name"}]}))))

(deftest translate-template-test
  (is (= {:name   "template-name"
          :groups [{:name       "group-name"
                    :properties [{:name         "prop-name"
                                  :arguments    []
                                  :required     false
                                  :validators   []
                                  :defaultValue nil}]}]}
         (translate-template
          {:name   "template-name"
           :groups [{:name       "group-name"
                     :properties [{:name "prop-name"}]}]}))))
