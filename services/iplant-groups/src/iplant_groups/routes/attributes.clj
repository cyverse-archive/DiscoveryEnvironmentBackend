(ns iplant_groups.routes.attributes
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.params]
        [iplant_groups.routes.domain.attribute]
        [ring.util.http-response :only [ok]])
  (:require [iplant_groups.service.attributes :as attributes]))

(defroutes* attributes
  (POST* "/" []
        :return      AttributeName
        :query       [params StandardUserQueryParams]
        :body        [body (describe BaseAttributeName "The attribute/resource to add.")]
        :summary     "Add Attribute Name/Resource Definition"
        :description "This endpoint allows adding a new attribute name. Grouper also uses attribute names to store resources to which permissions are assigned. An attribute definition must be present, already made by using the Grouper UI (most likely), as there is no web service for creating them."
        (ok (attributes/add-attribute-name body params))))
