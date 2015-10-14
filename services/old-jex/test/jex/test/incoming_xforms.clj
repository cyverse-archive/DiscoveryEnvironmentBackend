(ns jex.test.incoming-xforms
  (:use [jex.incoming-xforms] :reload)
  (:use [midje.sweet])
  (:require [jex.config :as cfg]))

(fact
 (replacer #"ll" "oo" "fll") => "foo"
 (replacer #"\s" "_" "foo oo") => "foo_oo")

(fact
 (replace-at "A" "foo@bar") => "fooAbar"
 (replace-at "A" "foobar@") => "foobarA"
 (replace-at "A" "@foobar") => "Afoobar")

(fact
 (at-underscore "foo@bar") => "foo_bar"
 (at-underscore "foobar@") => "foobar_"
 (at-underscore "@foobar") => "_foobar")

(fact
 (replace-space "^" "foo bar") => "foo^bar"
 (replace-space "^" "foobar ") => "foobar^"
 (replace-space "^" " foobar") => "^foobar")

(fact
 (space-underscore "foo bar") => "foo_bar"
 (space-underscore "foobar ") => "foobar_"
 (space-underscore " foobar") => "_foobar")

(fact
 (fmt-date now-fmt (java.util.Date. 0)) => "1969-12-31-17-00-00.000")

(fact
 (now-date {}) => #(contains? % :now_date)
 (now-date {} #(java.util.Date. 0)) => {:now_date "1969-12-31-17-00-00.000"})

(fact
 (pathize "/foo bar/baz@blippy/") => "/foo_bar/baz_blippy/")


(defn epoch-func [] (java.util.Date. 0))


(defmacro test-redefs
  [& body]
  `(with-redefs [cfg/run-on-nfs      (fn [] false)
                 cfg/nfs-base        (fn [] "/tmp/nfs-base")
                 cfg/irods-base      (fn [] "/tmp/irods-base")
                 cfg/condor-log-path (fn [] "/tmp/condor-log-path")
                 cfg/jar-path        (fn [] "/usr/local/lib/porklock/porklock-standalone.jar")
                 cfg/icommands-path  (fn [] "/usr/local/bin")
                 cfg/filter-files    (fn [] ",foo,bar,baz,blippy,cow,bees,")]
     (do ~@body)))

(test-redefs
  (facts
    (analysis-attrs {:username "wregglej"} epoch-func) => (contains {:run-on-nfs  (cfg/run-on-nfs)})
    (analysis-attrs {:username "wregglej"} epoch-func) => (contains {:type "analysis"})
    (analysis-attrs {:username "wregglej"} epoch-func) => (contains {:username "wregglej"})
    (analysis-attrs {:username "wregglej"} epoch-func) => (contains {:nfs_base (cfg/nfs-base)})
    (analysis-attrs {:username "wregglej"} epoch-func) => (contains {:irods_base (cfg/irods-base)})
    (analysis-attrs {:username "wregglej"} epoch-func) => (contains {:submission_date 0})

    (analysis-attrs {:username "wr @lej"} epoch-func) => (contains {:run-on-nfs (cfg/run-on-nfs)})
    (analysis-attrs {:username "wr @lej"} epoch-func) => (contains {:type "analysis"})
    (analysis-attrs {:username "wr @lej"} epoch-func) => (contains {:username "wr__lej"})
    (analysis-attrs {:username "wr @lej"} epoch-func) => (contains {:nfs_base (cfg/nfs-base)})
    (analysis-attrs {:username "wr @lej"} epoch-func) => (contains {:irods_base (cfg/irods-base)})
    (analysis-attrs {:username "wr @lej"} epoch-func) => (contains {:submission_date 0})

    (analysis-attrs {:username "wregglej" :type "foo"} epoch-func) => (contains {:run-on-nfs (cfg/run-on-nfs)})
    (analysis-attrs {:username "wregglej" :type "foo"} epoch-func) => (contains {:type "foo"})
    (analysis-attrs {:username "wregglej" :type "foo"} epoch-func) => (contains {:username "wregglej"})
    (analysis-attrs {:username "wregglej" :type "foo"} epoch-func) => (contains {:nfs_base (cfg/nfs-base)})
    (analysis-attrs {:username "wregglej" :type "foo"} epoch-func) => (contains {:irods_base (cfg/irods-base)})
    (analysis-attrs {:username "wregglej" :type "foo"} epoch-func) => (contains {:submission_date 0})))

(test-redefs
 (def out-dir-test-0
   {:irods_base (cfg/irods-base)
    :username "wregglej"
    :name "out-dir-test-0"
    :now_date "1969-12-31-17-00-00.000"})

 (def out-dir-test-1
   {:irods_base (cfg/irods-base)
    :username "wregglej"
    :output_dir ""
    :create_output_subdir true
    :name "out-dir-test-1"
    :now_date "1969-12-31-17-00-00.000"})

 (def out-dir-test-2
   {:irods_base (cfg/irods-base)
    :username "wregglej"
    :now_date "1969-12-31-17-00-00.000"
    :name "out-dir-test-2"
    :create_output_subdir false
    :output_dir ""})

 (def out-dir-test-3
   {:irods_base (cfg/irods-base)
    :username "wregglej"
    :now_date "1969-12-31-17-00-00.000"
    :name "out-dir-test-3"
    :create_output_subdir true
    :output_dir "/my/output-dir/"})

 (def out-dir-test-4
   (assoc out-dir-test-3
     :create_output_subdir false
     :name "out-dir-test-4"))

 (fact
  (output-directory out-dir-test-0) =>
  "/tmp/irods-base/wregglej/analyses/out-dir-test-0-1969-12-31-17-00-00.000"

  (output-directory out-dir-test-1) =>
  "/tmp/irods-base/wregglej/analyses/out-dir-test-1-1969-12-31-17-00-00.000"

  (output-directory out-dir-test-2) =>
  "/tmp/irods-base/wregglej/analyses/out-dir-test-2-1969-12-31-17-00-00.000"

  (output-directory out-dir-test-3) =>
  "/my/output-dir/out-dir-test-3-1969-12-31-17-00-00.000"

  (output-directory out-dir-test-4) =>
  "/my/output-dir"))

(test-redefs
 (def context-dirs-map
  {:username "wregglej"
   :nfs_base (cfg/nfs-base)
   :irods_base (cfg/irods-base)
   :name "context-dirs-map"
   :create_output_subdir true
   :output_dir "/my/output-dir"
   :now_date "1969-12-31-17-00-00.000"})

 (fact
  (context-dirs context-dirs-map) =>
  (merge
   context-dirs-map
   {:output_dir
    (output-directory context-dirs-map)

    :working_dir
    "/tmp/nfs-base/wregglej/context-dirs-map-1969-12-31-17-00-00.000/"

    :condor-log-dir
    "/tmp/condor-log-path/wregglej/context-dirs-map-1969-12-31-17-00-00.000/"})))


(def p
  [{:name "-n0" :value "v0" :order "0" :foo "bar"}
   {:name "-n1" :value "v1" :order "1" :bar "a"}
   {:name "-n2" :value "v2" :order "2" :bees "no!"}
   {:name "-n3" :value "v3" :order "3" :bears "oh my"}])

(test-redefs
 (fact
  (param-maps p) => [{:name "-n0" :value "v0" :order "0"}
                     {:name "-n1" :value "v1" :order "1"}
                     {:name "-n2" :value "v2" :order "2"}
                     {:name "-n3" :value "v3" :order "3"}]))

(test-redefs
 (fact
  (naively-quote "foo 'bar' baz") => "'foo '\\''bar'\\'' baz'"
  (naively-quote "''foo''") => "''\\'''\\''foo'\\'''\\'''"))

(test-redefs
 (fact
  (quote-value "foo 'bar' baz") => "'foo '\\''bar'\\'' baz'"
  (quote-value "''foo''") => "\\'''\\''foo'\\'''\\'"))

(def fancy-params
  [{:name "-n1" :value "foo 'bar' baz" :order 5}
   {:name "-n2" :value "''foo''" :order 4}
   {:name "-n3" :value "nargle" :order 3}])

(test-redefs
 (fact
  (escape-params fancy-params) =>
  "-n3 'nargle' -n2 \\'''\\''foo'\\'''\\' -n1 'foo '\\''bar'\\'' baz'"))

(test-redefs
 (fact
  (format-env-variables {:foo "bar" :baz "blippy"}) =>
  "foo=\"bar\" baz=\"blippy\""))

(def step-map
  {:component
   {:location "/usr/local/bin/"
    :name "footastic"}
   :config
   {:params p}
   :stdin "/test/stdin"
   :stdout "/test/stdout"
   :stderr "/test/stderr"
   :environment {:foo "bar" :baz "blippy"}})

(test-redefs
 (fact
  (executable step-map) => "/usr/local/bin/footastic")

 (fact
  (arguments step-map) => "-n0 'v0' -n1 'v1' -n2 'v2' -n3 'v3'")

 (fact
  (stdin step-map) => "'/test/stdin'")

 (fact
  (stdout step-map 0) => "'/test/stdout'")

 (fact
  (stderr step-map 0) => "'/test/stderr'")

 (fact
  (environment step-map) => "foo=\"bar\" baz=\"blippy\"")

 (fact
  (log-file {:log-file "log-file"} 0 "/tmp/logs") => "/tmp/logs/log-file"
  (log-file {} 0 "/tmp/logs") => "/tmp/logs/logs/condor-log-0")

 (fact
  (step-iterator-vec {:steps [{} {} {}]}) => [[0 {}] [1 {}] [2 {}]]))

(def step-map1
  {:component
   {:location "/usr/local/bin"
    :name "footastic1"}
   :config {:params p}
   :stdin "/test/stdin1"
   :stdout "/test/stdout1"
   :stderr "/test/stderr1"
   :environment {:PATH "/usr/local/bin"}
   :log-file "log-file1"})

(def condor-map
  {:steps [step-map step-map1]
   :submission_date 0
   :condor-log-dir "/tmp"})

(test-redefs
 (fact
  (process-steps condor-map) =>
  (sequence
   [
    {:id "condor-0"
     :type "condor"
     :submission_date 0
     :status "Submitted"
     :environment "foo=\"bar\" baz=\"blippy\""
     :executable "/usr/local/bin/footastic"
     :arguments "-n0 'v0' -n1 'v1' -n2 'v2' -n3 'v3'"
     :stdout "'/test/stdout'"
     :stderr "'/test/stderr'"
     :stdin "/test/stdin"
     :log-file "/tmp/logs/condor-log-0"
     :component
     {:location "/usr/local/bin/"
      :name "footastic"}
     :config
     {:params p}}
    {:id "condor-1"
     :type "condor"
     :submission_date 0
     :status "Submitted"
     :environment "PATH=\"/usr/local/bin\""
     :executable "/usr/local/bin/footastic1"
     :arguments "-n0 'v0' -n1 'v1' -n2 'v2' -n3 'v3'"
     :stdout "'/test/stdout1'"
     :stderr "'/test/stderr1'"
     :stdin "/test/stdin1"
     :log-file "/tmp/log-file1"
     :component
     {:location "/usr/local/bin"
      :name "footastic1"}
     :config {:params p}}])))

(test-redefs
 (fact
  (steps condor-map) =>
  (assoc condor-map
    :steps (sequence
            [
             {:id "condor-0"
              :type "condor"
              :submission_date 0
              :status "Submitted"
              :environment "foo=\"bar\" baz=\"blippy\""
              :executable "/usr/local/bin/footastic"
              :arguments "-n0 'v0' -n1 'v1' -n2 'v2' -n3 'v3'"
              :stdout "'/test/stdout'"
              :stderr "'/test/stderr'"
              :stdin "/test/stdin"
              :log-file "/tmp/logs/condor-log-0"
              :component
              {:location "/usr/local/bin/"
               :name "footastic"}
              :config
              {:params p}}
             {:id "condor-1"
              :type "condor"
              :submission_date 0
              :status "Submitted"
              :environment "PATH=\"/usr/local/bin\""
              :executable "/usr/local/bin/footastic1"
              :arguments "-n0 'v0' -n1 'v1' -n2 'v2' -n3 'v3'"
              :stdout "'/test/stdout1'"
              :stderr "'/test/stderr1'"
              :stdin "/test/stdin1"
              :log-file "/tmp/log-file1"
              :component
              {:location "/usr/local/bin"
               :name "footastic1"}
              :config {:params p}}]))))

(test-redefs
 (fact
  (handle-source-path "/tmp/foo" "collection") => "/tmp/foo/"
  (handle-source-path "/tmp/foo" "single") => "/tmp/foo"
  (handle-source-path "/tmp/foo" "") => "/tmp/foo"
  (handle-source-path "/tmp/foo" nil) => "/tmp/foo")

 (fact
  (input-id-str 0 0) => "condor-0-input-0"
  (input-id-str 0 1) => "condor-0-input-1")

 (fact
  (input-stdout 0 0) => "logs/condor-0-input-0-stdout"
  (input-stdout 0 1) => "logs/condor-0-input-1-stdout")

 (fact
  (input-stderr 0 0) => "logs/condor-0-input-0-stderr"
  (input-stderr 0 1) => "logs/condor-0-input-1-stderr")

 (fact
  (input-log-file "/tmp" 0 0) => "/tmp/logs/condor-0-input-0-log"
  (input-log-file "/tmp" 0 1) => "/tmp/logs/condor-0-input-1-log")

 (fact
  (input-arguments {:username "foo"} "/tmp/foo" {:multiplicity "collection"}) =>
  "get --user foo --source '/tmp/foo/' --config logs/irods-config"

  (input-arguments {:username "foo"} "/tmp/foo" {:multiplicity "single"}) =>
  "get --user foo --source '/tmp/foo' --config logs/irods-config"

  (input-arguments {:username "foo"} "/tmp/foo" {:multiplicity ""}) =>
  "get --user foo --source '/tmp/foo' --config logs/irods-config"

  (input-arguments {:username "foo"} "/tmp/foo" {}) =>
  "get --user foo --source '/tmp/foo' --config logs/irods-config")

 (fact
  (input-iterator-vec {:config {:input [{:step 1} {:step 2} {:step 3}]}}) =>
  [[0 {:step 1}] [1 {:step 2}] [2 {:step 3}]]))

(def input-condor-map
  {:submission_date 0
   :username "foo"
   :condor-log-dir "/tmp"
   :steps
   [{:config
     {:input
      [{:retain true
        :multiplicity "collection"
        :value "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :value "/tmp/source1"}]}}]})

(test-redefs
 (fact
  (process-step-inputs
   input-condor-map
   [0
    {:config
     {:input
      [{:retain true
        :multiplicity "collection"
        :value "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :value "/tmp/source1"}]}}]) =>
  (sequence
   [{:id "condor-0-input-0"
     :submission_date 0
     :type "condor"
     :status "Submitted"
     :retain true
     :multi "collection"
     :source "/tmp/source"
     :executable "/usr/local/bin/filetool"
     :environment "PATH=/usr/local/bin"
     :arguments "get --user foo --source '/tmp/source/' --config logs/irods-config"
     :stdout "logs/condor-0-input-0-stdout"
     :stderr "logs/condor-0-input-0-stderr"
     :log-file "/tmp/logs/condor-0-input-0-log"}
    {:id "condor-0-input-1"
     :submission_date 0
     :type "condor"
     :status "Submitted"
     :retain false
     :multi "single"
     :source "/tmp/source1"
     :executable "/usr/local/bin/filetool"
     :environment "PATH=/usr/local/bin"
     :arguments "get --user foo --source '/tmp/source1' --config logs/irods-config"
     :stdout "logs/condor-0-input-1-stdout"
     :stderr "logs/condor-0-input-1-stderr"
     :log-file "/tmp/logs/condor-0-input-1-log"}])))

(test-redefs
 (fact
  (process-inputs input-condor-map) =>
  (sequence
   [{:input-jobs
     (sequence
      [{:id "condor-0-input-0"
        :submission_date 0
        :type "condor"
        :status "Submitted"
        :retain true
        :multi "collection"
        :source "/tmp/source"
        :executable "/usr/local/bin/filetool"
        :environment "PATH=/usr/local/bin"
        :arguments "get --user foo --source '/tmp/source/' --config logs/irods-config"
        :stdout "logs/condor-0-input-0-stdout"
        :stderr "logs/condor-0-input-0-stderr"
        :log-file "/tmp/logs/condor-0-input-0-log"}
       {:id "condor-0-input-1"
        :submission_date 0
        :type "condor"
        :status "Submitted"
        :retain false
        :multi "single"
        :source "/tmp/source1"
        :executable "/usr/local/bin/filetool"
        :environment "PATH=/usr/local/bin"
        :arguments "get --user foo --source '/tmp/source1' --config logs/irods-config"
        :stdout "logs/condor-0-input-1-stdout"
        :stderr "logs/condor-0-input-1-stderr"
        :log-file "/tmp/logs/condor-0-input-1-log"}])
     :config
     {:input
      [{:retain true
        :multiplicity "collection"
        :value "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :value "/tmp/source1"}]}}])))

(test-redefs
 (fact
  (input-jobs input-condor-map) =>
  {:submission_date 0
   :username "foo"
   :condor-log-dir "/tmp"
   :steps
   [{:input-jobs
     (sequence
      [{:id "condor-0-input-0"
        :submission_date 0
        :type "condor"
        :status "Submitted"
        :retain true
        :multi "collection"
        :source "/tmp/source"
        :executable "/usr/local/bin/filetool"
        :environment "PATH=/usr/local/bin"
        :arguments "get --user foo --source '/tmp/source/' --config logs/irods-config"
        :stdout "logs/condor-0-input-0-stdout"
        :stderr "logs/condor-0-input-0-stderr"
        :log-file "/tmp/logs/condor-0-input-0-log"}
       {:id "condor-0-input-1"
        :submission_date 0
        :type "condor"
        :status "Submitted"
        :retain false
        :multi "single"
        :source "/tmp/source1"
        :executable "/usr/local/bin/filetool"
        :environment "PATH=/usr/local/bin"
        :arguments "get --user foo --source '/tmp/source1' --config logs/irods-config"
        :stdout "logs/condor-0-input-1-stdout"
        :stderr "logs/condor-0-input-1-stderr"
        :log-file "/tmp/logs/condor-0-input-1-log"}])
     :config
     {:input
      [{:retain true
        :multiplicity "collection"
        :value "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :value "/tmp/source1"}]}}]}))

(test-redefs
 (fact
  (output-arguments "foo" "/tmp/source" "/tmp/dest") =>
  "put --user foo --source '/tmp/source' --destination '/tmp/dest' --config logs/irods-config")

 (fact
  (output-id-str 0 0) => "condor-0-output-0"
  (output-id-str 0 1) => "condor-0-output-1")

 (fact
  (output-iterator-vec {:config {:output [{:step 0} {:step 1} {:step 2}]}}) =>
  [[0 {:step 0}] [1 {:step 1}] [2 {:step 2}]]))

(def output-condor-map
  {:submission_date 0
   :username "foo"
   :condor-log-dir "/tmp"
   :output_dir "/tmp/output-dir"
   :steps
   [{:config
     {:output
      [{:retain true
        :multiplicity "collection"
        :name "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :name "/tmp/source1"}]}}]})

(test-redefs
 (fact
  (process-step-outputs
   output-condor-map
   [0
    {:config
     {:output
      [{:retain true
        :multiplicity "collection"
        :name "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :name "/tmp/source1"}]}}]) =>
  (sequence
   [{:id "condor-0-output-0"
     :submission_date 0
     :type "condor"
     :status "Submitted"
     :retain true
     :multi "collection"
     :source "/tmp/source"
     :executable "/usr/local/bin/filetool"
     :environment "PATH=/usr/local/bin"
     :arguments "put --user foo --source '/tmp/source' --destination '/tmp/output-dir' --config logs/irods-config"
     :dest "/tmp/output-dir"}
    {:id "condor-0-output-1"
     :submission_date 0
     :type "condor"
     :status "Submitted"
     :retain false
     :multi "single"
     :source "/tmp/source1"
     :executable "/usr/local/bin/filetool"
     :environment "PATH=/usr/local/bin"
     :arguments "put --user foo --source '/tmp/source1' --destination '/tmp/output-dir' --config logs/irods-config"
     :dest "/tmp/output-dir"}])))

(test-redefs
 (fact
  (process-outputs output-condor-map) =>
  (sequence
   [{:config
     {:output
      [{:retain true
        :multiplicity "collection"
        :name "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :name "/tmp/source1"}]}
     :output-jobs
     (sequence
      [{:id "condor-0-output-0"
        :submission_date 0
        :type "condor"
        :status "Submitted"
        :retain true
        :multi "collection"
        :source "/tmp/source"
        :executable "/usr/local/bin/filetool"
        :environment "PATH=/usr/local/bin"
        :arguments "put --user foo --source '/tmp/source' --destination '/tmp/output-dir' --config logs/irods-config"
        :dest "/tmp/output-dir"}
       {:id "condor-0-output-1"
        :submission_date 0
        :type "condor"
        :status "Submitted"
        :retain false
        :multi "single"
        :source "/tmp/source1"
        :executable "/usr/local/bin/filetool"
        :environment "PATH=/usr/local/bin"
        :arguments "put --user foo --source '/tmp/source1' --destination '/tmp/output-dir' --config logs/irods-config"
        :dest "/tmp/output-dir"}])}])))

(test-redefs
 (fact
  (output-jobs output-condor-map) =>
  (merge output-condor-map
         {:steps
          (sequence
           [{:config
             {:output
              [{:retain true
                :multiplicity "collection"
                :name "/tmp/source"}
               {:retain false
                :multiplicity "single"
                :name "/tmp/source1"}]}
             :output-jobs
             (sequence
              [{:id "condor-0-output-0"
                :submission_date 0
                :type "condor"
                :status "Submitted"
                :retain true
                :multi "collection"
                :source "/tmp/source"
                :executable "/usr/local/bin/filetool"
                :environment "PATH=/usr/local/bin"
                :arguments "put --user foo --source '/tmp/source' --destination '/tmp/output-dir' --config logs/irods-config"
                :dest "/tmp/output-dir"}
               {:id "condor-0-output-1"
                :submission_date 0
                :type "condor"
                :status "Submitted"
                :retain false
                :multi "single"
                :source "/tmp/source1"
                :executable "/usr/local/bin/filetool"
                :environment "PATH=/usr/local/bin"
                :arguments "put --user foo --source '/tmp/source1' --destination '/tmp/output-dir' --config logs/irods-config"
                :dest "/tmp/output-dir"}])}])})))

(test-redefs
 (fact
  (all-input-jobs
   {:steps
    [{:input-jobs
      [{:job 1} {:job 2} {:job 3}]}
     {:input-jobs
      [{:job 4} {:job 5} {:job 6}]}]}) =>
  {:steps
   [{:input-jobs
     [{:job 1} {:job 2} {:job 3}]}
    {:input-jobs
     [{:job 4} {:job 5} {:job 6}]}]
   :all-input-jobs [{:job 1} {:job 2} {:job 3} {:job 4} {:job 5} {:job 6}]})

 (fact
  (all-output-jobs
   {:steps
    [{:output-jobs
      [{:job 1} {:job 2} {:job 3}]}
     {:output-jobs
      [{:job 4} {:job 5} {:job 6}]}]}) =>
  {:steps
   [{:output-jobs
     [{:job 1} {:job 2} {:job 3}]}
    {:output-jobs
     [{:job 4} {:job 5} {:job 6}]}]
   :all-output-jobs [{:job 1} {:job 2} {:job 3} {:job 4} {:job 5} {:job 6}]}))

(test-redefs
 (fact
  (input-coll {:multi "collection" :source "/tmp/source"}) => "'source/'"
  (input-coll {:multi "single" :source "/tmp/source"}) => "'source'")

 (fact
  (make-abs-output "foo/bar") => "$(pwd)/'foo/bar'"
  (make-abs-output "/foo/bar") => "'/foo/bar'")

 (fact
  (output-coll {:multi "collection" :source "/tmp/source"}) => "'/tmp/source/'"
  (output-coll {:multi "collection" :source "source"}) => "$(pwd)/'source/'"
  (output-coll {:multi "single" :source "/tmp/source"}) => "/tmp/source"
  (output-coll {:multi "single" :source "tmp/source"}) => "tmp/source"))

(def outjobs
  [{:retain true
    :multi "collection"
    :source "/tmp/output-source0"}
   {:retain false
    :multi "single"
    :source "/tmp/output-source1"}
   {:retain "true"
    :multi "collection"
    :source "/tmp/output-source2"}])

(def injobs
  [{:retain true
    :multi "collection"
    :source "/tmp/input-source0"}
   {:retain false
    :multi "single"
    :source "/tmp/input-source1"}
   {:retain true
    :multi "collection"
    :source "/tmp/input-source2"}])

(test-redefs
 (fact
  (exclude-arg injobs outjobs) =>
  "--exclude ,foo,bar,baz,blippy,cow,bees,,/tmp/output-source1,'input-source1'")

 (fact
  (shotgun-job-map {:output_dir      "/tmp/output"
                    :condor-log-dir  "/tmp/condor-log"
                    :all-input-jobs  injobs
                    :all-output-jobs outjobs
                    :username        "testuser"}) =>
  {:id "output-last"
   :status "Submitted"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :stderr "logs/output-last-stderr"
   :stdout "logs/output-last-stdout"
   :log-file "/tmp/condor-log/logs/output-last-log"
   :arguments
   "put --user testuser --config logs/irods-config --destination '/tmp/output' --exclude ,foo,bar,baz,blippy,cow,bees,,/tmp/output-source1,'input-source1'"}))

(def testmap
  {:output_dir "/tmp/output"
   :condor-log-dir "/tmp/condor-log"
   :all-input-jobs injobs
   :all-output-jobs outjobs
   :username "testuser"})


(test-redefs
 (facts
  (extra-jobs testmap) => (contains {:output_dir "/tmp/output"})
  (extra-jobs testmap) => (contains {:condor-log-dir "/tmp/condor-log"})
  (extra-jobs testmap) => (contains {:all-input-jobs injobs})
  (extra-jobs testmap) => (contains {:all-output-jobs outjobs})
  (extra-jobs testmap) => (contains {:username "testuser"})
  (extra-jobs testmap) => (contains {:imkdir-job map?})
  (:imkdir-job (extra-jobs testmap)) => (contains {:id "imkdir"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:status "Submitted"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:environment "PATH=/usr/local/bin"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:executable "/usr/local/bin/filetool"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:stderr "logs/imkdir-stderr"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:stdout "logs/imkdir-stdout"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:log-file "/tmp/condor-log/logs/imkdir-log"})
  (:imkdir-job (extra-jobs testmap)) => (contains {:arguments "mkdir --user testuser --destination '/tmp/output'"})
  (:final-output-job (extra-jobs testmap)) => (contains {:id "output-last"})
  (:final-output-job (extra-jobs testmap)) => (contains {:status "Submitted"})
  (:final-output-job (extra-jobs testmap)) => (contains {:executable "/usr/local/bin/filetool"})
  (:final-output-job (extra-jobs testmap)) => (contains {:environment "PATH=/usr/local/bin"})
  (:final-output-job (extra-jobs testmap)) => (contains {:stderr "logs/output-last-stderr"})
  (:final-output-job (extra-jobs testmap)) => (contains {:stdout "logs/output-last-stdout"})
  (:final-output-job (extra-jobs testmap)) => (contains {:log-file "/tmp/condor-log/logs/output-last-log"})
  (:final-output-job (extra-jobs testmap)) => (contains {:arguments "put --user testuser --config logs/irods-config --destination '/tmp/output' --exclude ,foo,bar,baz,blippy,cow,bees,,/tmp/output-source1,'input-source1'"}))

 (fact
  (rm-step-component
   {:steps
    [{:component "foo"}
     {:component "bar"}]}) => {:steps [{} {}]})

 (fact
  (rm-step-config
   {:steps
    [{:config "foo"}
     {:config "bar"}]}) => {:steps [{} {}]}))

(def final-test-map
  {:username "wregglej"
   :output_dir "/tmp/output"
   :create_output_subdir true
   :name "final-test-map"
   :steps
   [{:component
     {:location "/usr/local/bin"
      :name "foobar"}
     :config
     {:params
      [{:name "-n0"
        :value "param0"}
       {:name "-n1"
        :value "param1"}]
      :input
      [{:retain true
        :multiplicity "collection"
        :value "/tmp/source"}
       {:retain false
        :multiplicity "single"
        :value "/tmp/source1"}]}}
    {:component
     {:location "/usr/local/bin"
      :name "foobar1"}
     :config
     {:params
      [{:name "-n2"
        :value "param2"}
       {:name "-n3"
        :value "param3"}]
      :output
      [{:retain true
        :multiplicity "collection"
        :name "/tmp/output1"}
       {:retain false
        :multiplicity "single"
        :name "/tmp/output2"}]
      :input
      [{:retain true
        :multiplicity "collection"
        :value "/tmp/source2"}
       {:retain false
        :multiplicity "single"
        :value "/tmp/source3"}]}}]})

(test-redefs
 (def transform-output
  (transform final-test-map epoch-func))


 (fact
  (:now_date transform-output) => "1969-12-31-17-00-00.000"
  (:username transform-output) => "wregglej"

  (:output_dir transform-output) =>
  "/tmp/output/final-test-map-1969-12-31-17-00-00.000"

  (:create_output_subdir transform-output) => true?
  (:condor-log-dir transform-output) =>
  "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/"

  (:name transform-output) => "final-test-map"
  (:working_dir transform-output) =>
  "/tmp/nfs-base/wregglej/final-test-map-1969-12-31-17-00-00.000/"

  (first (:all-input-jobs transform-output)) =>
  {:id "condor-0-input-0"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :submission_date 0
   :status "Submitted"
   :arguments "get --user wregglej --source '/tmp/source/' --config /tmp/nfs-base/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/irods-config"
   :type "condor"
   :source "/tmp/source"
   :stderr "logs/condor-0-input-0-stderr"
   :stdout "logs/condor-0-input-0-stdout"
   :log-file "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/condor-0-input-0-log"
   :multi "collection"
   :retain true}

  (nth (:all-input-jobs transform-output) 1) =>
  {:id "condor-0-input-1"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :submission_date 0
   :status "Submitted"
   :arguments "get --user wregglej --source '/tmp/source1' --config /tmp/nfs-base/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/irods-config"
   :type "condor"
   :source "/tmp/source1"
   :stderr "logs/condor-0-input-1-stderr"
   :stdout "logs/condor-0-input-1-stdout"
   :log-file "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/condor-0-input-1-log"
   :multi "single"
   :retain false}

  (nth (:all-input-jobs transform-output) 2) =>
  {:id "condor-1-input-0"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :submission_date 0
   :status "Submitted"
   :arguments "get --user wregglej --source '/tmp/source2/' --config /tmp/nfs-base/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/irods-config"
   :type "condor"
   :source "/tmp/source2"
   :stderr "logs/condor-1-input-0-stderr"
   :stdout "logs/condor-1-input-0-stdout"
   :log-file "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/condor-1-input-0-log"
   :multi "collection"
   :retain true}

  (nth (:all-input-jobs transform-output) 3) =>
  {:id "condor-1-input-1"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :submission_date 0
   :status "Submitted"
   :arguments "get --user wregglej --source '/tmp/source3' --config /tmp/nfs-base/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/irods-config"
   :type "condor"
   :source "/tmp/source3"
   :stderr "logs/condor-1-input-1-stderr"
   :stdout "logs/condor-1-input-1-stdout"
   :log-file "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/condor-1-input-1-log"
   :multi "single"
   :retain false}

  (first (:all-output-jobs transform-output)) =>
  {:id "condor-1-output-0"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :submission_date 0
   :status "Submitted"
   :arguments "put --user wregglej --source '/tmp/output1' --destination '/tmp/output/final-test-map-1969-12-31-17-00-00.000' --config logs/irods-config"
   :type "condor"
   :multi "collection"
   :source "/tmp/output1"
   :dest "/tmp/output/final-test-map-1969-12-31-17-00-00.000"
   :retain true}

  (nth (:all-output-jobs transform-output) 1) =>
  {:id "condor-1-output-1"
   :executable "/usr/local/bin/filetool"
   :environment "PATH=/usr/local/bin"
   :submission_date 0
   :status "Submitted"
   :arguments "put --user wregglej --source '/tmp/output2' --destination '/tmp/output/final-test-map-1969-12-31-17-00-00.000' --config logs/irods-config"
   :type "condor"
   :source "/tmp/output2"
   :dest "/tmp/output/final-test-map-1969-12-31-17-00-00.000"
   :multi "single"
   :retain false}))

(def first-step (first (:steps transform-output)))

(test-redefs
 (fact
  (:id first-step) => "condor-0"
  (:executable first-step) => "/usr/local/bin/foobar"
  (:environment first-step) => nil?
  (:type first-step) => "condor"
  (:submission_date first-step) => 0
  (:status first-step) => "Submitted"
  (:arguments first-step) => "-n0 'param0' -n1 'param1'"
  (:stderr first-step) => "logs/condor-stderr-0"
  (:stdout first-step) => "logs/condor-stdout-0"
  (:log-file first-step) =>
  "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/condor-log-0"))

;;;As a note, we don't care about the input-jobs and the output-jobs
;;;at the step level. They're the same as the all-input-jobs, and
;;;all-output-jobs, so testing them should be a moot point. Also,
;;;I don't believe they're used anywhere.

(def second-step (nth (:steps transform-output) 1))

(test-redefs
 (fact
  (:id second-step) => "condor-1"
  (:executable second-step) => "/usr/local/bin/foobar1"
  (:environment second-step) => nil?
  (:type second-step) => "condor"
  (:submission_date second-step) => 0
  (:status second-step) => "Submitted"
  (:arguments second-step) => "-n2 'param2' -n3 'param3'"
  (:stderr second-step) => "logs/condor-stderr-1"
  (:stdout second-step) => "logs/condor-stdout-1"
  (:log-file second-step) =>
  "/tmp/condor-log-path/wregglej/final-test-map-1969-12-31-17-00-00.000/logs/condor-log-1"))





