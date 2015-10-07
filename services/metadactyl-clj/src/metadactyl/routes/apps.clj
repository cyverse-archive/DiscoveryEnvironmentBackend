(ns metadactyl.routes.apps
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.rating]
        [metadactyl.routes.domain.tool :only [NewToolListing]]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.coercions :only [coerce!]]
        [ring.util.http-response :only [ok]])
  (:require [metadactyl.service.apps :as apps]))

(defroutes* apps
  (GET* "/" []
        :query [params AppSearchParams]
        :summary "Search Apps"
        :return AppListing
        :description "This service allows users to search for Apps based on a part of the App name or
        description. The response body contains an `apps` array that is in the same format as
        the `apps` array in the /apps/categories/:category-id endpoint response."
        (ok (coerce! AppListing
                 (apps/search-apps current-user params))))

  (POST* "/" []
         :query [params SecuredQueryParamsRequired]
         :body [body (describe AppRequest "The App to add.")]
         :return App
         :summary "Add a new App."
         :description "This service adds a new App to the user's workspace."
         (ok (apps/add-app current-user body)))

  (POST* "/arg-preview" []
         :query [params SecuredQueryParams]
         :body [body (describe AppPreviewRequest "The App to preview.")]
         :summary "Preview Command Line Arguments"
         :description "The app integration utility in the DE uses this service to obtain an example list
         of command-line arguments so that the user can tell what the command-line might look like
         without having to run a job using the app that is being integrated first. The App request
         body also requires that each parameter contain a `value` field that contains the parameter
         value to include on the command line. The response body is in the same format as the
         `/arg-preview` service in the JEX. Please see the JEX documentation for more information."
         (ok (apps/preview-command-line current-user body)))

  (GET* "/ids" []
        :query [params SecuredQueryParams]
        :return AppIdList
        :summary "List All App Identifiers"
        :description "The export script needs to have a way to obtain the identifiers of all of the apps
        in the Discovery Environment, deleted or not. This service provides that information."
        (ok (apps/list-app-ids current-user)))

  (POST* "/shredder" []
         :query [params SecuredQueryParams]
         :body [body (describe AppDeletionRequest "List of App IDs to delete.")]
         :summary "Logically Deleting Apps"
         :description "One or more Apps can be marked as deleted in the DE without being completely
         removed from the database using this service. <b>Note</b>: an attempt to delete an app that
         is already marked as deleted is treated as a no-op rather than an error condition. If the
         App doesn't exist in the database at all, however, then that is treated as an error
         condition."
         (ok (apps/delete-apps current-user body)))

  (GET* "/:app-id" []
        :path-params [app-id :- AppIdJobViewPathParam]
        :query [params SecuredQueryParams]
        :summary "Obtain an app description."
        :return AppJobView
        :description "This service allows the Discovery Environment user interface to obtain an
        app description that can be used to construct a job submission form."
        (ok (coerce! AppJobView
                 (apps/get-app-job-view current-user app-id))))

  (DELETE* "/:app-id" []
           :path-params [app-id :- AppIdPathParam]
           :query [params SecuredQueryParams]
           :summary "Logically Deleting an App"
           :description "An app can be marked as deleted in the DE without being completely removed from
           the database using this service. <b>Note</b>: an attempt to delete an App that is already
           marked as deleted is treated as a no-op rather than an error condition. If the App
           doesn't exist in the database at all, however, then that is treated as an error
           condition."
           (ok (apps/delete-app current-user app-id)))

  (PATCH* "/:app-id" []
          :path-params [app-id :- AppIdPathParam]
          :query [params SecuredQueryParamsEmailRequired]
          :body [body (describe App "The App to update.")]
          :return App
          :summary "Update App Labels"
          :description "This service is capable of updating just the labels within a single-step app, and
          it allows apps that have already been made available for public use to be updated, which
          helps to eliminate administrative thrash for app updates that only correct typographical
          errors. Upon error, the response body contains an error code along with some additional
          information about the error. <b>Note</b>: Although this endpoint accepts all App fields,
          only the 'name' (except in parameters and parameter arguments), 'description', 'label',
          and 'display' (only in parameter arguments) fields will be processed and updated by this
          endpoint."
          (ok (apps/relabel-app current-user (assoc body :id app-id))))

  (PUT* "/:app-id" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParamsEmailRequired]
        :body [body (describe AppRequest "The App to update.")]
        :return App
        :summary "Update an App"
        :description "This service updates a single-step App in the database, as long as the App has not
        been submitted for public use."
        (ok (apps/update-app current-user (assoc body :id app-id))))

  (POST* "/:app-id/copy" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParamsRequired]
         :return App
         :summary "Make a Copy of an App Available for Editing"
         :description "This service can be used to make a copy of an App in the user's workspace."
         (ok (apps/copy-app current-user app-id)))

  (GET* "/:app-id/details" []
        :path-params [app-id :- AppIdJobViewPathParam]
        :query [params SecuredQueryParams]
        :return AppDetails
        :summary "Get App Details"
        :description "This service is used by the DE to obtain high-level details about a single App"
        (ok (coerce! AppDetails
                 (apps/get-app-details current-user app-id))))

  (GET* "/:app-id/documentation" []
        :path-params [app-id :- AppIdJobViewPathParam]
        :query [params SecuredQueryParams]
        :return AppDocumentation
        :summary "Get App Documentation"
        :description "This service is used by the DE to obtain documentation for a single App"
        (ok (coerce! AppDocumentation
                 (apps/get-app-docs current-user app-id))))

  (PATCH* "/:app-id/documentation" []
          :path-params [app-id :- AppIdPathParam]
          :query [params SecuredQueryParamsEmailRequired]
          :body [body (describe AppDocumentationRequest "The App Documentation Request.")]
          :return AppDocumentation
          :summary "Update App Documentation"
          :description "This service is used by the DE to update documentation for a single App"
          (ok (coerce! AppDocumentation
                   (apps/owner-edit-app-docs current-user app-id body))))

  (POST* "/:app-id/documentation" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParamsEmailRequired]
         :body [body (describe AppDocumentationRequest "The App Documentation Request.")]
         :return AppDocumentation
         :summary "Add App Documentation"
         :description "This service is used by the DE to add documentation for a single App"
         (ok (coerce! AppDocumentation
                  (apps/owner-add-app-docs current-user app-id body))))

  (DELETE* "/:app-id/favorite" []
           :path-params [app-id :- AppIdPathParam]
           :query [params SecuredQueryParams]
           :summary "Removing an App as a Favorite"
           :description "Apps can be marked as favorites in the DE, which allows users to access them
           without having to search. This service is used to remove an App from a user's favorites
           list."
           (ok (apps/remove-app-favorite current-user app-id)))

  (PUT* "/:app-id/favorite" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :summary "Marking an App as a Favorite"
        :description "Apps can be marked as favorites in the DE, which allows users to access them without
        having to search. This service is used to add an App to a user's favorites list."
        (ok (apps/add-app-favorite current-user app-id)))

  (GET* "/:app-id/is-publishable" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :summary "Determine if an App Can be Made Public"
        :description "A multi-step App can't be made public if any of the Tasks that are included in it
        are not public. This endpoint returns a true flag if the App is a single-step App or it's a
        multistep App in which all of the Tasks included in the pipeline are public."
        (ok (apps/app-publishable? current-user app-id)))

  (POST* "/:app-id/publish" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParamsEmailRequired]
         :body [body (describe PublishAppRequest "The user's Publish App Request.")]
         :summary "Submit an App for Public Use"
         :description "This service can be used to submit a private App for public use. The user supplies
         basic information about the App and a suggested location for it. The service records the
         information and suggested location then places the App in the Beta category. A Tito
         administrator can subsequently move the App to the suggested location at a later time if it
         proves to be useful."
         (ok (apps/make-app-public current-user (assoc body :id app-id))))

  (DELETE* "/:app-id/rating" []
           :path-params [app-id :- AppIdPathParam]
           :query [params SecuredQueryParams]
           :return RatingResponse
           :summary "Delete an App Rating"
           :description "The DE uses this service to remove a rating that a user has previously made. This
           service deletes the authenticated user's rating for the corresponding app-id."
           (ok (apps/delete-app-rating current-user app-id)))

  (POST* "/:app-id/rating" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :body [body (describe RatingRequest "The user's new rating for this App.")]
         :return RatingResponse
         :summary "Rate an App"
         :description "Users have the ability to rate an App for its usefulness, and this service provides
         the means to store the App rating. This service accepts a rating level between one and
         five, inclusive, and a comment identifier that refers to a comment in iPlant's Confluence
         wiki. The rating is stored in the database and associated with the authenticated user."
         (ok (apps/rate-app current-user app-id body)))

  (GET* "/:app-id/tasks" []
        :path-params [app-id :- AppIdJobViewPathParam]
        :query [params SecuredQueryParams]
        :return AppTaskListing
        :summary "List Tasks with File Parameters in an App"
        :description "When a pipeline is being created, the UI needs to know what types of files are
        consumed by and what types of files are produced by each App's task in the pipeline. This
        service provides that information."
        (ok (coerce! AppTaskListing
                 (apps/get-app-task-listing current-user app-id))))

  (GET* "/:app-id/tools" []
        :path-params [app-id :- AppIdJobViewPathParam]
        :query [params SecuredQueryParams]
        :return NewToolListing
        :summary "List Tools used by an App"
        :description "This service lists information for all of the tools that are associated with an App.
        This information used to be included in the results of the App listing service."
        (ok (coerce! NewToolListing
                 (apps/get-app-tool-listing current-user app-id))))

  (GET* "/:app-id/ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParamsEmailRequired]
        :return App
        :summary "Make an App Available for Editing"
        :description "The app integration utility in the DE uses this service to obtain the App
        description JSON so that it can be edited. The App must have been integrated by the
        requesting user."
        (ok (apps/get-app-ui current-user app-id))))
