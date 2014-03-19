(ns porklock.shell-interop
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure.pprint :only [pprint]])
  (:require [clojure.java.shell :as sh]
            [clojure.string :as string]))

(defn process-exit
  "Examines the exit map that gets passed in and decides whether to
   throw an exception or not."
  [{:keys [exit out err]} skip-err]
  
  (when-not (string/blank? out) 
    (println "stdout: ")
    (println out))
  
  (when-not (or skip-err (string/blank? err)) 
    (println "stderr: ")
    (println err))
  
  (if (not= exit 0)
    (throw+ {:error_code "ERR_BAD_EXIT_CODE"
             :exit-code exit})))

(defn print-command
  "Prints out the command-line invocation based on the arguments
   passed in. The first arg should be the executable. Returns
   the arguments untouched."
  ([args]
    (print-command args " "))
  ([args delimiter]
    (let [[str-args opts] (split-with string? args)]
      (dotimes [n 80] (print "="))
      (println " ")
      (doseq [opt-part (partition 2 opts)]
        ;;; don't print stdin.
        (let [field     (name (first opt-part))
              field-val (last opt-part)]
          (when-not (= field "in")
            (println (str field ": "))
            (pprint field-val)
            (println " "))))
      (println "command:")
      (println (string/join delimiter str-args))
      (println " "))
    args))

(defn exec
  [args]
  (apply sh/sh args))

(defn shell-out
  "Prints out the command, executes the command, and then
   processes the exit map."
  [args & {:keys [skip-err] :or {skip-err false}}]
  (-> args
    print-command
    exec
    (process-exit skip-err)))

