(ns saved-searches.serve-test
  (:use [midje.sweet]
        [kameleon.user-saved-searches-queries]
        [kameleon.misc-queries]
        [saved-searches.serve]
        [ring.util.response]))


(fact "santize works"
      (sanitize {:foo "bar"}) => {:foo "bar"}
      (sanitize nil) => {}
      (sanitize {:saved_searches "{\"foo\":\"bar\"}"}) => {:saved_searches {:foo "bar"}}
      (sanitize {:saved_searches "{\"foo\":\"bar\"}" :id "foo" :user_id "bar"}) => {:saved_searches {:foo "bar"}})

(fact "not-a-user returns a sane map"
      (not-a-user "foo") => {:status 404 :body {:user "foo"}})

(fact "invalid content returns a sane map"
      (invalid-content "foo") {:status 415 :body {:content-type "foo"}})


(with-redefs [user? (fn [u] false)]
  (fact "validate macro is sane-ish with a bad user."
      (validate ["foo" {:content-type "poopy"} true]) =>
        {:status 404 :body {:user "foo"}}

      (validate ["foo" {:content-type "poopy"} false]) =>
        {:status 404 :body {:user "foo"}}))

(with-redefs [user? (fn [u] true)]
  (fact "validate macro is sane-ish with a bad content-type."
      (validate ["foo" {:content-type "poopy"} true]) =>
        {:status 415 :body {:content-type "poopy"}}

      (validate ["foo" {:content-type "poopy"} false] true) => true))


;;; Testing the (get-req) function
(with-redefs [user? (fn [u] true)
              saved-searches (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns a sane value with a good user and content type"
          (get-req "foo" {:content-type "application-json"}) => (response "{\"foo\":\"bar\"}"))

  (fact "get-req shouldn't care about the content-type"
        (get-req "foo" {:content-type "foo"}) => (response "{\"foo\":\"bar\"}")))

(with-redefs [user? (fn [u] false)
              saved-searches (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns an error map when the user is bad"
        (get-req "foo" {:content-type "application-json"}) => {:status 404 :body {:user "foo"}}))


;;; Testing the (post-req) function
(with-redefs [user?           (fn [u] true)
              save-saved-searches (fn [u b] {:user_id 50
                                         :saved_searches "{\"foo\":\"bar\"}"
                                         :id "boo"})]
  (fact "post-req returns a sane value with a good user and content type"
        (post-req "foo" {:content-type "application/json"
                         :body "{\"foo\":\"bar\"}"}) => (response {:saved_searches {:foo "bar"}}))

  (fact "post-req should care about the content type"
        (post-req "foo" {:content-type "foo"
                         :body "{\"foo\":\"bar\"}"}) => (invalid-content "foo")))

(with-redefs [user? (fn [u] false)
              save-saved-searches (fn [u b] {:user_id 50
                                         :saved_searches "{\"foo\":\"bar\"}"
                                         :id "boo"})]
  (fact "post-req should care about the user existing"
        (post-req "foo" {:content-type "foo"
                         :body "{\"foo\":\"bar\"}"}) => (not-a-user "foo")))


;;; testing the (delete-req) function
(with-redefs [user? (fn [u] true)
              delete-saved-searches (fn [u] {:user_id 50
                                         :saved_searches "{\"foo\":\"bar\"}"
                                         :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "foo" {:content-type "application/json"
                           :body "{\"foo\":\"bar\"}"}) => ""
        (delete-req "foo" {:content-type "foo"
                           :body "{\"foo\":\"bar\"}"}) => ""))

(with-redefs [user? (fn [u] false)
              delete-saved-searches (fn [u] {:user_id 50
                                           :saved_searches "{\"foo\":\"bar\"}"
                                           :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "foo" {:content-type "application/json"
                           :body "{\"foo\":\"bar\"}"}) => ""))


