(ns data-info.services.space-handling
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.permissions :only [process-parent-dirs]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.item-ops]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [clojure.set :as set]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.validations :as valid]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]
            [data-info.services.common-paths :as paths]))


(defn- paths-contain-char
  [paths char]
  (when-not (valid/good-string? char)
    (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
             :character char}))

  (apply merge (map #(hash-map %1 (not (nil? (re-seq (re-pattern char) %1)))) paths)))

(defn- parent-dirs
  [user path]
  (let [pdirs (atom [])]
    (process-parent-dirs
     #(reset! pdirs (conj @pdirs %1))
     #(and (not (nil? %1))
           (not (= %1 (paths/user-home-dir user)))) path)
    @pdirs))

(defn- all-parent-dirs
  [user paths]
  (sort-by
   #(count (string/split %1 #"/")) >
   (vec (apply set/union (map #(set (parent-dirs user %1)) paths)))))

(defn- looped-new-name
  "Iterates over the path, appending a _# to the end until a path that doesn't already exist is
   found."
  [cm path new-char]
  (loop [idx 0]
    (let [new-path (string/replace path #" " (str new-char "_" idx))]
      (if-not (exists? cm new-path)
        new-path
        (recur (inc idx))))))

(defn- new-name
  "Creates a new name for the given path by replacing all spaces with the provided new-char.
   If the path is indicated to be a parent with the :parent flag, then the new name will NOT have
   a _# appended to it if the name already exists."
  [cm path new-char & {:keys [parent] :or {parent false}}]
  (let [new-path (string/replace path #" " new-char)]
    (if (or parent (not (exists? cm new-path)))
      new-path
      (looped-new-name cm path new-char))))

(defn- has-space?
  "Returns a truthy value if the path contains a space somewhere in it."
  [path]
  (re-seq (re-pattern " ") path))

(defn- move-spacey-path
  "Takes in a path and a new-char, replaces all spaces in the path with new-char, and returns a map
   with the original path as the key and the new path as the value. If the path is a parent, then
   the new name will not have a _# appended to it if it already exists."
  [cm user path new-char & {:keys [parent] :or {parent false}}]
  (when (has-space? (ft/basename path))
    (let [new-basename (new-name cm (ft/basename path) new-char :parent parent)
          new-path     (ft/path-join (ft/dirname path) new-basename)]
      (if (and (not (exists? cm new-path)) (exists? cm path))
        (move cm path new-path :user user :admin-users (cfg/irods-admins)))
      {path new-path})))

(defn- fix-return-map
  [retmap new-char]
  (into {} (map #(hash-map (first %1) (string/replace (last %1) #" " new-char)) (seq retmap))))

(defn- replace-spaces
  "Generates new paths by replacing all spaces with new-char."
  [user paths new-char]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/all-paths-exist cm paths)
    (validators/user-owns-paths cm user paths)
    (when-not (valid/good-string? new-char)
      (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
               :character new-char}))
    (let [parent-dirs (all-parent-dirs user paths)]
      (validators/user-owns-paths cm user parent-dirs)

      (let [mv-base         #(move-spacey-path cm user %1 new-char :parent false)
            mv-parent       #(move-spacey-path cm user %1 new-char :parent true)
            basename-merges (apply merge (map mv-base paths))
            parent-merges   (apply merge (map mv-parent parent-dirs))]
        {:paths (fix-return-map basename-merges new-char)}))))

(defn do-paths-contain-space
  [params {paths :paths}]
  {:paths (paths-contain-char paths " ")})

(with-pre-hook! #'do-paths-contain-space
  (fn [params body]
    (dul/log-call "do-path-contain-space" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})
    (when-not (every? true? (mapv string? (:paths body)))
      (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
               :field      "paths"}))
    (validators/validate-num-paths (:paths body))))

(with-post-hook! #'do-paths-contain-space (dul/log-func "do-paths-contain-space"))

(defn do-replace-spaces
  [{user :user} {paths :paths}]
  (replace-spaces user paths "_"))

(with-pre-hook! #'do-replace-spaces
  (fn [params body]
    (dul/log-call "do-substitute-spaces" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})
    (when-not (every? true? (mapv string? (:paths body)))
      (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
               :field      "paths"}))
    (validators/validate-num-paths (:paths body))))

(with-post-hook! #'do-replace-spaces (dul/log-func "do-replace-spaces"))
