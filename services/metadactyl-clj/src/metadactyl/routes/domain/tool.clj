(ns metadactyl.routes.domain.tool
  (:use [common-swagger-api.schema :only [->optional-param describe]]
        [metadactyl.routes.params]
        [schema.core :only [defschema enum optional-key]])
  (:require [metadactyl.schema.containers :as containers])
  (:import [java.util UUID]))

(def ToolRequestIdParam (describe UUID "The Tool Requests's UUID"))
(def ToolNameParam (describe String "The Tool's name (should be the file name)"))
(def ToolDescriptionParam (describe String "A brief description of the Tool"))
(def VersionParam (describe String "The Tool's version"))
(def AttributionParam (describe String "The Tool's author or publisher"))
(def SubmittedByParam (describe String "The username of the user that submitted the Tool Request"))

(defschema ToolTestData
  {(optional-key :params) (describe [String] "The list of command-line parameters")
   :input_files           (describe [String] "The list of paths to test input files in iRODS")
   :output_files          (describe [String] "The list of paths to expected output files in iRODS")})

(defschema ToolImplementation
  {:implementor       (describe String "The name of the implementor")
   :implementor_email (describe String "The email address of the implementor")
   :test              (describe ToolTestData "The test data for the Tool")})

(defschema Tool
  {:id                         ToolIdParam
   :name                       ToolNameParam
   (optional-key :description) ToolDescriptionParam
   (optional-key :attribution) AttributionParam
   :location                   (describe String "The path of the directory containing the Tool")
   (optional-key :version)     VersionParam
   :type                       (describe String "The Tool Type name")})

(defschema ToolImportRequest
  (-> Tool
      (->optional-param :id)
      (merge
        {:implementation (describe ToolImplementation
                           "Information about the user who integrated the Tool into the DE")
         :container      containers/NewToolContainer})))

(defschema ToolsImportRequest
  {:tools (describe [ToolImportRequest] "zero or more Tool definitions")})

(defschema ToolUpdateRequest
  (-> ToolImportRequest
      (->optional-param :name)
      (->optional-param :location)
      (->optional-param :type)
      (->optional-param :implementation)
      (dissoc :container)))

(defschema ToolListing
  {:tools (describe [Tool] "Listing of App Tools")})

(defschema NewTool
  (assoc Tool
    :id (describe String "The tool's ID")))

(defschema NewToolListing
  {:tools (describe [NewTool] "Listing of App Tools")})

(defschema ToolRequestStatus
  {(optional-key :status)
   (describe String
     "The status code of the Tool Request update. The status code is case-sensitive, and if it isn't
      defined in the database already then it will be added to the list of known status codes")

   :status_date
   (describe Long "The timestamp of the Tool Request status update")

   :updated_by
   (describe String "The username of the user that updated the Tool Request status")

   (optional-key :comments)
   (describe String "The administrator comments of the Tool Request status update")})

(defschema ToolRequestStatusUpdate
  (dissoc ToolRequestStatus :updated_by :status_date))

(defschema ToolRequestDetails
  {:id
   ToolRequestIdParam

   :submitted_by
   SubmittedByParam

   (optional-key :phone)
   (describe String "The phone number of the user submitting the request")

   :name
   ToolNameParam

   :description
   ToolDescriptionParam

   (optional-key :source_url)
   (describe String "A link that can be used to obtain the tool")

   (optional-key :source_upload_file)
   (describe String "The path to a file that has been uploaded into iRODS")

   :documentation_url
   (describe String "A link to the tool documentation")

   :version
   VersionParam

   (optional-key :attribution)
   AttributionParam

   (optional-key :multithreaded)
   (describe Boolean
     "A flag indicating whether or not the tool is multithreaded. This can be `true` to indicate
      that the user requesting the tool knows that it is multithreaded, `false` to indicate that the
      user knows that the tool is not multithreaded, or omitted if the user does not know whether or
      not the tool is multithreaded")

   :test_data_path
   (describe String "The path to a test data file that has been uploaded to iRODS")

   :cmd_line
   (describe String "Instructions for using the tool")

   (optional-key :additional_info)
   (describe String
     "Any additional information that may be helpful during tool installation or validation")

   (optional-key :additional_data_file)
   (describe String
     "Any additional data file that may be helpful during tool installation or validation")

   :architecture
   (describe (enum "32-bit Generic" "64-bit Generic" "Others" "Don't know")
     "One of the architecture names known to the DE. Currently, the valid values are
      `32-bit Generic` for a 32-bit executable that will run in the DE,
      `64-bit Generic` for a 64-bit executable that will run in the DE,
      `Others` for tools run in a virtual machine or interpreter, and
      `Don't know` if the user requesting the tool doesn't know what the architecture is")

   :history
   (describe [ToolRequestStatus] "A history of status updates for this Tool Request")})

(defschema ToolRequest
  (dissoc ToolRequestDetails :id :submitted_by :history))

(defschema ToolRequestSummary
  {:id             ToolRequestIdParam
   :name           ToolNameParam
   :version        VersionParam
   :requested_by   SubmittedByParam
   :date_submitted (describe Long "The timestamp of the Tool Request submission")
   :status         (describe String "The current status of the Tool Request")
   :date_updated   (describe Long "The timestamp of the last Tool Request status update")
   :updated_by     (describe String
                     "The username of the user that last updated the Tool Request status")})

(defschema ToolRequestListing
  {:tool_requests (describe [ToolRequestSummary]
                    "A listing of high level details about tool requests that have been submitted")})

(defschema ToolRequestListingParams
  (merge SecuredPagingParams
    {(optional-key :status)
     (describe String
       "The name of a status code to include in the results. The name of the status code is case
        sensitive. If the status code isn't already defined, it will be added to the database")}))

(defschema StatusCodeListingParams
  (merge SecuredQueryParams
    {(optional-key :filter)
     (describe String
       "If this parameter is set then only the status codes that contain the string passed in this
        query parameter will be listed. This is a case-insensitive search")}))

(defschema StatusCode
  {:id          (describe UUID "The Status Code's UUID")
   :name        (describe String "The Status Code")
   :description (describe String "A brief description of the Status Code")})

(defschema StatusCodeListing
  {:status_codes (describe [StatusCode] "A listing of known Status Codes")})
