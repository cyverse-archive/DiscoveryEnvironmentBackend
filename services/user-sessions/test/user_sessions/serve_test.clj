(ns user-sessions.serve-test
  (:use [midje.sweet]
        [user-sessions.serve]))


(fact "santize works"
      (sanitize {:foo "bar"}) => {:foo "bar"})
