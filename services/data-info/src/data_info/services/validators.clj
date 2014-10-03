(ns data-info.services.validators
  (:use [clj-jargon.item-info]
        [clj-jargon.permissions]
        [clj-jargon.tickets]
        [clj-jargon.users]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-icat-direct.icat :as icat]
            [clj-jargon.init :as init]
            [data-info.util :as util]
            [data-info.util.config :as cfg]
            [data-info.services.icat :as dsi]))


(defn- num-paths-okay?
  [path-count]
  (<= path-count (cfg/max-paths-in-request)))


(defn- validate-path-count
  [count]
  (if-not (num-paths-okay? count)
    (throw+ {:error_code "ERR_TOO_MANY_PATHS"
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


(defn user-exists
  ([cm user]
   (when-not (user-exists? cm user)
     (throw+ {:error_code ERR_NOT_A_USER :user user})))

  ([user]
    (init/with-jargon (dsi/jargon-cfg) [cm]
      (user-exists cm user))))


(defn all-users-exist
  [cm users]
  (when-not (every? #(user-exists? cm %) users)
    (throw+ {:error_code ERR_NOT_A_USER
             :users (filterv #(not (user-exists? cm %1)) users)})))

(defn path-exists
  [cm path]
  (when-not (exists? cm path)
    (throw+ {:error_code ERR_DOES_NOT_EXIST
             :path path})))

(defn all-paths-exist
  [cm paths]
  (when-not (every? #(exists? cm %) paths)
    (throw+ {:error_code ERR_DOES_NOT_EXIST
             :paths (filterv #(not (exists? cm  %1)) paths)})))

(defn no-paths-exist
  [cm paths]
  (when (some #(exists? cm %) paths)
    (throw+ {:error_code ERR_EXISTS
             :paths (filterv #(exists? cm %) paths)})))

(defn path-readable
  [cm user path]
  (when-not (is-readable? cm user path)
    (throw+ {:error_code ERR_NOT_READABLE
             :path path
             :user user})))

(defn all-paths-readable
  [cm user paths]
  (when-not (every? #(is-readable? cm user %) paths)
    (throw+ {:error_code ERR_NOT_READABLE
             :path (filterv #(not (is-readable? cm user %)) paths)})))

(defn path-writeable
  [cm user path]
  (when-not (is-writeable? cm user path)
    (throw+ {:error_code ERR_NOT_WRITEABLE
             :path path})))

(defn all-paths-writeable
  [cm user paths]
  (when-not (paths-writeable? cm user paths)
    (throw+ {:paths (filterv #(not (is-writeable? cm user %)) paths)
             :error_code ERR_NOT_WRITEABLE})))

(defn path-not-exists
  [cm path]
  (when (exists? cm path)
    (throw+ {:path path
             :error_code ERR_EXISTS})))

(defn path-is-dir
  [cm path]
  (when-not (is-dir? cm path)
    (throw+ {:error_code ERR_NOT_A_FOLDER
             :path path})))

(defn path-is-file
  [cm path]
  (when-not (is-file? cm path)
    (throw+ {:error_code ERR_NOT_A_FILE
             :path path})))

(defn paths-are-files
  [cm paths]
  (when-not (every? #(is-file? cm %) paths)
    (throw+ {:error_code ERR_NOT_A_FILE
             :path (filterv #(not (is-file? cm %)) paths)})))

(defn path-satisfies-predicate
  [cm path pred-func? pred-err]
  (when-not (pred-func? cm  path)
    (throw+ {:paths path
             :error_code pred-err})))

(defn paths-satisfy-predicate
  [cm paths pred-func? pred-err]
  (when-not  (every? true? (mapv #(pred-func? cm %) paths))
    (throw+ {:error_code pred-err
             :paths (filterv #(not (pred-func? cm %)) paths)})))

(defn ownage?
  [cm user path]
  (owns? cm user path))

(defn user-owns-path
  [cm user path]
  (when-not (owns? cm user path)
    (throw+ {:error_code ERR_NOT_OWNER
             :user user
             :path path})))

(defn user-owns-paths
  [cm user paths]
  (let [belongs-to? (partial ownage? cm user)]
    (when-not (every? #(belongs-to? %) paths)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :paths (filterv #(not (belongs-to? %)) paths)}))))

(defn ticket-exists
  [cm user ticket-id]
  (when-not (ticket? cm (:username cm) ticket-id)
    (throw+ {:error_code ERR_TICKET_DOES_NOT_EXIST
             :user user
             :ticket-id ticket-id})))

(defn ticket-does-not-exist
  [cm user ticket-id]
  (when (ticket? cm (:username cm) ticket-id)
    (throw+ {:error_code ERR_TICKET_EXISTS
             :user user
             :ticket-id ticket-id})))

(defn all-tickets-exist
  [cm user ticket-ids]
  (when-not (every? #(ticket? cm (:username cm) %) ticket-ids)
    (throw+ {:ticket-ids (filterv #(not (ticket? cm (:username cm) %)) ticket-ids)
             :error_code ERR_TICKET_DOES_NOT_EXIST})))

(defn all-tickets-nonexistant
  [cm user ticket-ids]
  (when (some #(ticket? cm (:username cm) %) ticket-ids)
    (throw+ {:ticket-ids (filterv #(ticket? cm (:username cm) %) ticket-ids)
             :error_code ERR_TICKET_EXISTS})))


(defn valid-uuid-field
  "Validates that a given value is a UUID.

   Parameters:
     field-name - the name of the field holding the proposed UUID
     filed-val  - the proposed UUID"
  [^String field-name ^String field-val]
  (when-not (util/is-uuid? field-val)
    (throw+ {:error_code ERR_BAD_REQUEST
             :field      field-name
             :value      field-val})))
