(ns dewey.util-test
  (:use midje.sweet
        dewey.util))

(def ^{:private true :const true} ordinaries
  "~!@#&`1234567890-=QWERTYUIOP}qwertyuiop]ASDFGHJKL:\"asdfghjkl;'
  ZXCVBNM<>zxcvbnm,/ ")

(facts "about `sql-glob->regex`"
  (fact "empty string maps to empty expression"
    (sql-glob->regex "") => #"")
  (fact "leaves text that aren't special characters in SQL glob patterns or regular expressions
        alone."
    (sql-glob->regex ordinaries) => (re-pattern ordinaries))
  (fact "_ maps to ."
    (sql-glob->regex "_") => #"."
    (sql-glob->regex "_a") => #".a"
    (sql-glob->regex "a_") => #"a."
    (sql-glob->regex "a_b") => #"a.b"
    (sql-glob->regex "a_b_c") => #"a.b.c"
    (sql-glob->regex "a__b") => #"a..b")
  (fact "% maps to .*"
    (sql-glob->regex "%") => #".*"
    (sql-glob->regex "%a") => #".*a"
    (sql-glob->regex "a%") => #"a.*"
    (sql-glob->regex "a%b") => #"a.*b"
    (sql-glob->regex "a%b%c") => #"a.*b.*c")
  (fact "escaped characters don't get translated"
    (sql-glob->regex "\\_") => #"_"
    (sql-glob->regex "\\%") => #"%"
    (sql-glob->regex "\\\\") => #"\\"
    (sql-glob->regex "\\a") => #"a")
  (fact "special regex characters get escaped"
    (sql-glob->regex "[") => #"\["
    (sql-glob->regex ".") => #"\."
    (sql-glob->regex "^") => #"\^"
    (sql-glob->regex "$") => #"\$"
    (sql-glob->regex "?") => #"\?"
    (sql-glob->regex "*") => #"\*"
    (sql-glob->regex "+") => #"\+"
    (sql-glob->regex "{") => #"\{"
    (sql-glob->regex "|") => #"\|"
    (sql-glob->regex "(") => #"\("
    (sql-glob->regex ")") => #"\)"))


(facts "about `get-parent-path`"
  (fact "returns parent without trailing slash"
    (get-parent-path "/parent/child") => "/parent"))
