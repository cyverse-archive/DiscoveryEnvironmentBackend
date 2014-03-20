(ns mescal.agave-v1
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [mescal.util :as util]))

(defn get-auth-token
  "Obtains an authentication token."
  [base key secret {:keys [username grant-type scope]
                    :or   {grant-type "client_credentials"
                           scope      "PRODUCTION"}}]
  (util/assert-defined key secret username)
  (-> (client/post (curl/url base "v2" "token")
                   {:basic-auth  [key secret]
                    :as          :stream
                    :form-params {:grantType grant-type
                                  :username  username
                                  :password  username
                                  :scope     scope}})
      (:body)
      (util/decode-json)))
