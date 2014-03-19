(ns kameleon.test.sql-reader
  (:use [clojure.java.io :only [reader]]
        [clojure.pprint :only [pprint]]
        [clojure.test]
        [kameleon.sql-reader]
        [slingshot.slingshot :only [try+]]))

(defn fail
  "Called any time we want a test to fail immediately."
  [msg]
  (is #(identity false) msg))

(defn pass
  "Called any time we want a test to pass immediately."
  [msg]
  (is #(identity true) msg))

(defn parse
  "Parses a string into a sequence of SQL statements."
  [string]
  (sql-statements (reader (char-array string))))

(deftest simple-statement
  "Verifies that we can extract a single simple statement."
  (let [stmts (parse "SELECT * FROM foo;")]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))))

(deftest multiple-statements
  "Verifies that we can extract multiple simple statements."
  (let [lines ["SELECT * FROM foo;\n"
               "SELECT * FROM bar;\n"]
        stmts (parse (apply str lines))]
    (is (= 2 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))
    (is (= "SELECT * FROM bar" (last stmts)))))

(deftest leading-line-comment
  "Verifies that we can extract a statement if there's a leading line comment."
  (let [lines ["-- This is a leading comment.\n"
               "SELECT * FROM foo;\n"]
        stmts (parse (apply str lines))]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))))

(deftest leading-c-comment
  "Verifies that we can extract a statement if there's a leading C comment."
  (let [lines ["/* This is a leading comment. */\n"
               "SELECT * FROM foo;\n"]
        stmts (parse (apply str lines))]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))))

(deftest single-quoted-value
  "Verifies that we can handle single-quoted strings."
  (let [text "SELECT * FROM foo WHERE bar = 'baz';"
        stmts (parse text)]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo WHERE bar = 'baz'" (first stmts)))))

(deftest single-quoted-value-with-single-quotes
  "Verifies that we can handle single-quoted strings that contain single
   quotes that are either escaped using backslashes or using consecutive
   single quote characters."
  (let [lines ["SELECT * FROM foo WHERE bar = 'single''quote';\n"
               "SELECT * FROM foo WHERE bar = 'single\\'quote';\n"]
        stmts (parse (apply str lines))]
    (is (= 2 (count stmts)))
    (is (= "SELECT * FROM foo WHERE bar = 'single''quote'" (first stmts)))
    (is (= "SELECT * FROM foo WHERE bar = 'single\\'quote'" (last stmts)))))

(deftest double-quoted-name
  "Verifies that we can handle a double-quoted name."
  (let [text "SELECT \"id\", \"name\" FROM foo;\n"
        stmts (parse text)]
    (is (= 1 (count stmts)))
    (is (= "SELECT \"id\", \"name\" FROM foo") (first stmts))))

(deftest quoted-values-with-semicolons
  "Verifies that we can handle quoted values that contain semicolons."
  (let [text "SELECT \"id;\" FROM foo WHERE bar = 'baz;';\n"
        stmts (parse text)]
    (is (= 1 (count stmts)))
    (is (= "SELECT \"id;\" FROM foo WHERE bar = 'baz;'") (first stmts))))

(deftest false-c-comment-start
  "Verifies that we can handle a slash in a statement."
  (let [stmts (parse "BOGUS / STATEMENT;")]
    (is (= 1 (count stmts)))
    (is (= "BOGUS / STATEMENT" (first stmts)))))

(deftest false-c-comment-end
  "Verifies that we can handle asterisks in a C-style comment."
  (let [lines ["/* Asterisks * in * comment. */\n"
               "SELECT * FROM foo;\n"]
        stmts (parse (apply str lines))]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo") (first stmts))))

(deftest false-line-comment-start
  "Verifies that we can handle a hyphen in a statement."
  (let [stmts (parse "SELECT foo - bar FROM baz;\n")]
    (is (= 1 (count stmts)))
    (is (= "SELECT foo - bar FROM baz") (first stmts))))

(deftest nested-c-comments
  "Verifies that we can correctly handle nested C-style comments."
  (let [lines ["/* These /* are /* nested */ comments */ */\n"
               "SELECT * FROM foo;\n"]
        stmts (parse (apply str lines))]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo") (first stmts))))

(deftest comments-in-statements
  "Verifies that we can correctly handle comments in statements."
  (let [lines ["SELECT * FROM foo -- a comment in a statement\n"
               "    WHERE bar = 'baz';"
               "SELECT * FROM foo /* JOIN bar */ WHERE baz = 1;\n"]
        stmts (parse (apply str lines))]
    (is (= 2 (count stmts)))
    (is (= "SELECT * FROM foo     WHERE bar = 'baz'" (first stmts)))
    (is (= "SELECT * FROM foo  WHERE baz = 1" (last stmts)))))

(deftest extra-comments
  "Verifies that we can correctly handle multiple comments."
  (let [lines [""
               "-- This is a comment.\n"
               "\n"
               "/* This is another\n"
               " * comment.\n"
               " */\n"
               "\n"
               "-- Yet another comment.\n"
               "SELECT * FROM foo;\n"]
        stmts (parse (apply str lines))]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))))

(deftest comments-between-statements
  "Verifies that we can correctly handle comments between statements."
  (let [lines ["SELECT * FROM foo;\n"
               "-- A comment.\n"
               "SELECT * FROM bar;\n"
               "/* A comment. */\n"
               "SELECT * FROM baz;\n"]
        stmts (parse (apply str lines))]
    (is (= 3 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))
    (is (= "SELECT * FROM bar" (second stmts)))
    (is (= "SELECT * FROM baz" (last stmts)))))

(deftest unterminated-statement
  "Verifies that we can handle an unterminated statement."
  (let [stmts (parse "SELECT * FROM foo")]
    (is (= 1 (count stmts)))
    (is (= "SELECT * FROM foo" (first stmts)))))

(deftest empty-file
  "Verifies that we can handle a completely empty file."
  (is (empty? (parse ""))))

(deftest unterminated-quoted-name
  "Verifies that we get an exception for an unterminated quoted name."
  (try+
   (let [smts (parse "SELECT \"foo FROM bar;\n")]
     (fail "No exception thrown."))
   (catch [:type :kameleon.sql-reader/unterminated-quoted-name] _
     (pass  "Correct exception thrown."))
   (catch Object _
     (fail "Unexpected exception thrown."))))

(deftest unterminated-string
  "Verifies that we get an exception for an unterminated string."
  (try+
   (let [stmts (parse "SELECT * FROM foo WHERE bar = 'baz;\n")]
     (fail "No exception thrown."))
   (catch [:type :kameleon.sql-reader/unterminated-string] _
     (pass "Correct exception thrown."))
   (catch Object _
     (fail "Unexpected exception thrown."))))

(deftest unterminated-string-with-escaped-character
  "Verifies that we get an exception for an unterminated string when the end of
   the file is preceded by a backslash."
  (try+
   (let [stmts (parse "SELECT * FROM foo WHERE bar = 'BAZ\\")]
     (fail "No exception thrown."))
   (catch [:type :kameleon.sql-reader/unterminated-string] _
     (pass "Correct exception thrown."))
   (catch Object _
     (fail "Unexpected exception thrown."))))

(deftest hyphen-at-eof
  "Verifies that we can handle a hyphen at the end of a file."
  (let [stmts (parse "SELECT -")]
    (assert (= 1 (count stmts)))
    (assert (= "SELECT -" (first stmts)))))

(deftest unterminated-c-comment
  "Verifies that we get an exception for an unterminated C-style comment."
  (try+
   (let [stmts (parse "/* This is an unterminated C-style comment.\n")]
     (fail "No exception thrown."))
   (catch [:type :kameleon.sql-reader/unterminated-c-comment] _
     (pass "Correct exception thrown."))
   (catch Object _
     (fail "Incorrect exception thrown."))))

(deftest unterminated-c-comment-end-candidate
  "Verifies that we get an exception for an unterminated C-style comment when
   the end of the file is preceded by an asterisk."
  (try+
   (let [stmts (parse "/* This is an unterminated C-style comment *")]
     (fail "No exception thrown."))
   (catch [:type :kameleon.sql-reader/unterminated-c-comment] _
     (pass "Correct exception thrown."))
   (catch Object _
     (fail "Incorrect exception thrown."))))
