(ns metadactyl.translations.app-metadata.external-to-preview-test
  (:use [clojure.test]
        [metadactyl.translations.app-metadata.external-to-preview]))

(deftest no-property-groups
  (is (= {:params []}
         (translate-template {:groups []}))))

(deftest no-properties
  (is (= {:params []}
         (translate-template
          {:groups
           {:groups
            [{:properties []}
             {:properties []}]}}))))

(deftest one-property
  (is (= {:params
          [{:name  "-f"
            :value "foo"
            :order 1}]}
         (translate-template
          {:groups
           [{:properties
             [{:id    "some-uuid"
               :name  "-f"
               :label "foo"
               :value "foo"
               :order 1}]}]}))))

(deftest properties-in-one-group
  (is (= {:params
          [{:name  "-f"
            :value "foo"
            :order 2}
           {:name  "-b"
            :value "bar"
            :order 1}]}
         (translate-template
          {:groups
           {:groups
            [{:properties
              [{:id    "some-uuid"
                :name  "-f"
                :label "foo"
                :value "foo"
                :order 2}
               {:id    "some-other-uuid"
                :name  "-b"
                :label "bar"
                :value "bar"
                :order 1}]}]}}))))

(deftest properties-in-multiple-groups
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
           [{:properties
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
            {:properties
             [{:id    "yet-another-uuid"
               :name  "-r"
               :label "bar"
               :value "bar"
               :order 3}]}]}))))
