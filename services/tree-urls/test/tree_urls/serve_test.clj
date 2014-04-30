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

(fact "not-sha1 returns a sane map"
      (not-sha1 "foo") => {:status 400 :body "Invalid SHA1 format: foo"})

(fact "invalid content returns a sane map"
      (invalid-content "foo") {:status 415 :body "Invalid content type: foo"})


(with-redefs [tree-urls? (fn [u] false)]
  (fact "validate macro is sane-ish with a bad SHA1."
      (validate ["foo" {:content-type "poopy"} true]) =>
        {:status 400 :body "Invalid SHA1 format: foo"}

      (validate ["foo" {:content-type "poopy"} false]) =>
        {:status 400 :body "Invalid SHA1 format: foo"}))

(with-redefs [tree-urls? (fn [u] true)]
  (fact "validate macro is sane-ish with a bad content-type."
      (validate ["9e484232659a9f3502bb8340f21fde1dadbbe62d" {:content-type "poopy"} true]) =>
        {:status 415 :body "Invalid content type: poopy"}

      (validate ["9e484232659a9f3502bb8340f21fde1dadbbe62d" {:content-type "poopy"} false] true) => true))


;;; Testing the (get-req) function
(with-redefs [tree-urls? (fn [u] true)
              tree-urls (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns a sane value with a good SHA1 and content type"
        (get-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                 {:content-type "application-json"}) =>
                 (response {:foo "bar"}))

  (fact "get-req shouldn't care about the content-type"
        (get-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                 {:content-type "foo"}) =>
                 (response {:foo "bar"})))

(with-redefs [tree-urls? (fn [u] false)
              tree-urls (fn [u] "{\"foo\":\"bar\"}")]
  (fact "get-req returns an error map when the sha1 is bad"
        (get-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                 {:content-type "application-json"}) =>
        {:status 404 :body "Not Found: 9e484232659a9f3502bb8340f21fde1dadbbe62d"}))

;;; Testing the (post-req) function
(with-redefs [tree-urls?     (fn [u] true)
              save-tree-urls (fn [u b] {:tree_urls "{\"foo\":\"bar\"}"
                                       :sha1 "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                                       :id "boo"})]
  (fact "post-req returns a sane value with a good user and content type"
        (post-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                  {:content-type "application/json"
                   :body "{\"foo\":\"bar\"}"}) =>
                   (response {:tree_urls {:foo "bar"}}))

  (fact "post-req should care about the content type"
        (post-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                  {:content-type "foo"
                   :body "{\"foo\":\"bar\"}"}) =>
                   (invalid-content "foo")))

;;; testing the (delete-req) function
(with-redefs [tree-urls?      (fn [u] true)
              delete-tree-urls (fn [u] {:tree_urls "{\"foo\":\"bar\"}"
                                        :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                    {:content-type "application/json"
                     :body "{\"foo\":\"bar\"}"}) =>
                     (response {:tree_urls {:foo "bar"}})
        (delete-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                    {:content-type "foo"
                     :body "{\"foo\":\"bar\"}"}) =>
                     (response {:tree_urls {:foo "bar"}})))

(with-redefs [tree-urls?       (fn [u] false)
              delete-tree-urls (fn [u] {:tree_urls "{\"foo\":\"bar\"}"
                                        :id "boo"})]
  (fact "delete-req returns a sane value"
        (delete-req "9e484232659a9f3502bb8340f21fde1dadbbe62d"
                    {:content-type "application/json"}) =>
                     nil))


