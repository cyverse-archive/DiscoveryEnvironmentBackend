(ns clojure-commons.jwt-test
  (:use [clojure.test])
  (:require [clojure-commons.jwt :as jwt]))

(def user {:user        "ipctest"
           :email       "ipctest@iplantcollaborative.org"
           :given-name  "Ipc"
           :family-name "Test"
           :common-name "Ipc Test"})

(def opts {:after                300
           :public-key-path      "test-resources/public-key.pem"
           :private-key-path     "test-resources/private-key.pem"
           :private-key-password "testkey"
           :alg                  :rs256})

(def generator (jwt/generator opts))

(def validator (jwt/validator opts))

(defn- user-from-jwt
  [jwt]
  {:user        (:sub jwt)
   :email       (:email jwt)
   :given-name  (:given_name jwt)
   :family-name (:family_name jwt)
   :common-name (:name jwt)})

(deftest jwt-test
  (is (= user (user-from-jwt (validator (generator user))))
      "Can validate a generated assertion."))
