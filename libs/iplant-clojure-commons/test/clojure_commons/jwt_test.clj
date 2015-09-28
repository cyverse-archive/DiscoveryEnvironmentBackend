(ns clojure-commons.jwt-test
  (:use [clojure.test])
  (:require [clojure-commons.jwt :as jwt]))

(def user {:user        "ipctest"
           :email       "ipctest@iplantcollaborative.org"
           :given-name  "Ipc"
           :family-name "Test"
           :common-name "Ipc Test"})

(def opts {:validity-window-end  300
           :public-key-path      "test-resources/public-key.pem"
           :private-key-path     "test-resources/private-key.pem"
           :private-key-password "testkey"
           :alg                  :rs256})

(def generator (jwt/generator opts))

(def validator (jwt/validator opts))

(deftest jwt-test
  (is (= user (jwt/user-from-assertion (validator (generator user))))
      "Can validate a generated assertion."))
