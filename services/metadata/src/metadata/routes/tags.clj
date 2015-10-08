(ns metadata.routes.tags
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.common]
        [metadata.routes.domain.tags]
        [ring.util.http-response :only [ok]])
  (:require [metadata.services.tags :as tags]))

(defroutes* filesystem-tags
  (context* "/filesystem/data" []
    :tags ["tags"]

    (GET* "/:data-id/tags" []
      :path-params [data-id :- TargetIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :return TagList
      :summary "List Attached Tags"
      :description
      "This endpoint lists the tags of the user that are attached to the indicated file or folder."
      (ok (tags/list-attached-tags user data-id)))

    (PATCH* "/:data-id/tags" []
      :path-params [data-id :- TargetIdPathParam]
      :query [{:keys [user data-type type]} UpdateAttachedTagsQueryParams]
      :body [body (describe TagIdList "The UUIDs of the tags to attach/detach.")]
      :return UpdateAttachedTagsResponse
      :summary "Attach/Detach Tags to a File/Folder"
      :description "
Depending on the `type` parameter, this endpoint either attaches a set of the authenticated user's
tags to the indicated file or folder, or it detaches the set."
      (ok (tags/handle-patch-file-tags user data-id data-type type body)))))

(defroutes* tags
  (context* "/tags" []
    :tags ["tags"]

    (GET* "/suggestions" []
      :query [{:keys [user contains limit]} TagSuggestQueryParams]
      :return TagList
      :summary "Suggest a Tag"
      :description "
Given a textual fragment of a tag's value, this endpoint will list up to a given number of the
authenticated user's tags that contain the fragment."
      (ok (tags/suggest-tags user contains limit)))

    (POST* "/user" []
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body (describe TagRequest "The user tag to create.")]
      :return TagDetails
      :summary "Create a Tag"
      :description "This endpoint creates a tag for use by the authenticated user."
      (ok (tags/create-user-tag user body)))

    (DELETE* "/user/:tag-id" []
      :path-params [tag-id :- TagIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Delete a Tag"
      :description "This endpoint allows a user tag to be deleted, detaching it from all metadata."
      (ok (tags/delete-user-tag user tag-id)))

    (PATCH* "/user/:tag-id" []
      :path-params [tag-id :- TagIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body (describe TagUpdateRequest "The tag fields to update.")]
      :return TagDetails
      :summary "Update Tag Labels/Descriptions"
      :description
      "This endpoint allows a tag's label and description to be modified by the owning user."
      (ok (tags/update-user-tag user tag-id body)))))
