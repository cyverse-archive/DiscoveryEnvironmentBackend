(ns conrad.config
  (:use [clojure.string :only (blank? trim)])
  (:require [clojure-commons.props :as cc-props]
            [clojure.tools.logging :as log]))

(defn prop-file
  "The name of the properties file."
  []
  "zkhosts.properties")

(defn zk-props
  "The properties loaded from the properties file."
  []
  (cc-props/parse-properties (prop-file)))

(defn zk-url
  "The URL used to connect to zookeeper."
  []
  (get (zk-props) "zookeeper"))

(def props
  "The properites that have been loaded from Zookeeper."
  (atom nil))

(def required-props
  "The list of required properties."
  (ref []))

(def configuration-is-valid
  "True if the configuraiton is valid."
  (atom true))

(defn- record-missing-prop
  "Records a property that is missing.  Instead of failing on the first
   missing parameter, we log the missing parameter, mark the configuration
   as invalid and keep going so that we can log as many configuration errors
   as possible in one run."
  [prop-name]
  (log/error "required configuration setting" prop-name "is empty or"
             "undefined")
  (reset! configuration-is-valid false))

(defn- record-invalid-prop
  "Records a property that is invalid.  Instead of failing on the first
   invalid parameter, we log the parameter name, mark the configuraiton as
   invalid and keep going so that we can log as many configuration errors as
   possible in one run."
  [prop-name t]
  (log/error "invalid configuration setting for" prop-name ":" t)
  (reset! configuration-is-valid false))

(defn- get-str
  "Gets a string property from the properties that were loaded from
   Zookeeper."
  [prop-name]
  (let [value (get @props prop-name)]
    (log/trace prop-name "=" value)
    (when (blank? value)
      (record-missing-prop prop-name))
    value))

(defn- get-int
  "Gets an integer property from the properties that were loaded from
   Zookeeper."
  [prop-name]
  (try
    (Integer/valueOf (get-str prop-name))
    (catch NumberFormatException e
      (do (record-invalid-prop prop-name e) 0))))

(defn- is-space
  "Determines whether or not a character is whitespace."
  [c]
  (Character/isWhitespace (char c)))

(declare escaped-char quoted-string elm leading-whitespace next-elm)

(defn- escaped-char
  "Retrieves a backslash-escaped character from a comma-delimited list.  Any
   character can be escaped, including a comma.  If a comma is escaped then
   it's treated as part of the current element rather than an element
   separator.  If a single quote character is escaped then it's not treated as
   the beginning or end of a quoted string.  The only way to include a literal
   backslash in a list element is to escape it with another backslash."
  [res [c & cs] prev-state]
  (cond (nil? c) [res cs]
        :else    #(prev-state (conj res c) cs)))

(defn- quoted-string
  "Extracts element values from a quoted string in a comma-delimited list."
  [res [c & cs]]
  (cond (nil? c) [res cs]
        (= c \') #(elm res cs)
        (= c \\) #(escaped-char res cs quoted-string)
        :else    #(quoted-string (conj res c) cs)))

(defn- elm
  "Extracts an element from a comma-delimited list."
  [res [c & cs]]
  (cond (nil? c) [res cs]
        (= c \,) [res cs]
        (= c \\) #(escaped-char res cs elm)
        (= c \') #(quoted-string res cs)
        :else    #(elm (conj res c) cs)))

(defn- leading-whitespace
  "Skips leading whitespace in a comma-delimited list element."
  [res [c & cs :as all]]
  (cond (nil? c)     [res cs]
        (is-space c) #(leading-whitespace res cs)
        :else        #(elm res all)))

(defn- next-elm
  "Obtains the next element from a comma-delimited list."
  [res cs]
 #(leading-whitespace res cs))

(defn parse-comma-delimited-list
  "Parses a comma-delimited list that uses single quotes to quote list
   elements."
  [s]
  (loop [res [] [elm s] (trampoline #(next-elm [] s))]
    (if-not (empty? elm)
      (recur (conj res (apply str elm)) (trampoline #(next-elm [] s)))
      res)))

(defn- get-vector
  "Gets a vector property from the properties that were loaded from
   Zookeeper."
  [prop-name]
  (parse-comma-delimited-list (get-str prop-name)))

(defmacro defprop
  "Defines a property."
  [sym docstr & init-forms]
  `(def ~(with-meta sym {:doc docstr}) (memoize (fn [] ~@init-forms))))

(defn- required
  "Registers a property in the list of required properties."
  [prop]
  (dosync (alter required-props conj prop)))

(required
  (defprop listen-port
    "The port to listen to for incoming connections."
    (get-int "conrad.listen-port")))

(required
  (defprop db-vendor
    "The name of the database vendor (e.g. postgresql)."
    (get-str "conrad.db.vendor")))

(required
  (defprop db-host
    "the host name or IP address used to connect to the database."
    (get-str "conrad.db.host")))

(required
  (defprop db-port
    "The port used to connect to the database."
    (get-str "conrad.db.port")))

(required
  (defprop db-name
    "The name of the database."
    (get-str "conrad.db.name")))

(required
  (defprop db-user
    "The username used to authenticate to the databse."
    (get-str "conrad.db.user")))

(required
  (defprop db-password
    "The password used to authenticate to the database."
    (get-str "conrad.db.password")))

(required
  (defprop db-max-idle-time
    "The maximum amount of time to retain idle database connections."
    (* (get-int "conrad.db.max-idle-minutes") 60)))

(required
  (defprop cas-server
    "The URL prefix to use when connecting to the CAS server."
    (get-str "conrad.cas.server")))

(required
  (defprop server-name
    "The name of the local server to provide to CAS."
    (get-str "conrad.server-name")))

(required
  (defprop group-attr-name
    "The name of the user attribute containing group membership information."
    (get-str "conrad.cas.group-attr-name")))

(required
  (defprop allowed-groups
    "The names of the groups that are permitted to access secured services."
    (get-vector "conrad.cas.allowed-groups")))

(required
  (defprop uid-domain
    "The user domain of the current CAS authenticated user."
    (get-str "conrad.uid-domain")))

(defn configuration-valid
  "Ensures that all required properties are valued."
  []
  (dorun (map #(%) @required-props))
  @configuration-is-valid)
