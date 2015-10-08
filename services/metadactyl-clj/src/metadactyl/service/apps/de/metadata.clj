(ns metadactyl.service.apps.de.metadata
  "DE app metadata services."
  (:use [clojure.java.io :only [reader]]
        [clojure-commons.client :only [build-url]]
        [kameleon.app-groups :only [add-app-to-category
                                    decategorize-app
                                    get-app-subcategory-id
                                    remove-app-from-category]]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.service.apps.de.validation :only [app-publishable?]]
        [metadactyl.util.config :only [workspace-beta-app-category-id
                                       workspace-favorites-app-category-index]]
        [metadactyl.validation :only [get-valid-user-id verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [metadactyl.persistence.app-metadata :as amp]
            [metadactyl.service.apps.de.docs :as app-docs]
            [metadactyl.translations.app-metadata :as atx]
            [metadactyl.util.config :as config]))

(defn- validate-app-existence
  "Verifies that apps exist."
  [app-id]
  (amp/get-app app-id))

(defn- validate-app-ownership
  "Verifies that a user owns an app."
  [username app-id]
  (when-not (every? (partial = username) (amp/app-accessible-by app-id))
    (throw+ {:type  :clojure-commons.exception/bad-request-field
             :error (str username " does not own app " app-id)})))

(defn- validate-deletion-request
  "Validates an app deletion request."
  [user req]
  (when (empty? (:app_ids req))
    (throw+ {:type  :clojure-commons.exception/bad-request-field
             :error "no app identifiers provided"}))
  (when (and (nil? (:username user)) (not (:root_deletion_request req)))
    (throw+ {:type  :clojure-commons.exception/bad-request-field
             :error "no username provided for non-root deletion request"}))
  (dorun (map validate-app-existence (:app_ids req)))
  (when-not (:root_deletion_request req)
    (dorun (map (partial validate-app-ownership (:username user)) (:app_ids req)))))

(defn permanently-delete-apps
  "This service removes apps from the database rather than merely marking them as deleted."
  [user req]
  (validate-deletion-request user req)
  (transaction
    (dorun (map amp/permanently-delete-app (:app_ids req)))
    (amp/remove-workflow-map-orphans))
  nil)

(defn delete-apps
  "This service marks existing apps as deleted in the database."
  [user req]
  (validate-deletion-request user req)
  (transaction (dorun (map amp/delete-app (:app_ids req))))
  {})

(defn delete-app
  "This service marks an existing app as deleted in the database."
  [user app-id]
  (validate-app-existence app-id)
  (validate-app-ownership (:username user) app-id)
  (amp/delete-app app-id)
  {})

(defn preview-command-line
  "This service sends a command-line preview request to the JEX."
  [body]
  (let [jex-req (atx/template-cli-preview-req body)]
    (cheshire/decode-stream
     ((comp reader :body)
      (client/post
       (build-url (config/jex-base-url) "arg-preview")
       {:body             (cheshire/encode jex-req)
        :content-type     :json
        :as               :stream}))
     true)))

(defn rate-app
  "Adds or updates a user's rating and comment ID for the given app. The request must contain either
   the rating or the comment ID, and the rating must be between 1 and 5, inclusive."
  [user app-id {:keys [rating comment_id] :as request}]
  (validate-app-existence app-id)
  (let [user-id (get-valid-user-id (:username user))]
    (when (and (nil? rating) (nil? comment_id))
      (throw+ {:type  :clojure-commons.exception/bad-request-field
               :error (str "No rating or comment ID given")}))
    (when (or (> 1 rating) (> rating 5))
      (throw+ {:type  :clojure-commons.exception/bad-request-field
               :error (str "Rating must be an integer between 1 and 5 inclusive."
                                " Invalid rating (" rating ") for App ID " app-id)}))
    (amp/rate-app app-id user-id request)
    (amp/get-app-avg-rating app-id)))

(defn delete-app-rating
  "Removes a user's rating and comment ID for the given app."
  [user app-id]
  (validate-app-existence app-id)
  (let [user-id (get-valid-user-id (:username user))]
    (amp/delete-app-rating app-id user-id)
    (amp/get-app-avg-rating app-id)))

(defn- get-favorite-category-id
  "Gets the current user's Favorites category ID."
  [user]
  (get-app-subcategory-id
    (:root_category_id (get-workspace (:username user)))
    (workspace-favorites-app-category-index)))

(defn add-app-favorite
  "Adds the given app to the current user's favorites list."
  [user app-id]
  (let [app (amp/get-app app-id)
        fav-category-id (get-favorite-category-id user)]
    (add-app-to-category app-id fav-category-id))
  nil)

(defn remove-app-favorite
  "Removes the given app from the current user's favorites list."
  [user app-id]
  (let [app (amp/get-app app-id)
        fav-category-id (get-favorite-category-id user)]
  (remove-app-from-category app-id fav-category-id))
  nil)

(defn- publish-app
  [user {app-id :id :keys [references categories] :as app}]
  (transaction
    (amp/update-app app true)
    (app-docs/add-app-docs user app-id app)
    (amp/set-app-references app-id references)
    (amp/set-app-suggested-categories app-id categories)
    (decategorize-app app-id)
    (add-app-to-category app-id (uuidify (workspace-beta-app-category-id))))
  nil)

(defn make-app-public
  [user {app-id :id :as app}]
  (verify-app-ownership user (validate-app-existence app-id))
  (let [[publishable? reason] (app-publishable? app-id)]
    (if publishable?
      (publish-app user app)
      (throw+ {:type  :clojure-commons.exception/bad-request-field
               :error reason}))))

(defn get-app
  "This service obtains an app description that can be used to build a job submission form in
   the user interface."
  [app-id]
  (amp/get-app app-id))

(defn get-param-definitions
  [app-id]
  (filter (comp nil? :external_app_id) (amp/get-app-parameters app-id)))
