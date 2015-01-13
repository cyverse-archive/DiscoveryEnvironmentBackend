(ns metadactyl.translations.app-metadata.external-to-preview-test
  (:use [clojure.test]
        [metadactyl.translations.app-metadata.external-to-preview]))

(deftest no-parameter-groups
  (is (= {:params []}
         (translate-template {:groups []}))))

(deftest no-parameters
  (is (= {:params []}
         (translate-template
          {:groups
           {:groups
            [{:parameters []}
             {:parameters []}]}}))))

(deftest one-parameter
  (is (= {:params
          [{:name  "-f"
            :value "foo"
            :order 1}]}
         (translate-template
          {:groups
           [{:parameters
             [{:id    "some-uuid"
               :name  "-f"
               :label "foo"
               :value "foo"
               :order 1}]}]}))))

(deftest parameters-in-one-group
  (is (= {:params
          [{:name  "-f"
            :value "foo"
            :order 2}
           {:name  "-b"
            :value "bar"
            :order 1}]}
         (translate-template
          {:groups
           [{:parameters
             [{:id    "some-uuid"
               :name  "-f"
               :label "foo"
               :value "foo"
               :order 2}
              {:id    "some-other-uuid"
               :name  "-b"
               :label "bar"
               :value "bar"
               :order 1}]}]}))))

(deftest parameters-in-multiple-groups
  (is (= {:params
          [{:name  "-f"
            :value "foo"
            :order 1}
           {:name  "-z"
            :value "baz"
            :order 4}
           {:name  "-r"
            :value "bar"
            :order 3}]}
         (translate-template
          {:groups
           [{:parameters
             [{:id    "some-uuid"
               :name  "-f"
               :label "foo"
               :value "foo"
               :order 1}
              {:id    "some-other-uuid"
               :name  "-z"
               :label "baz"
               :value "baz"
               :order 4}]}
            {:parameters
             [{:id    "yet-another-uuid"
               :name  "-r"
               :label "bar"
               :value "bar"
               :order 3}]}]}))))

(defn- implicit-test-prop
  [[base-name type implicit?] order]
  (let [param-value {:path base-name}]
    {:id              (str base-name "-id")
     :name            (str "--" base-name)
     :label           base-name
     :value           (if (= type "MultiFileSelector") [param-value] param-value)
     :order           order
     :type            type
     :file_parameters {:is_implicit implicit?}}))

(deftest implicit-input-parameters
  (is (= {:params
          [{:name  "--foo"
            :value "foo"
            :order 1}
           {:name  "--bar"
            :value "bar"
            :order 3}
           {:name  "--baz"
            :value "baz"
            :order 5}]}
         (translate-template
          {:groups
           [{:parameters
             (map implicit-test-prop
                  [["foo" "FileInput"         false]
                   ["oof" "FileInput"         true]
                   ["bar" "FolderInput"       false]
                   ["rab" "FolderInput"       true]
                   ["baz" "MultiFileSelector" false]
                   ["zab" "MultiFileSelector" true]]
                  (iterate inc 1))}]}))))

(defn- build-empty-input-parameter
  [type order]
  (let [param-value {:path ""}]
    {:id    (str "empty-" type)
     :name  (str "--" type)
     :label type
     :value (if (= type "MultiFileSelector") [param-value] param-value)
     :order order
     :type  type}))

(deftest empty-input-parameters
  (is (= {:params []}
         (translate-template
          {:groups
           [:parameters
            (map build-empty-input-parameter
                 ["FileInput" "FolderInput" "MultiFileSelector"]
                 (iterate inc 1))]}))))
