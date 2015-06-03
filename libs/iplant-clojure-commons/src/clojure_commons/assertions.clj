(ns clojure-commons.assertions
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [clojure-commons.error-codes :as ce]))

(defn not-found
  "Throws an exception indicating that an object wasn't found."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_FOUND
           :reason     (string/join " " [desc id "not found"])}))

(defn not-owner
  "Throws an exception indicating that the user isn't permitted to perform the requested option."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_OWNER
           :reason     (str "authenticated user doesn't own " desc ", " id)}))

(defn not-unique
  "Throws an exception indicating that multiple objects were found when only one was expected."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_UNIQUE
           :reason     (string/join " " [desc id "not unique"])}))

(defn bad-request
  "Throws an exception indicating that the incoming request is invalid."
  [reason]
  (throw+ {:error_code ce/ERR_BAD_REQUEST
           :reason     reason}))

(defn assert-found
  "Asserts that an object to modify or retrieve was found."
  [obj desc id]
  (if (nil? obj)
    (not-found desc id)
    obj))

(defn assert-not-found
  "Asserts that an object that is being created is not a duplicate."
  [obj desc id]
  (when-not (nil? obj)
    (not-unique desc id)))

(defn assert-valid
  "Throws an exception if an arbitrary expression is false."
  [valid? & msgs]
  (when-not valid?
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :message    (string/join " " msgs)})))

(defn request-failure
  "Throws an exception indicating that a request failed for an unexpected reason."
  [& msgs]
  (throw+ {:error_code ce/ERR_REQUEST_FAILED
           :message    (string/join " " msgs)}))
