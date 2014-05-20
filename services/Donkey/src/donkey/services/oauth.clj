(ns donkey.services.oauth
  "Service implementations dealing with OAuth 2.0 authentication."
  (:use [donkey.auth.user-attributes :only [current-user]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [donkey.persistence.oauth :as op]
            [donkey.util.service :as service]))

(defn get-access-token
  "Receives an OAuth authorization code and obtains an access token."
  [{:keys [api-name api-key api-secret oauth-base]} {code :code redirect-uri :redirect_uri}]
  (-> (http/post (str (curl/url oauth-base "token"))
                 {:basic-auth  [api-key api-secret]
                  :form-params {:grant_type   "authorization_code"
                                :code         code
                                :redirect_uri redirect-uri}
                  :as          :stream})
      (:body)
      (service/decode-json)
      (op/store-access-token api-name (:username current-user))))
