(ns metadactyl.translations.app-metadata.util-test
  (:use [clojure.test]
        [metadactyl.translations.app-metadata.util]
        [slingshot.test]))

(deftest get-property-groups-test
  (is (= ["foo" "bar" "baz"]
         (get-property-groups {:groups ["foo" "bar" "baz"]})))
  (is (= ["foo" "bar" "baz"]
         (get-property-groups {:groups {:groups ["foo" "bar" "baz"]}})))
  (is (thrown+? [:detail :MISSING_PROPERTY_GROUP_LIST]
                (get-property-groups {})))
  (is (thrown+? [:detail :INVALID_PROPERTY_GROUP_LIST]
                (get-property-groups {:groups "Ni!"}))))
