(ns kameleon.sql-reader
  (:use [clojure.java.io :only [file reader]]
        [slingshot.slingshot :only [throw+]]))

(def test-file
  "/Users/dennis/src/iplant/ua/de-database-schema/src/main/data/01_data_formats.sql")

(defn char-seq
  "Returns a lazy sequence of characters obtained from a reader."
  [rdr]
  (let [c (.read rdr)]
    (if-not (< c 0)
      (lazy-seq (cons (char c) (char-seq rdr)))
      '())))

(defn is-space
  "Determines whether or not a character is whitespace."
  [c]
  (Character/isWhitespace (char c)))

(declare c-comment-end-candidate c-comment c-comment-candidate line-comment
         line-comment-candidate single-quoted-string double-quoted-string
         statement-beginning statement-base statement)

(defn c-comment-end-candidate
  "Handles what may or may not be the end of a C-style comment.  If the comment
   is really ending at this point then the state switches back to whatever it
   was before the comment started.  Otherwise, the state switches back to
   c-comment."
  [ps res [c & cs]]
  (cond (nil? c) (throw+ {:type ::unterminated-c-comment})
        (= c \/) #(ps res cs)
        :else    #(c-comment ps res cs)))

(defn c-comment
  "Handles characters in a C-style comment.  If the character is a slash then
   there may be a nested comment.  In that case, we switch to the
   c-comment-candidate state.  If the character is an asterisk then we might
   have encountered a comment terminator.  In that case, we siwtch to the
   c-comment-end-candidate state.  Otherwise, we continue discarding
   characters."
  [ps res [c & cs]]
  (cond (nil? c) (throw+ {:type ::unterminated-c-comment})
        (= c \/) #(c-comment-candidate (partial c-comment ps) res cs)
        (= c \*) #(c-comment-end-candidate ps res cs)
        :else    #(c-comment ps res cs)))

(defn c-comment-candidate
  "Handles what may be the start of a C-style comment.  If a comment is really
   starting then we switch to the c-comment state.  Otherwise, we go back to
   the previous state."
  [ps res [c & cs]]
  (cond (nil? c) [(conj res \/) cs]
        (= c \*) #(c-comment ps res cs)
        :else    #(ps (conj res \/ c) cs)))

(defn line-comment
  "Handles a line comment, which continues until the end of the line is
   reached.  If the current line is a newline then the comment terminates.
   Otherwise we continue discarding characters."
  [ps res [c & cs]]
  (cond (nil? c)       [res cs]
        (= c \newline) #(ps res cs)
        :else          #(line-comment ps res cs)))

(defn line-comment-candidate
  "Handles what may be the beginning of a line comment.  If the next character
   is a hyphen then we switch to the line-comment state.  Otherwise, we switch
   back to the previous state."
  [ps res [c & cs]]
  (cond (nil? c) [(conj res \-) cs]
        (= c \-) #(line-comment ps res cs)
        :else    #(ps (conj res \- c) cs)))

(defn escaped-char
  "Handles an escaped character in a single-quoted string."
  [res [c & cs]]
  (cond (nil? c) (throw+ {:type ::unterminated-string})
        :else    #(single-quoted-string (conj res c) cs)))

(defn single-quoted-string
  "Handles a single-quoted string.  If the next character is a single quote
   then the string is being terminated and we switch back to the statement-base
   state.  Otherwise, we continue accumulating characters in the string."
  [res [c & cs]]
  (cond (nil? c) (throw+ {:type ::unterminated-string})
        (= c \\) #(escaped-char (conj res c) cs)
        (= c \') #(statement-base (conj res c) cs)
        :else    #(single-quoted-string (conj res c) cs)))

(defn double-quoted-string
  "Handles a double-quoted string.  If the next character is a double quote
   then the string is being terminated and we switch back to the statement-base
   state.  Otherwise, we continue accumulating characters in the string."
  [res [c & cs]]
  (cond (nil? c) (throw+ {:type ::unterminated-quoted-name})
        (= c \") #(statement-base (conj res c) cs)
        :else    #(double-quoted-string (conj res c) cs)))

(defn statement-beginning
  "Skips to the beginning of the next SQL statement."
  [res [c & cs :as all]]
  (cond (nil? c)     [res cs]
        (is-space c) #(statement-beginning res cs)
        (= c \-)     #(line-comment-candidate statement-beginning res cs)
        (= c \/)     #(c-comment-candidate statement-beginning res cs)
        :else        #(statement-base res all)))

(defn statement-base
  "Handles the base state for identifying SQL statements.  If there are no more
   characters then we return whatever we've accumulated so far.  If the next
   character is a semicolon then the statement is complete, so we return it.
   If the next candidate is a slash then we may be at the start of a C-style
   comment, so we switch to the c-comment-candidate state.  If the next
   character is a hyphen then we might be at the beginning of a line comment,
   so we switch to the line-comment-candidate state.  If the next character is
   a single or double quote then we switch to the single-quoted-string or
   double-quoted-string state, respectively.  Otherwise, we continue
   accumulating characters in the statement."
  [res [c & cs]]
  (cond (nil? c) [res cs]
        (= c \;) [res cs]
        (= c \/) #(c-comment-candidate statement-base res cs)
        (= c \-) #(line-comment-candidate statement-base res cs)
        (= c \') #(single-quoted-string (conj res c) cs)
        (= c \") #(double-quoted-string (conj res c) cs)
        :else    #(statement-base (conj res c) cs)))

(defn statement
  "Extracts the next SQL statement from a character sequence, skipping any
   leading whitespace."
  [res cs]
  #(statement-beginning res cs))

(defn sql-statements
  "Returns a sequence of SQL statements in the data that a reader points to."
  [rdr]
  (loop [res [] [stmt cs] (trampoline #(statement [] (char-seq rdr)))]
    (if (empty? stmt)
      res
      (recur (conj res (apply str stmt))
             (trampoline #(statement [] cs))))))
