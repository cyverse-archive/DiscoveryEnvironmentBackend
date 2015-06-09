(ns donkey.services.filesystem.metadata-templates
  (:use [clojure-commons.core :only [remove-nil-values]]
        [donkey.auth.user-attributes :only [current-user]]
        [donkey.services.filesystem.common-paths]
        [kameleon.queries :only [get-user-id]]
        [kameleon.uuids :only [is-uuid?]]
        [korma.core]
        [korma.db :only [transaction with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as error-codes]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.clients.metadata :as metadata]
            [donkey.util.db :as db]
            [donkey.util.service :as service]))

(def user-id-ks [:created_by :modified_by])

(defn- load-user-id-map
  [ids]
  (->> (metadactyl/get-users-by-id ids)
       (:users)
       (map (juxt :id :username))
       (into {})))

(defn- extract-user-ids
  ([ms ks]
     (extract-user-ids ms ks []))
  ([ms ks ids]
     (set (remove nil? (concat ids (mapcat (apply juxt ks) ms))))))

(defn- user-ids-to-usernames
  ([ms ks]
     (user-ids-to-usernames ms ks (extract-user-ids ms ks)))
  ([ms ks user-id-map]
     (let [replace-user-id  (fn [m k] (assoc m k (user-id-map (m k))))
           replace-user-ids (fn [m] (reduce replace-user-id m ks))]
       (mapv replace-user-ids ms))))

(defn- list-metadata-templates
  []
  (update-in (metadata/list-templates) [:metadata_templates]
             user-ids-to-usernames user-id-ks))

(defn- admin-list-metadata-templates
  []
  (update-in (metadata/admin-list-templates) [:metadata_templates]
             user-ids-to-usernames user-id-ks))

(defn- load-template-user-id-map
  [{attrs :attributes :as template}]
  (->> ((apply juxt user-id-ks) template)
       (extract-user-ids attrs user-id-ks)
       (load-user-id-map)))

(defn- replace-template-user-ids
  ([template]
     (replace-template-user-ids template (load-template-user-id-map template)))
  ([template user-id-map]
     (assoc template
       :attributes  (user-ids-to-usernames (:attributes template) user-id-ks user-id-map)
       :created_by  (user-id-map (:created_by template))
       :modified_by (user-id-map (:modified_by template)))))

(defn- view-metadata-template
  [id]
  (replace-template-user-ids (metadata/get-template id)))

(defn- replace-attr-user-ids
  [attr]
  (let [user-id-map (load-user-id-map ((apply juxt user-id-ks) attr))]
    (assoc attr
      :created_by  (user-id-map (:created_by attr))
      :modified_by (user-id-map (:modified_by attr)))))

(defn- view-metadata-attribute
  [id]
  (replace-attr-user-ids (metadata/get-attribute id)))

(defn- add-metadata-template
  "Adds a new metadata template."
  [template]
  (-> (:id (metadactyl/get-authenticated-user))
      (metadata/admin-add-template template)
      (replace-template-user-ids)))

(defn- remove-usernames-from-template
  [template]
  (let [remove-username-fields     (fn [m] (apply dissoc m user-id-ks))
        remove-all-username-fields (fn [s] (doall (map remove-username-fields s)))]
    (update-in (remove-username-fields template) [:attributes] remove-all-username-fields)))

(defn- update-metadata-template
  "Updates a Metadata Template and adds or updates its associated Attributes. Also deletes any
   orphaned Attributes."
  [template-id template]
  (-> (:id (metadactyl/get-authenticated-user))
      (metadata/admin-update-template template-id (remove-usernames-from-template template))
      (replace-template-user-ids)))

(defn- delete-metadata-template
  "Sets a Metadata Template's deleted flag to 'true'."
  [template-id]
  (-> (:id (metadactyl/get-authenticated-user))
      (metadata/admin-delete-template template-id)))

(defn do-metadata-template-list
  []
  {:metadata_templates (list-metadata-templates)})

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
  {:metadata_templates (admin-list-metadata-templates)})

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
