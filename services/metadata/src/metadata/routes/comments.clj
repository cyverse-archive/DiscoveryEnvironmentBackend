(ns metadata.routes.comments
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.common]
        [metadata.routes.domain.comments]
        [ring.util.http-response :only [ok]])
  (:require [metadata.services.comments :as comments]))

(defroutes* data-comment-routes
  (context* "/filesystem/data" []
    :tags ["data-comments"]

    (GET* "/:data-id/comments" []
      :path-params [data-id :- TargetIdPathParam]
      :return CommentList
      :summary "Listing Data Comments"
      :description "This endpoint retrieves all of the comments made on a file or folder."
      (ok (comments/list-data-comments data-id)))

    (POST* "/:data-id/comments" []
      :path-params [data-id :- TargetIdPathParam]
      :query [{:keys [user data-type]} StandardDataItemQueryParams]
      :body [body (describe CommentRequest "The comment to add.")]
      :return CommentResponse
      :summary "Create a Comment"
      :description "This endpoint allows a user to post a comment on a file or folder."
      (ok (comments/add-data-comment user data-id data-type body)))

    (PATCH* "/:data-id/comments/:comment-id" []
      :path-params [data-id :- TargetIdPathParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows a user to retract a comment on a file or folder, if the user is the commenter.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/update-data-retract-status user data-id comment-id retracted)))))

(defroutes* admin-data-comment-routes
  (context* "/admin/filesystem/data" []
    :tags ["admin-data-comments"]

    (DELETE* "/:data-id/comments/:comment-id" []
      :path-params [data-id :- TargetIdPathParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Delete a Comment"
      :description
      "This endpoint allows an administrative user to delete a comment on a file or folder."
      (ok (comments/delete-data-comment data-id comment-id)))

    (PATCH* "/:data-id/comments/:comment-id" []
      :path-params [data-id :- TargetIdPathParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows an administrative user to retract any comment on a file or folder.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/admin-update-retract-status user data-id comment-id retracted)))))

(defroutes* app-comment-routes
  (context* "/apps" []
    :tags ["app-comments"]

    (GET* "/:app-id/comments" []
      :path-params [app-id :- TargetIdPathParam]
      :return CommentList
      :summary "Listing App Comments"
      :description "This endpoint retrieves all of the comments made on an app."
      (ok (comments/list-app-comments app-id)))

    (POST* "/:app-id/comments" []
      :path-params [app-id :- TargetIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body (describe CommentRequest "The comment to add.")]
      :return CommentResponse
      :summary "Create a Comment"
      :description "This endpoint allows a user to post a comment on an app."
      (ok (comments/add-app-comment user app-id body)))

    (PATCH* "/:app-id/comments/:comment-id" []
      :path-params [app-id :- TargetIdPathParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows a user to retract a comment on an app, if the user is the commenter.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/update-app-retract-status user app-id comment-id retracted)))))

(defroutes* admin-app-comment-routes
  (context* "/admin/apps" []
    :tags ["admin-app-comments"]

    (DELETE* "/:app-id/comments/:comment-id" []
      :path-params [app-id :- TargetIdPathParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Delete an App Comment"
      :description "This endpoint allows an administrative user to delete a comment on an app."
      (ok (comments/delete-app-comment app-id comment-id)))

    (PATCH* "/:app-id/comments/:comment-id" []
      :path-params [app-id :- TargetIdPathParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows an administrative user to retract any comment on an app.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/admin-update-retract-status user app-id comment-id retracted)))))
