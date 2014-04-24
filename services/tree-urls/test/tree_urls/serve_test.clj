(ns tree-urls.serve-test
  (:use [midje.sweet]
        [kameleon.tree-urls-queries]
        [kameleon.misc-queries]
        [tree-urls.serve]
        [ring.util.response]))


(fact "santize works"
      (sanitize {:foo "bar"}) => {:foo "bar"}
      (sanitize nil) => {}
      (sanitize {:tree_urls "{\"foo\":\"bar\"}"}) => {:tree_urls {:foo "bar"}}
      (sanitize {:tree_urls "{\"foo\":\"bar\"}" :id "foo"}) => {:tree_urls {:foo "bar"}})

(fact "not-a-uuid returns a sane map"
      (not-a-uuid "foo") => {:status 404 :body {:uuid "foo"}})

(fact "invalid content returns a sane map"
      (invalid-content "foo") {:status 415 :body {:content-type "foo"}})


(with-redefs [tree-urls? (fn [u] false)]
  (fact "validate macro is sane-ish with a bad uuid."
      (validate ["foo" {:content-type "poopy"} true]) =>
        {:status 404 :body {:uuid "foo"}}

      (validate ["foo" {:content-type "poopy"} false]) =>
        {:status 404 :body {:uuid "foo"}}))

(with-redefs [tree-urls? (fn [u] true)]
  (fact "validate macro is sane-ish with a bad content-type."
      (validate ["foo" {:content-type "poopy"} true]) =>
        {:status 415 :body {:content-type "poopy"}}

      (validate ["foo" {:content-type "poopy"} false] true) => true))


;;; Testing the (get-req) function
(with-redefs [tree-urls? (fn [u] true)
              tree-urls (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns a sane value with a good user and content type"
          (get-req "foo" {:content-type "application-json"}) => (response {:foo "bar"}))

  (fact "get-req shouldn't care about the content-type"
        (get-req "foo" {:content-type "foo"}) => (response {:foo "bar"})))

(with-redefs [tree-urls? (fn [u] false)
              tree-urls (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns an error map when the user is bad"
        (get-req "foo" {:content-type "application-json"}) => {:status 404 :body {:uuid "foo"}}))


;;; Testing the (post-req) function
(with-redefs [tree-urls?          (fn [u] true)
              save-tree-urls (fn [u b] {:tree_urls "{\"foo\":\"bar\"}"
                                        :id "boo"})]
  (fact "post-req returns a sane value with a good user and content type"
        (post-req "foo" {:content-type "application/json"
                         :body "{\"foo\":\"bar\"}"}) => (response {:tree_urls {:foo "bar"}}))

  (fact "post-req should care about the content type"
        (post-req "foo" {:content-type "foo"
                         :body "{\"foo\":\"bar\"}"}) => (invalid-content "foo")))

(with-redefs [tree-urls? (fn [u] false)
              save-tree-urls (fn [u b] {:tree_urls "{\"foo\":\"bar\"}"
                                        :id "boo"})]
  (fact "post-req should care about the user existing"
        (post-req "foo" {:content-type "foo"
                         :body "{\"foo\":\"bar\"}"}) => (not-a-uuid "foo")))


;;; testing the (delete-req) function
(with-redefs [tree-urls? (fn [u] true)
              delete-tree-urls (fn [u] {:tree_urls "{\"foo\":\"bar\"}"
                                        :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "foo" {:content-type "application/json"
                           :body "{\"foo\":\"bar\"}"}) => (response {:tree_urls {:foo "bar"}})
        (delete-req "foo" {:content-type "foo"
                           :body "{\"foo\":\"bar\"}"}) => (response {:tree_urls {:foo "bar"}})))

(with-redefs [tree-urls? (fn [u] false)
              delete-tree-urls (fn [u] {:tree_urls "{\"foo\":\"bar\"}"
                                        :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "foo" {:content-type "application/json"
                           :body "{\"foo\":\"bar\"}"}) => (not-a-uuid "foo")))


