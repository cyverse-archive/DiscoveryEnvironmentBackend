(ns dingle.services
  (:use [pallet.stevedore]
        [dingle.scripting])
  (:require [clojure.string :as string]))

(defn service
  [name cmd sudo-pass]
  (sudo-cmd (str "/sbin/service " name " " cmd) sudo-pass))

(defn service-restart
  [name sudo-pass]
  (service name "restart" sudo-pass))

(defn service-stop
  [name sudo-pass]
  (service name "stop" sudo-pass))

(defn service-start
  [name sudo-pass]
  (service name "start" sudo-pass))

(defn yum-clean 
  [sudo-pass] 
  (sudo-cmd (scriptify (yum clean metadata)) sudo-pass))

(defn yum-install 
  [sudo-pass & pkgs] 
  (sudo-cmd (scriptify (yum install ~@pkgs)) sudo-pass))

(defn yum-update 
  [sudo-pass & pkgs] 
  (sudo-cmd (scriptify (yum update -y ~@pkgs)) sudo-pass))

(defn yum-erase 
  [sudo-pass & pkgs] 
  (sudo-cmd (scriptify (yum erase -y ~@pkgs)) sudo-pass))

(defn yum-info 
  [sudo-pass & pkgs] 
  (sudo-cmd (scriptify (yum info ~@pkgs)) sudo-pass))
