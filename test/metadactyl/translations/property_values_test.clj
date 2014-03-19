(ns metadactyl.translations.property-values-test
  (:use [metadactyl.translations.property-values]
        [clojure.test]))

(deftest string-value
  (is (= {:parameters [{:param_value {:value "foo"}}]})
      (format-property-values-response
       {:parameters [{:param_value "foo"}]})))

(deftest int-value
  (is (= {:parameters [{:param_value {:value 1}}]}
         (format-property-values-response
          {:parameters [{:param_value 1}]}))))

(deftest array-value
  (is (= {:parameters [{:param_value {:value "foo"}}
                       {:param_value {:value "bar"}}
                       {:param_value {:value "baz"}}]}
         (format-property-values-response
          {:parameters [{:param_value ["foo" "bar" "baz"]}]}))))

(deftest object-value
  (is (= {:parameters [{:param_value {:value {:foo "bar" :baz "quux"}}}]}
         (format-property-values-response
          {:parameters [{:param_value {:foo "bar" :baz "quux"}}]}))))
