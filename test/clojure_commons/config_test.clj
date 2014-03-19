(ns clojure-commons.config-test
  (:use [clojure.test]
        [clojure-commons.config])
  (:import [java.util Properties]))

(defn props-from-map
  "Creates an instance of java.util.Properties from a map."
  [m]
  (let [props (Properties.)]
    (dorun (map (fn [[k v]] (.setProperty props k v)) m))
    props))

(def props
  "The example properties to use for testing. Note that the properties aren't actually loaded
   into the reference until after the configuration settings are defined. This simulates the
   most common usage, in which a service loads the configuration properties after the namespace
   that defines the configuration settings has been loaded."
  (ref nil))

(def config-valid
  "A flag indicating that the configuration is valid."
  (ref true))

(def configs
  "A vector of configuration settings."
  (ref []))

(defprop-boolean enabled-flag
  "Description of enabled-flag."
  [props config-valid configs]
  "enabled-flag")

(defprop-boolean disabled-flag
  "Description of disabled-flag."
  [props config-valid configs]
  "disabled-flag")

(defprop-str foo
  "Description of foo."
  [props config-valid configs]
  "foo")

(defprop-str baz
  "Description of baz."
  [props config-valid configs]
  "baz")

(defprop-str enabled-string
  "String property with enabled feature flag."
  [props config-valid configs enabled-flag]
  "enabled-string")

(defprop-str disabled-string
  "String property with disabled feature flag."
  [props config-valid configs disabled-flag]
  "disabled-string")

(defprop-optstr defined-optional-string
  "Defined optional string."
  [props config-valid configs]
  "defined-optional-string")

(defprop-optstr undefined-optional-string
  "Undefined optional string."
  [props config-valid configs]
  "undefined-optional-string")

(defprop-optstr undefined-optional-string-with-default
  "Undefined optional string with default value."
  [props config-valid configs]
  "undefined-optional-string-with-default"
  "The foo is in the bar.")

(defprop-optstr enabled-optional-string
  "Enabled optional string"
  [props config-valid configs enabled-flag]
  "enabled-optional-string")

(defprop-optstr enabled-optional-string-with-default
  "Enabled optional string with default."
  [props config-valid configs enabled-flag]
  "enabled-optional-string-with-default"
  "This is an enabled optional string.")

(defprop-optstr disabled-optional-string
  "Disabled optional string."
  [props config-valid configs disabled-flag]
  "disabled-optional-string")

(defprop-optstr disabled-optional-string-with-default
  "Disabled optional string with default."
  [props config-valid configs disabled-flag]
  "disabled-optional-string-with-default"
  "This is a disabled optional string.")

(defprop-vec required-vector
  "Required vector."
  [props config-valid configs]
  "required-vector")

(defprop-vec enabled-vector
  "Enabled vector."
  [props config-valid configs enabled-flag]
  "enabled-vector")

(defprop-vec disabled-vector
  "Disabled vector."
  [props config-valid configs disabled-flag]
  "disabled-vector")

(defprop-optvec defined-optional-vector
  "Defined optional vector."
  [props config-valid configs]
  "defined-optional-vector")

(defprop-optvec undefined-optional-vector
  "Undefined optional vector."
  [props config-valid configs]
  "undefined-optional-vector")

(defprop-optvec undefined-optional-vector-with-default
  "Undefined optional vector with default value."
  [props config-valid configs]
  "undefined-optional-vector-with-default"
  (mapv str (range 5)))

(defprop-optvec enabled-optional-vector
  "Enabled optional vector."
  [props config-valid configs enabled-flag]
  "enabled-optional-vector")

(defprop-optvec enabled-optional-vector-with-default
  "enabled optional vector with default."
  [props config-valid configs enabled-flag]
  "enabled-optional-vector-with-default"
  (mapv str (range 10)))

(defprop-optvec disabled-optional-vector
  "Disabled optional vector."
  [props config-valid configs disabled-flag]
  "disabled-optional-vector")

(defprop-optvec disabled-optional-vector-with-default
  "Disabled optional vector with default."
  [props config-valid configs disabled-flag]
  "disabled-optional-vector-with-default"
  (mapv str (range 10)))

(defprop-int required-int
  "Required integer."
  [props config-valid configs]
  "required-int")

(defprop-int enabled-int
  "Enabled required integer."
  [props config-valid configs enabled-flag]
  "enabled-int")

(defprop-int disabled-int
  "Disabled required integer."
  [props config-valid configs disabled-flag]
  "disabled-int")

(defprop-optint defined-optional-int
  "Defined optional integer."
  [props config-valid configs]
  "defined-optional-int")

(defprop-optint undefined-optional-int
  "Undefined optional integer."
  [props config-valid configs]
  "undefined-optional-int")

(defprop-optint undefined-optional-int-with-default
  "Undefined optional integer with default value."
  [props config-valid configs]
  "undefined-optional-int-with-default"
  42)

(defprop-optint enabled-optional-int
  "Enabled optional integer."
  [props config-valid configs enabled-flag]
  "enabled-optional-int")

(defprop-optint enabled-optional-int-with-default
  "Enabled optional integer with default."
  [props config-valid configs enabled-flag]
  "enabled-optional-int-with-default"
  55)

(defprop-optint disabled-optional-int
  "Disabled optional integer."
  [props config-valid configs disabled-flag]
  "disabled-optional-int")

(defprop-optint disabled-optional-int-with-default
  "Disabled optional integer with default."
  [props config-valid configs disabled-flag]
  77)

(defprop-long required-long
  "Required long integer."
  [props config-valid configs]
  "required-long")

(defprop-long enabled-long
  "Enabled long integer."
  [props config-valid configs enabled-flag]
  "enabled-long")

(defprop-long disabled-long
  "Disabled long integer."
  [props config-valid configs disabled-flag]
  "disabled-long")

(defprop-optlong defined-optional-long
  "Defined optional long integer."
  [props config-valid configs]
  "defined-optional-long")

(defprop-optlong undefined-optional-long
  "Undefined optional long integer."
  [props config-valid configs]
  "undefined-optional-long")

(defprop-optlong enabled-optional-long
  "Enabled optional long integer."
  [props config-valid configs enabled-flag]
  "enabled-optional-long")

(defprop-optlong enabled-optional-long-with-default
  "Enabled optional long integer with default."
  [props config-valid configs enabled-flag]
  "enabled-optional-long-with-default"
  99)

(defprop-optlong disabled-optional-long
  "Disabled optional long integer."
  [props config-valid configs disabled-flag]
  "disabled-optional-long")

(defprop-optlong disabled-optional-long-with-default
  "Disabled optional long integer with default."
  [props config-valid configs disabled-flag]
  "disabled-optional-long-with-default"
  101)

(defprop-boolean required-boolean
  "Required boolean."
  [props config-valid configs]
  "required-boolean")

(defprop-boolean enabled-boolean
  "Enabled boolean."
  [props config-valid configs enabled-flag]
  "enabled-boolean")

(defprop-boolean disabled-boolean
  "Disabled boolean."
  [props config-valid configs disabled-flag]
  "disabled-boolean")

(defprop-optboolean defined-optional-boolean
  "Defined optional boolean."
  [props config-valid configs]
  "defined-optional-boolean")

(defprop-optboolean undefined-optional-boolean
  "Undefined optional boolean."
  [props config-valid configs]
  "undefined-optional-boolean")

(defprop-optboolean undefined-optional-boolean-with-default
  "Undefined optional boolean with default value."
  [props config-valid configs]
  "undefined-optional-boolean-with-default"
  true)

(defprop-optboolean enabled-optional-boolean
  "Enabled optional boolean."
  [props config-valid configs enabled-flag]
  "enabled-optional-boolean")

(defprop-optboolean enabled-optional-boolean-with-default
  "Enabled optional boolean with default."
  [props config-valid configs enabled-flag]
  "enabled-optional-boolean-with-default"
  true)

(defprop-optboolean disabled-optional-boolean
  "Disabled optional boolean."
  [props config-valid configs disabled-flag]
  "disabled-optional-boolean")

(defprop-optboolean disabled-optional-boolean-with-default
  "Disbled optional boolean with default."
  [props config-valid configs disabled-flag]
  "disabled-optional-boolean-with-default"
  true)

(defprop-optstr multiple-enabled-flags
  "Flagged property with multiple enabled flags."
  [props config-valid configs enabled-flag enabled-flag]
  "multiple-enabled-flags"
  "multiple-enabled-flags")

(defprop-optstr mixed-feature-flags
  "Flagged property with one disabled flag and one enabled flag."
  [props config-valid configs enabled-flag disabled-flag]
  "mixed-feature-flags"
  "mixed-feature-flags")

(defprop-optstr multiple-disabled-flags
  "Flagged property with multiple disabled flags."
  [props config-valid configs disabled-flag disabled-flag]
  "multiple-disabled-flags"
  "multiple-disabled-flags")

;; The properties can be loaded now that the settings are defined.
(dosync
 (ref-set props (props-from-map
                 {"enabled-flag"             "true"
                  "disabled-flag"            "false"
                  "foo"                      "bar"
                  "baz"                      "quux"
                  "enabled-string"           "Ni!"
                  "defined-optional-string"  "blarg"
                  "required-vector"          "foo, bar, baz"
                  "enabled-vector"           "z'bang, zoom, boing"
                  "defined-optional-vector"  "baz, bar, foo"
                  "required-int"             "27"
                  "enabled-int"              "4"
                  "defined-optional-int"     "53"
                  "required-long"            "72"
                  "enabled-long"             "97"
                  "defined-optional-long"    "35"
                  "required-boolean"         "true"
                  "enabled-boolean"          "true"
                  "defined-optional-boolean" "true"})))

(deftest foo-defined
  (is (= "bar" (foo))))

(deftest baz-defined
  (is (= "quux" (baz))))

(deftest enabled-string-defined
  (is (resolve 'clojure-commons.config-test/enabled-string)))

(deftest enabled-string-correct
  (is (= "Ni!" (enabled-string))))

(deftest disabled-string-defined
  (is (resolve 'clojure-commons.config-test/disabled-string)))

(deftest disabled-string-nil
  (is (nil? (disabled-string))))

(deftest defined-optional-string-defined
  (is (= "blarg" (defined-optional-string))))

(deftest undefined-optional-string-empty
  (is (= "" (undefined-optional-string))))

(deftest undefined-optional-string-with-default-correct
  (is (= "The foo is in the bar." (undefined-optional-string-with-default))))

(deftest enabled-optional-string-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-string)))

(deftest enabled-optional-string-correct
  (is (= "" (enabled-optional-string))))

(deftest enabled-optional-string-with-default-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-string-with-default)))

(deftest enabled-optional-string-with-default-correct
  (is (= "This is an enabled optional string." (enabled-optional-string-with-default))))

(deftest disabled-optional-string-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-string)))

(deftest disabled-optional-string-nil
  (is (nil? (disabled-optional-string))))

(deftest disabled-optional-string-with-default-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-string-with-default)))

(deftest disabled-optional-string-with-default-nil
  (is (nil? (disabled-optional-string-with-default))))

(deftest required-vector-defined
  (is (= ["foo" "bar" "baz"] (required-vector))))

(deftest enabled-vector-defined
  (is (resolve 'clojure-commons.config-test/enabled-vector)))

(deftest enabled-vector-correct
  (is (= ["z'bang" "zoom" "boing"] (enabled-vector))))

(deftest disabled-vector-defined
  (is (resolve 'clojure-commons.config-test/disabled-vector)))

(deftest disabled-vector-nil
  (is (nil? (disabled-vector))))

(deftest defined-optional-vector-correct
  (is (= ["baz" "bar" "foo"] (defined-optional-vector))))

(deftest undefined-optional-vector-correct
  (is (= [] (undefined-optional-vector))))

(deftest undefined-optional-vector-with-default-correct
  (is (= (mapv str (range 5)) (undefined-optional-vector-with-default))))

(deftest enabled-optional-vector-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-vector)))

(deftest enabled-optional-vector-correct
  (is (= [] (enabled-optional-vector))))

(deftest enabled-optional-vector-with-default-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-vector-with-default)))

(deftest enabled-optional-vector-with-default-correct
  (is (= (mapv str (range 10)) (enabled-optional-vector-with-default))))

(deftest disabled-optional-vector-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-vector)))

(deftest disabled-optional-vector-nil
  (is (nil? (disabled-optional-vector))))

(deftest disabled-optional-vector-with-default-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-vector-with-default)))

(deftest disabled-optional-vector-with-default-nil
  (is (nil? (disabled-optional-vector-with-default))))

(deftest required-int-defined
  (is (= 27 (required-int))))

(deftest enabled-int-defined
  (is (resolve 'clojure-commons.config-test/enabled-int)))

(deftest enabled-int-correct
  (is (= 4 (enabled-int))))

(deftest disabled-int-defined
  (is (resolve 'clojure-commons.config-test/disabled-int)))

(deftest disabled-int-nil
  (is (nil? (disabled-int))))

(deftest defined-optional-int-correct
  (is (= 53 (defined-optional-int))))

(deftest undefined-optional-int-correct
  (is (zero? (undefined-optional-int))))

(deftest undefined-optional-int-with-default-correct
  (is (= 42 (undefined-optional-int-with-default))))

(deftest enabled-optional-int-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-int)))

(deftest enabled-optional-int-correct
  (is (zero? (enabled-optional-int))))

(deftest enabled-optional-int-with-default-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-int-with-default)))

(deftest enabled-optional-int-with-default-correct
  (is (= 55 (enabled-optional-int-with-default))))

(deftest disabled-optional-int-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-int)))

(deftest disabled-optional-int-nil
  (is (nil? (disabled-optional-int))))

(deftest disabled-optional-int-with-default-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-int-with-default)))

(deftest disabled-optional-int-with-default-nil
  (is (nil? (disabled-optional-int-with-default))))

(deftest required-long-defined
  (is (= 72 (required-long))))

(deftest enabled-long-defined
  (is (resolve 'clojure-commons.config-test/enabled-long)))

(deftest enabled-long-correct
  (is (= 97 (enabled-long))))

(deftest disabled-long-defined
  (is (resolve 'clojure-commons.config-test/disabled-long)))

(deftest disabled-long-nil
  (is (nil? (disabled-long))))

(deftest defined-optional-long-correct
  (is (= 35 (defined-optional-long))))

(deftest undefined-optional-long-correct
  (is (zero? (undefined-optional-long))))

(deftest enabled-optional-long-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-long)))

(deftest enabled-optional-long-correct
  (is (zero? (enabled-optional-long))))

(deftest enabled-optional-long-with-default-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-long-with-default)))

(deftest enabled-optional-long-with-default-correct
  (is (= 99 (enabled-optional-long-with-default))))

(deftest disabled-optional-long-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-long)))

(deftest disabled-optional-long-nil
  (is (nil? (disabled-optional-long))))

(deftest disabled-optional-long-with-default-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-long-with-default)))

(deftest disabled-optional-long-with-default-nil
  (is (nil? (disabled-optional-long-with-default))))

(deftest required-boolean-defined
  (is (true? (required-boolean))))

(deftest enabled-boolean-defined
  (is (resolve 'clojure-commons.config-test/enabled-boolean)))

(deftest enabled-boolean-correct
  (is (true? (enabled-boolean))))

(deftest disabled-boolean-defined
  (is (resolve 'clojure-commons.config-test/disabled-boolean)))

(deftest disabled-boolean-nil
  (is (nil? (disabled-boolean))))

(deftest defined-optional-boolean-defined
  (is (true? (defined-optional-boolean))))

(deftest undefined-optional-boolean-correct
  (is (false? (undefined-optional-boolean))))

(deftest undefined-optional-boolean-with-default-correct
  (is (true? (undefined-optional-boolean-with-default))))

(deftest enabled-optional-boolean-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-boolean)))

(deftest enabled-optional-boolean-correct
  (is (false? (enabled-optional-boolean))))

(deftest enabled-optional-boolean-with-default-defined
  (is (resolve 'clojure-commons.config-test/enabled-optional-boolean-with-default)))

(deftest enabled-optional-boolean-with-default-correct
  (is (true? (enabled-optional-boolean-with-default))))

(deftest disabled-optional-boolean-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-boolean)))

(deftest disabled-optional-boolean-nil
  (is (nil? (disabled-optional-boolean))))

(deftest disabled-optional-boolean-with-default-defined
  (is (resolve 'clojure-commons.config-test/disabled-optional-boolean-with-default)))

(deftest disabled-optional-boolean-with-default-nil
  (is (nil? (disabled-optional-boolean-with-default))))

(deftest multiple-enabled-flags-defined
  (is (resolve 'clojure-commons.config-test/multiple-enabled-flags)))

(deftest multiple-enabled-flags-correct
  (is (= "multiple-enabled-flags" (multiple-enabled-flags))))

(deftest mixed-feature-flags-defined
  (is (resolve 'clojure-commons.config-test/mixed-feature-flags)))

(deftest mixed-feature-flags-correct
  (is (= "mixed-feature-flags" (mixed-feature-flags))))

(deftest multiple-disabled-flags-defined
  (is (resolve 'clojure-commons.config-test/multiple-disabled-flags)))

(deftest multiple-disabled-flags-nil
  (is (nil? (multiple-disabled-flags))))

(deftest configs-defined
  (is (= [#'enabled-flag
          #'disabled-flag
          #'foo
          #'baz
          #'enabled-string
          #'disabled-string
          #'defined-optional-string
          #'undefined-optional-string
          #'undefined-optional-string-with-default
          #'enabled-optional-string
          #'enabled-optional-string-with-default
          #'disabled-optional-string
          #'disabled-optional-string-with-default
          #'required-vector
          #'enabled-vector
          #'disabled-vector
          #'defined-optional-vector
          #'undefined-optional-vector
          #'undefined-optional-vector-with-default
          #'enabled-optional-vector
          #'enabled-optional-vector-with-default
          #'disabled-optional-vector
          #'disabled-optional-vector-with-default
          #'required-int
          #'enabled-int
          #'disabled-int
          #'defined-optional-int
          #'undefined-optional-int
          #'undefined-optional-int-with-default
          #'enabled-optional-int
          #'enabled-optional-int-with-default
          #'disabled-optional-int
          #'disabled-optional-int-with-default
          #'required-long
          #'enabled-long
          #'disabled-long
          #'defined-optional-long
          #'undefined-optional-long
          #'enabled-optional-long
          #'enabled-optional-long-with-default
          #'disabled-optional-long
          #'disabled-optional-long-with-default
          #'required-boolean
          #'enabled-boolean
          #'disabled-boolean
          #'defined-optional-boolean
          #'undefined-optional-boolean
          #'undefined-optional-boolean-with-default
          #'enabled-optional-boolean
          #'enabled-optional-boolean-with-default
          #'disabled-optional-boolean
          #'disabled-optional-boolean-with-default
          #'multiple-enabled-flags
          #'mixed-feature-flags
          #'multiple-disabled-flags]
         @configs)))

(deftest initial-configs-valid
  (is (validate-config configs config-valid)))
