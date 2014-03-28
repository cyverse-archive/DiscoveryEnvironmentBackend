(ns panopticon.test.core
  (:use [panopticon.core] :reload)
  (:use [midje.sweet]))

(fact
 (boolize "true") => true)


