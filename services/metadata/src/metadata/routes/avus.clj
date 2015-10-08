(ns metadata.routes.avus
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.common]
        [metadata.routes.domain.avus]
        [ring.util.http-response :only [ok]])
  (:require [metadata.services.avus :as avus]))

(defroutes* avus
  (context* "/filesystem/data/:data-id/avus" []
    :tags ["avus"]

    (GET* "/" []
      :path-params [data-id :- TargetIdPathParam]
      :return DataItemMetadataTemplateList
      :summary "View all Metadata AVUs on a File/Folder"
      :description "Lists all AVUs associated with the data item, grouped by Metadata Template."
      (ok (avus/list-metadata-template-avus data-id)))

    (POST* "/copy" []
      :path-params [data-id :- TargetIdPathParam]
      :query [{:keys [user force]} AvuCopyQueryParams]
      :body [body (describe DataItemList "The destination files and folders.")]
      :summary "Copy all Metadata AVUs from a File/Folder"
      :description "
Copies all Metadata Template AVUs from the data item with the ID given in the URL to other data
items sent in the request body."
      (ok (avus/copy-metadata-template-avus user force data-id body)))

    (GET* "/:template-id" []
      :path-params [data-id :- TargetIdPathParam
                    template-id :- TemplateIdPathParam]
      :return DataItemMetadataTemplateAvuList
      :summary "View a Metadata Template's AVUs on a File/Folder"
      :description "Lists all AVUs associated with the data item and the given Metadata Template."
      (ok (avus/list-metadata-template-avus data-id template-id)))

    (POST* "/:template-id" []
      :path-params [data-id :- TargetIdPathParam
                    template-id :- TemplateIdPathParam]
      :query [{:keys [user data-type]} StandardDataItemQueryParams]
      :body [body (describe SetMetadataTemplateAvuRequest "The Metadata Template AVU save request")]
      :return DataItemMetadataTemplateAvuList
      :summary "Add/Update Metadata AVUs on a File/Folder"
      :description "
Saves Metadata AVUs on the given data item, associating them with the given Metadata Template.

Including an existing AVU’s ID in its JSON in the POST body will update its values and `modified_on`
timestamp, and also ensure that the AVU is associated with the metadata template. AVUs included
without an ID will be added to the data item if the AVU does not already exist, otherwise the AVU
with matching `attr`, `owner`, and `target` is updated as previously described.

AVUs can only be associated with one metadata template per data item, per user. All AVUs on the
given data item will be disaccociated with all other Metadata Templates."
      (ok (avus/set-metadata-template-avus user data-id data-type template-id body)))

    (DELETE* "/:template-id" []
      :path-params [data-id :- TargetIdPathParam
                    template-id :- TemplateIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Remove all Metadata AVUs on a File/Folder"
      :description "Removes all AVUs associated with the given data item and Metadata Template."
      (ok (avus/remove-metadata-template-avus user data-id template-id)))

    (DELETE* "/:template-id/:avu-id" []
      :path-params [data-id :- TargetIdPathParam
                    template-id :- TemplateIdPathParam
                    avu-id :- AvuIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Remove a Metadata AVU from a File/Folder"
      :description "Removes the AVU associated with the given ID, data item, and Metadata Template."
      (ok (avus/remove-metadata-template-avu user data-id template-id avu-id)))))
