(ns dingle.scm
  (:use [dingle.scripting]
        [cemerick.url])
  (:require [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(defn script-setup-scm
  "Non-execing script that will download the scm tarball
   and untar it.
   
   Params:
     scm-url - Full URL to the scm bundle. String
     working-dir - path to a local directory that the scm bundle will be 
                   untarred in. String.

   Returns string containing the bash commands that should be exec'ed."
  [scm-url working-dir]
  (let [scm-filename (ft/basename (:path (url scm-url)))] 
    (scriptify
      (rm -rf ~working-dir)
      (mkdir -p ~working-dir)
      (pushd ~working-dir)
      (curl -o ~scm-filename ~scm-url)
      (tar xzf ~scm-filename)
      (popd))))

(defn script-directory
  [working-dir]
  (ft/path-join working-dir "scm/scripts/analysis"))

(defn script-run-export-tool
  "Non-execing script that will run the export-tool.py. Assumes (script-setup-rpm)
   has already been run.

   Params:
     working-dir - Path to the directory where scm was untarred. String.
     de-host - Source DE deployment hostname. String.
     de-port - Source DE deployment port. String.
     source - Source DE version. String.

   Returns bash script as a string."
  [working-dir de-host de-port source]
  (let [script-dir (script-directory working-dir)] 
    (chain
      (cd ~script-dir)
      (python2.6 "export-tool.py" 
              "-p" ~de-port 
              "-h" ~de-host
              "-s" ~source))))

(defn script-run-import-tool
  "Non-execing script that will run the import-tool.py. Assumes 
   (script-setup-scm) and (script-run-export-tool) has already been run.

   Params:
      working-dir - Path to the directory where scm was untarred. String.
      de-host - Source DE deployment hostname. String.
      de-port - Source DE deployment port. String.
      dest - Destination DE version. String."
  [working-dir de-host de-port dest]
  (let [script-dir (script-directory working-dir)]
    (chain
      (cd ~script-dir)
      (python2.6 "import-tool.py"
              "-p" ~de-port
              "-h" ~de-host
              "-v" ~dest))))

(defn script-run-export-analyses
  "Non-execing script that will run export-analyses.py. Assumes
   (script-setup-scm has already been run.

   Params:
     working-dir - Path to the directory where scm was untarred. String.
     de-host - Source DE deployment hostname. String.
     de-port - Source DE deployment port. String.
     dest - Destination DE version. String.

   Returns a bash script in a string."
  [working-dir de-host de-port dest]
  (let [script-dir (script-directory working-dir)]
    (chain
      (cd ~script-dir)
      (python2.6 "export-analyses.py"
              "-p" ~de-port
              "-h" ~de-host
              "-d" ~dest))))

(defn script-run-import-analyses
  "Non-execing script that will run import-analyses.py. Assumes
   (script-setup-scm) has already been run.

   Params:
     working-dir - Path to the directory where scm was untarred. String.
     de-host - Source DE deployment hostname. String.
     de-port - Source DE deployment port. String.
     dest - Destination DE version. String."
  [working-dir de-host de-port dest]
  (let [script-dir (script-directory working-dir)]
    (chain
      (cd ~script-dir)
      (python2.6 "import-analyses.py"
                 "-p" ~de-port
                 "-h" ~de-host
                 "-d" ~dest))))

