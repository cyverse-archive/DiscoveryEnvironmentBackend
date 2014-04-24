(ns user-sessions.serve-test
  (:use [midje.sweet]
        [kameleon.user-session-queries]
        [kameleon.misc-queries]
        [user-sessions.serve]
        [ring.util.response]))


(fact "santize works"
      (sanitize {:foo "bar"}) => {:foo "bar"}
      (sanitize nil) => {}
      (sanitize {:session "{\"foo\":\"bar\"}"}) => {:session {:foo "bar"}}
      (sanitize {:session "{\"foo\":\"bar\"}" :id "foo" :user_id "bar"}) => {:session {:foo "bar"}})

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
              user-session (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns a sane value with a good user and content type"
          (get-req "foo" {:content-type "application-json"}) => (response {:foo "bar"}))

  (fact "get-req shouldn't care about the content-type"
        (get-req "foo" {:content-type "foo"}) => (response {:foo "bar"})))

(with-redefs [user? (fn [u] false)
              user-session (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns an error map when the user is bad"
        (get-req "foo" {:content-type "application-json"}) => {:status 404 :body {:user "foo"}}))


;;; Testing the (post-req) function
(with-redefs [user?             (fn [u] true)
              save-user-session (fn [u b] {:user_id 50
                                           :session "{\"foo\":\"bar\"}"
                                           :id "boo"})]
  (fact "post-req returns a sane value with a good user and content type"
        (post-req "foo" {:content-type "application/json"
                         :body "{\"foo\":\"bar\"}"}) => (response {:session {:foo "bar"}}))

  (fact "post-req should care about the content type"
        (post-req "foo" {:content-type "foo"
                         :body "{\"foo\":\"bar\"}"}) => (invalid-content "foo")))

(with-redefs [user? (fn [u] false)
              save-user-session (fn [u b] {:user_id 50
                                           :session "{\"foo\":\"bar\"}"
                                           :id "boo"})]
  (fact "post-req should care about the user existing"
        (post-req "foo" {:content-type "foo"
                         :body "{\"foo\":\"bar\"}"}) => (not-a-user "foo")))


;;; testing the (delete-req) function
(with-redefs [user? (fn [u] true)
              delete-user-session (fn [u] {:user_id 50
                                           :session "{\"foo\":\"bar\"}"
                                           :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "foo" {:content-type "application/json"
                           :body "{\"foo\":\"bar\"}"}) => (response {:session {:foo "bar"}})
        (delete-req "foo" {:content-type "foo"
                           :body "{\"foo\":\"bar\"}"}) => (response {:session {:foo "bar"}})))

(with-redefs [user? (fn [u] false)
              delete-user-session (fn [u] {:user_id 50
                                           :session "{\"foo\":\"bar\"}"
                                           :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "foo" {:content-type "application/json"
                           :body "{\"foo\":\"bar\"}"}) => (not-a-user "foo")))


