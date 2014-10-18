(ns data-info.services.service-info
  (:require [liberator.core :refer [defresource]]))


(defresource service-info []
  :available-media-types ["application/json"]
  :handle-ok             {:service "data-info" :version "4.0.1"})
