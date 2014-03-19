(ns clavin.templates
  (:use [clavin.environments :only [envs-by-dep self-referenced-params]]
        [clojure.java.io :only [file]]
        [clojure.set :only [difference union]])
  (:require [clojure.string :as string])
  (:import [java.io FilenameFilter]
           [org.stringtemplate.v4 ST STErrorListener STGroup]
           [org.stringtemplate.v4.debug EvalExprEvent]))

(def ^:private placeholder-delim \$)

(def ^:private quoted-delim (str "\\Q" placeholder-delim "\\E"))

(def ^:private placeholder-re
  (re-pattern (str quoted-delim "(.*?)" quoted-delim)))

(defn- new-st-group
  "Creates a new ST group using our placeholder delimiter as the beginning and
   ending characters for substitution expressions."
  []
  (STGroup. placeholder-delim placeholder-delim))

(defn load-template
  "Loads a template from disk.  The ST group may optionally be provided; if one
   is not provided then a default ST group will be used."
  ([template-dir template-name]
     (load-template template-dir template-name (new-st-group)))
  ([template-dir template-name template-group]
     (let [template-name (str template-name ".st")
           template-file (file template-dir template-name)]
       (ST. template-group (slurp template-file)))))

(defn list-templates
  "Obtains a list of the names of the templates in the given directory."
  [template-dir]
  (map
   #(string/replace % #"[.]st\z" "")
   (seq (.list (file template-dir)
               (proxy [FilenameFilter] []
                 (accept [dir filename]
                   (not (nil? (re-find #"[.]st\z" filename)))))))))

(defn- add-props
  "Adds properties from an environment to a string template."
  [env st]
  (dorun (map (fn [[k v]] (.add st (string/replace (name k) "-" "_") v)) env)))

(defn gen-file
  "Generates the configuration file text for an environment and template."
  [env template-dir template-name]
  (let [st (load-template template-dir template-name)]
    (add-props env st)
    (.render st)))

(defn- template-validation-error-listener
  "Creates an ST error listener that can be used for template validation.  This
   error listener will automatically include the name of the template in any
   messages that are displayed."
  [template]
  (letfn [(print-err [msg] (println template "is invalid:" (str msg)))]
    (proxy [STErrorListener] []
      (compileTimeError [msg] (print-err msg))
      (runTimeError [msg] (print-err msg))
      (IOError [msg] (print-err msg))
      (internalError [msg] (print-err msg)))))

(defn- template-validating-st-group
  "Creates an ST group with an error listener that will automatically include
   the name of the template in any messages that are displayed."
  [template-name]
  (doto (new-st-group)
    (.setListener (template-validation-error-listener template-name))))

(defn valid-template?
  "Validates a single template to ensure that it can be parsed by the String
   Template library.  If the library encounters any errors then a message
   indicating that the template is invalid will be displayed."
  [template-dir template-name]
  (let [st-group (template-validating-st-group template-name)]
    (try
      (load-template template-dir template-name st-group)
      true
      (catch Exception _ false))))

(defn validate-templates
  "Validates multiple templates to ensure that they can be parsed by the String
   Template library."
  ([template-dir]
     (validate-templates template-dir (list-templates template-dir)))
  ([template-dir templates]
     (every? identity (map #(valid-template? template-dir %) templates))))

(defn- placeholder-validation-error-listener
  "Creates an error listener that can be used to record cases in which an error
   occurs while a template is being rendered.  The method we use to obtain the
   information we use to check for unused and undefined property values doesn't
   throw an exception if an undefined property is encountered.  This error
   listener will display a helpful error message and record the fact that an
   error occurred whenever an error is encountered."
  [env-path template valid?]
  (letfn [(base-msg  []
            (str "validation of " @template " for " env-path " failed:"))
          (print-err [msg]
            (dosync (ref-set valid? false))
            (println (base-msg) (str msg)))]
    (proxy [STErrorListener] []
      (compileTimeError [msg] (print-err msg))
      (runTimeError [msg] (print-err msg))
      (IOError [msg] (print-err msg))
      (internalError [msg] (print-err msg)))))

(defn- placeholder-validating-st-group
  "Creates an ST group with an error listener that includes the template and
   environment names in any error messages and remembers when errors occur."
  [env-path template valid?]
  (doto (new-st-group)
    (.setListener
     (placeholder-validation-error-listener env-path template valid?))))

(defn- find-used-params-in-template
  "Finds parameters used in a template.  This is done by using one of the
   debugging features of the String Template library: interpreter events.
   Each interpreter event contains an expression that is supposed to be
   evaluated.  Replacement expressions will always begin and end with the
   delimiter characters that are being used."
  [st-group template-dir template env]
  (let [st        (load-template template-dir @template st-group)
        _         (add-props env st)
        events    (.getEvents st)]
    (->> events
         (filter #(instance? EvalExprEvent %))
         (map #(.expr %))
         (map #(re-matches placeholder-re %))
         (filter identity)
         (map second)
         (map #(string/replace % "_" "-"))
         (map keyword))))

(defn- find-used-params
  "Obtains the names of all of the parameters that are used in all of the
   templates in a template directory."
  [st-group template-dir template env]
  (set (mapcat
        (fn [template-name]
          (dosync (ref-set template template-name))
          (find-used-params-in-template st-group template-dir template env))
        (list-templates template-dir))))

(defn- validate-placeholders-for
  "Verifies that there are no unused or undefined placeholders in an environment
   and a set of templates."
  [tmpl-dir [env-name dep env]]
  (let [env-path      (str env-name "." dep)
        valid?        (ref true)
        template      (ref nil)
        st-group      (placeholder-validating-st-group env-path template valid?)
        used-params   (union (self-referenced-params env)
                             (find-used-params st-group tmpl-dir template env))
        unused-params (difference (set (keys env)) used-params)]
    (when-not (empty? unused-params)
      (println "Unused parameters were detected in" (str env-path ":"))
      (dorun (map (partial println "\t") (sort unused-params))))
    (and @valid? (empty? unused-params))))

(defn validate-placeholders
  "Verifies that there are no unused or undefined placeholders in a set of
   templates and all of the environments defined in an environment definition
   file."
  [template-dir envs]
  (or (nil? envs)
      (every? identity
              (doall (map #(validate-placeholders-for template-dir %)
                          (envs-by-dep envs))))))

(defn display-template-list
  "Displays the list of templates in a template directory."
  [template-dir]
  (dorun (map println (list-templates template-dir))))
