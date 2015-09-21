(ns data-info.util.validators
  (:use [kameleon.uuids :only [is-uuid?]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.permissions :as perm]
            [clj-jargon.users :as user]
            [clojure-commons.error-codes :as error]
            [data-info.util.config :as cfg])
  (:import [clojure.lang IPersistentCollection]))


(defn ^Boolean good-string?
  "Checks that a string doesn't contain any problematic characters.

   Params:
     bad-chars - the characters that shouldn't be in the string
     to-check  - The string to check

   Returns:
     It returns false if the string contains at least one problematic character, otherwise false."
  ([^String to-check]
    (good-string? (seq (cfg/bad-chars)) to-check))
  ([^IPersistentCollection bad-chars ^String to-check]
    (let [chars-to-check (set (seq to-check))]
      (empty? (set/intersection (set bad-chars) chars-to-check)))))


(defn valid-bool-param
  "Validates that a given value is a Boolean.

   Parameters:
     param-name - the name of the param holding the proposed Boolean
     param-val  - the proposed Boolean

   Throws:
     It throws a map with of the following form.

       {:error_code ERR_BAD_REQUEST
        :param      param-name
        :value      param-val}"
  [^String param-name ^String param-val]
  (let [val (str/lower-case param-val)]
    (when-not (or (= val "true") (= val "false"))
      (throw+ {:error_code error/ERR_BAD_REQUEST
               :param      param-name
               :value      param-val}))))


(defn valid-uuid-param
  "Validates that a given value is a UUID.

   Parameters:
     param-name - the name of the param holding the proposed UUID
     param-val  - the proposed UUID

   Throws:
     It throws a map with of the following form.

       {:error_code ERR_BAD_REQUEST
        :param      param-name
        :value      param-val}"
  [^String param-name ^String param-val]
  (when-not (is-uuid? param-val)
    (throw+ {:error_code error/ERR_BAD_REQUEST
             :param      param-name
             :value      param-val})))


(defn- num-paths-okay?
  [path-count]
  (<= path-count (cfg/max-paths-in-request)))


(defn- validate-path-count
  [count]
  (if-not (num-paths-okay? count)
    (throw+ {:error_code error/ERR_TOO_MANY_RESULTS
             :count count
             :limit (cfg/max-paths-in-request)})))


(defn validate-num-paths
  [paths]
  (validate-path-count (count paths)))


(defn validate-num-paths-under-folder
  [user folder]
  (let [total (icat/number-of-all-items-under-folder user (cfg/irods-zone) folder)]
    (validate-path-count total)))


(defn validate-num-paths-under-paths
  [user paths]
  (let [sum-fn #(+ %1 (icat/number-of-all-items-under-folder user (cfg/irods-zone) %2))
        total (reduce sum-fn 0 paths)]
    (validate-path-count total)))


(defn all-paths-exist
  [cm paths]
  (when-not (every? #(item/exists? cm %) paths)
    (throw+ {:error_code error/ERR_DOES_NOT_EXIST
             :paths      (filterv #(not (item/exists? cm  %1)) paths)})))


(defn no-paths-exist
  [cm paths]
  (when (some #(item/exists? cm %) paths)
    (throw+ {:error_code error/ERR_EXISTS
             :paths (filterv #(item/exists? cm %) paths)})))


(defn path-exists
  [cm path]
  (when-not (item/exists? cm path)
    (throw+ {:error_code error/ERR_DOES_NOT_EXIST
             :path path})))


(defn path-not-exists
  [cm path]
  (when (item/exists? cm path)
    (throw+ {:path path
             :error_code error/ERR_EXISTS})))


(defn all-paths-readable
  [cm user paths]
  (when-not (every? #(perm/is-readable? cm user %) paths)
    (throw+ {:error_code error/ERR_NOT_READABLE
             :path       (filterv #(not (perm/is-readable? cm user %)) paths)})))


(defn path-readable
  [cm user path]
  (when-not (perm/is-readable? cm user path)
    (throw+ {:error_code error/ERR_NOT_READABLE
             :path path
             :user user})))


(defn all-paths-writeable
  [cm user paths]
  (when-not (perm/paths-writeable? cm user paths)
    (throw+ {:paths      (filterv #(not (perm/is-writeable? cm user %)) paths)
             :error_code error/ERR_NOT_WRITEABLE})))


(defn path-writeable
  [cm user path]
  (when-not (perm/is-writeable? cm user path)
    (throw+ {:error_code error/ERR_NOT_WRITEABLE
             :path path})))


(defn path-is-dir
  [cm path]
  (when-not (item/is-dir? cm path)
    (throw+ {:error_code error/ERR_NOT_A_FOLDER
             :path path})))


(defn path-is-file
  [cm path]
  (when-not (item/is-file? cm path)
    (throw+ {:error_code error/ERR_NOT_A_FILE
             :path path})))


(defn paths-are-files
  [cm paths]
  (when-not (every? #(item/is-file? cm %) paths)
    (throw+ {:error_code error/ERR_NOT_A_FILE
             :path       (filterv #(not (item/is-file? cm %)) paths)})))


(defn all-users-exist
  [cm users]
  (when-not (every? #(user/user-exists? cm %) users)
    (throw+ {:error_code error/ERR_NOT_A_USER
             :users      (filterv #(not (user/user-exists? cm %1)) users)})))


(defn user-exists
  ([cm user]
    (when-not (user/user-exists? cm user)
      (throw+ {:error_code error/ERR_NOT_A_USER :user user})))

  ([user]
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (user-exists cm user))))


(defn user-owns-path
  [cm user path]
  (when-not (perm/owns? cm user path)
    (throw+ {:error_code error/ERR_NOT_OWNER
             :user user
             :path path})))


(defn user-owns-paths
  [cm user paths]
  (let [belongs-to? (partial perm/owns? cm user)]
    (when-not (every? belongs-to? paths)
      (throw+ {:error_code error/ERR_NOT_OWNER
               :user user
               :paths (filterv #(not (belongs-to? %)) paths)}))))
