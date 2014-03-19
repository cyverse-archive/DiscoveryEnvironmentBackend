(ns kameleon.test.pgpass
  (:use [clojure.java.io :only [reader]]
        [clojure.test]
        [kameleon.pgpass]))

(defn password-search
  "Performs a password search in exmple .pgpass file text."
  [text host port db user]
  (get-password (reader (char-array text))
                {:host host
                 :port port
                 :db db
                 :user user}))

(deftest single-line
  "Verifies that we can correctly find matches in a single-line file."
  (let [text "foo:1234:bar:baz:by-tor\n"]
    (is (= "by-tor" (password-search text "foo" "1234" "bar" "baz")))
    (is (nil? (password-search text "bar" "1234" "bar" "baz")))))

(deftest multiline
  "Verifies that we can correctly find matches in a multiline file."
  (let [lines ["foo:1234:bar:baz:by-tor\n"
               "foo:5432:bar:bar:snow-dog\n"]
        text (apply str lines)]
    (is (= "by-tor" (password-search text "foo" "1234" "bar" "baz")))
    (is (= "snow-dog" (password-search text "foo" "5432" "bar" "bar")))
    (is (nil? (password-search text "foo" "5432" "bar" "ni!")))))

(deftest different-hostname
  "Verifies that a line won't match if the hostnames are different."
  (let [text "foo:1234:bar:baz:by-tor\n"]
    (is (nil? (password-search text "ni!" "1234" "bar" "baz")))))

(deftest different-port
  "Verifies that a line won't match if the port is different."
  (let [text "foo:1234:bar:baz:by-tor\n"]
    (is (nil? (password-search text "foo" "5432" "bar" "baz")))))

(deftest different-database
  "Verifies that a line won't match if the database is different."
  (let [text "foo:1234:bar:baz:by-tor\n"]
    (is (nil? (password-search text "foo" "1234" "ni!" "baz")))))

(deftest different-user
  "Verifies that a line won't match if the user name is different."
  (let [text "foo:1234:bar:baz:by-tor\n"]
    (is (nil? (password-search text "foo" "1234" "bar" "ni!")))))

(deftest empty-file
  "Verifies that there isn't any match if the file is empty."
  (is (nil? (password-search "" "foo" "1234" "bar" "baz"))))

(deftest partial-line
  "Verifies that partial lines don't cause errors."
  (let [text "foo:1234"]
    (is (nil? (password-search text "foo" "1234" "bar" "baz")))))

(deftest wildcard-hostname
  "Verifies that a wildcard hostname will match any hostname."
  (let [text "*:1234:bar:baz:by-tor"]
    (is (= "by-tor" (password-search text "foo" "1234" "bar" "baz")))))

(deftest wildcard-port
  "Verifies that a wildcard port will match any port."
  (let [text "foo:*:bar:baz:by-tor"]
    (is (= "by-tor" (password-search text "foo" "1234" "bar" "baz")))))

(deftest wildcard-db
  "Verifies that a wildcard database name will match any database name."
  (let [text "foo:1234:*:baz:by-tor"]
    (is (= "by-tor" (password-search text "foo" "1234" "bar" "baz")))))

(deftest wildcard-user
  "Verifies that a wildcard username will match anyt username."
  (let [text "foo:1234:bar:*:by-tor"]
    (is (= "by-tor" (password-search text "foo" "1234" "bar" "baz")))))
