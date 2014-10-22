;; TODO: actually use this namespace.

(ns metadactyl.analyses.fapi
  (:use [kameleon.uuids :only [uuidify]])
  (:require [clojure.string :as string]
            [metadactyl.util.config :as config]))

(def ^:private generated-param-ids
  {:archive      (uuidify "06F127BB-0599-4343-89CB-DD15BD0163DE")
   :archive-path (uuidify "21039F17-3D4B-4E33-BD10-2904408826F8")
   :command      (uuidify "3046E5B2-F617-49BE-9C26-57D257F2331B")
   :job-name     (uuidify "0D1DB50C-C930-4CDA-8FEC-2E7326B26849")
   :proxy-user   (uuidify "616D8815-C42A-4E53-885B-E7A594D4BDBD")})

(def ^:private irods-home-pattern
  (memoize #(re-pattern (str "\\A\\Q" (string/replace (config/irods-home) #"/+\z") ""))))

(defn- remove-irods-home
  [path]
  (string/replace path (irods-home-pattern) ""))
