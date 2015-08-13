(ns donkey.services.filesystem.metadata-templates
  (:use [donkey.services.filesystem.common-paths])
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.clients.metadata :as metadata]))

(def user-id-ks [:created_by :modified_by])

(defn- list-metadata-templates
  []
  (metadata/list-templates))

(defn- admin-list-metadata-templates
  []
  (metadata/admin-list-templates))

(defn- view-metadata-template
  [id]
  (metadata/get-template id))

(defn- view-metadata-attribute
  [id]
  (metadata/get-attribute id))

(defn- add-metadata-template
  "Adds a new metadata template."
  [template]
  (metadata/admin-add-template template))

(defn- remove-usernames-from-template
  [template]
  (let [remove-username-fields     (fn [m] (apply dissoc m user-id-ks))
        remove-all-username-fields (fn [s] (doall (map remove-username-fields s)))]
    (update-in (remove-username-fields template) [:attributes] remove-all-username-fields)))

(defn- update-metadata-template
  "Updates a Metadata Template and adds or updates its associated Attributes. Also deletes any
   orphaned Attributes."
  [template-id template]
  (metadata/admin-update-template template-id (remove-usernames-from-template template)))

(defn- delete-metadata-template
  "Sets a Metadata Template's deleted flag to 'true'."
  [template-id]
  (metadata/admin-delete-template template-id))

(defn do-metadata-template-list
  []
  (list-metadata-templates))

(with-pre-hook! #'do-metadata-template-list
  (fn []
    (log-call "do-metadata-template-list")))

(with-post-hook! #'do-metadata-template-list (log-func "do-metadata-template-list"))

(defn do-metadata-template-view
  [id]
  (view-metadata-template id))

(with-pre-hook! #'do-metadata-template-view
  (fn [id]
    (log-call "do-metadata-template-view" id)))

(with-post-hook! #'do-metadata-template-view (log-func "do-metadata-template-view"))

(defn do-metadata-attribute-view
  [id]
  (view-metadata-attribute id))

(with-pre-hook! #'do-metadata-attribute-view
  (fn [id]
    (log-call "do-metadata-attribute-view" id)))

(with-post-hook! #'do-metadata-attribute-view (log-func "do-metadata-attribute-view"))

(defn do-metadata-template-admin-list
  []
  (admin-list-metadata-templates))

(with-pre-hook! #'do-metadata-template-admin-list
  (fn []
    (log-call "do-metadata-template-admin-list")))

(with-post-hook! #'do-metadata-template-admin-list (log-func "do-metadata-template-admin-list"))

(defn do-metadata-template-add
  [template]
  (add-metadata-template template))

(with-pre-hook! #'do-metadata-template-add
  (fn [body]
    (log-call "do-metadata-template-add")))

(with-post-hook! #'do-metadata-template-add (log-func "do-metadata-template-add"))

(defn do-metadata-template-edit
  [template-id template]
  (update-metadata-template template-id template))

(with-pre-hook! #'do-metadata-template-edit
  (fn [template-id template]
    (log-call "do-metadata-template-edit")))

(with-post-hook! #'do-metadata-template-edit (log-func "do-metadata-template-edit"))

(defn do-metadata-template-delete
  [template-id]
  (delete-metadata-template template-id)
  nil)

(with-pre-hook! #'do-metadata-template-delete
  (fn [template-id]
    (log-call "do-metadata-template-delete")))

(with-post-hook! #'do-metadata-template-delete (log-func "do-metadata-template-delete"))
