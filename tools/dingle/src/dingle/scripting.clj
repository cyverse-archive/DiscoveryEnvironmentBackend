(ns dingle.scripting
  (:use [pallet.stevedore]
        [clj-ssh.ssh])
  (:require [pallet.common.shell :as sh]
            [clojure.java.shell :as clj-sh]
            [pallet.stevedore.bash :as bash]))

(defn tmpfile
  []
  (doto (java.io.File/createTempFile "iplant" ".sh") .deleteOnExit))

(defn clj-execute
  [& scripts]
  (for [scr scripts]
    (let [tmpf (tmpfile)]
      (spit tmpf scr)
      (clj-sh/sh "bash" (.getAbsolutePath tmpf)))))

(defn execute
  [& scripts]
  (for [scr scripts] 
     (sh/bash scr)))

(defmacro scriptify
  [& body]
  `(with-script-language :pallet.stevedore.bash/bash
    (script ~@body)))

(defmacro combine
  [& scripts]
  `(with-script-language :pallet.stevedore.bash/bash
     (do-script ~@scripts)))

(defmacro chain
  [& scripts]
  `(with-script-language :pallet.stevedore.bash/bash
     (chained-script ~@scripts)))

(defmacro checked
  [& scripts]
  `(with-script-language :pallet.stevedore.bash/bash
     (checked-script ~@scripts)))

(defn sudo-cmd
  [cmd pass]
  (str "echo " pass " | sudo -S " cmd))

(defn ssh-command
  [host port user cmd]
  (let [quoted-cmd (str "'" cmd "'")
        conn-str (str user "@" host)]
    (scriptify
      (ssh "-t" "-t" "-p" ~port ~conn-str ~quoted-cmd))))

(defn remote-execute
  [host port user cmd]
  (execute (ssh-command host port user cmd)))
