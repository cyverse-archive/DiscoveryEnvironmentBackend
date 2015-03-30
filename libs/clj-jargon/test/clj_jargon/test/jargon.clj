(ns clj-jargon.test.jargon
  (:use [clojure.test]
        [clj-jargon.init]
        [clj-jargon.item-info]
        [clj-jargon.item-ops])
  (:require [slingshot.slingshot :as ss]
            [boxy.core :as boxy]
            [boxy.jargon-if :as irods])
  (:import [org.irods.jargon.core.connection IRODSAccount]
           [org.irods.jargon.core.exception FileNotFoundException]
           [org.irods.jargon.core.pub CollectionAO
                                      CollectionAndDataObjectListAndSearchAO
                                      DataObjectAO
                                      IRODSAccessObjectFactory
                                      IRODSFileSystemAO
                                      QuotaAO
                                      UserAO
                                      UserGroupAO]
           [org.irods.jargon.core.pub.io IRODSFileFactory]))


(def ^{:private true} too-long-dir-name 
  "/zone/home/user/trash-home-rods-wregglej-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.183209331-12345bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")


(def ^{:private true} init-repo
  {:users                 #{"user"}
   :groups                {}
   "/zone"                {:type :normal-dir
                           :acl  {}
                           :avus {}}
   "/zone/home"           {:type :normal-dir
                           :acl  {}
                           :avus {}}
   "/zone/home/user"      {:type :normal-dir
                           :acl  {}
                           :avus {}}
   "/zone/home/user/file" {:type    :file
                           :acl     {"user" :read}
                           :avus    {}
                           :content ""}
   "/zone/home/user/link" {:type :linked-dir
                           :acl  {}
                           :avus {}}
   too-long-dir-name      {:type :normal-dir
                           :acl  {}
                           :avus {}}})


(defrecord ^{:private true} IRODSProxyStub [repo-ref closed-ref?]  
  irods/IRODSProxy
  
  (close 
    [_]
    (reset! closed-ref? true))

  (getIRODSAccessObjectFactory 
    [_] 
    (boxy/mk-mock-ao-factory repo-ref))

  (getIRODSFileFactory 
    [_ acnt]
    (boxy/mk-mock-file-factory repo-ref acnt)))
                                   

(defn- mk-cm
  []
  ;; This hideous monstrousity was created to make the tests of functions to be 
  ;; used inside of with-jargon independent of the test of with-jargon.  Better 
  ;; would be to split the src/jargon.clj into two modules, one with the init 
  ;; and with-jargon functions and their friends; the other with the functions 
  ;; requiring a context map.
  (let [host "host"
        port  1294
        user  "user"
        pass "passwd"
        home "/zone/home/user"
        zone "zone"
        res  "resource"
        acnt (IRODSAccount. host port user pass home zone res)
        fs   #(->IRODSProxyStub (atom init-repo) (atom false))
        aof  (.getIRODSAccessObjectFactory fs)]
    {:host                host 
     :port                (Integer/toString port) 
     :username            user
     :password            pass 
     :home                home 
     :zone                zone 
     :defaultResource     res
     :max-retries         0
     :retry-sleep         0
     :use-trash           false
     :proxy               fs
     :irodsAccount        acnt
     :accessObjectFactory aof
     :collectionAO        (.getCollectionAO aof acnt)
     :dataObjectAO        (.getDataObjectAO aof acnt)
     :userAO              (.getUserAO aof acnt)
     :userGroupAO         (.getUserGroupAO aof acnt)
     :fileFactory         (.getIRODSFileFactory fs acnt)
     :fileSystemAO        (.getIRODSFileSystemAO aof acnt)
     :lister              (.getCollectionAndDataObjectListAndSearchAO 
                            aof 
                            acnt)
     :quotaAO             (.getQuotaAO aof acnt)}))   


(deftest test-simple-init
  (let [cfg (init "host" "port" "user" "passwd" "home" "zone" "resource")]
    (is (= "host" (:host cfg)))
    (is (= "port" (:port cfg)))
    (is (= "user" (:username cfg)))
    (is (= "passwd" (:password cfg)))
    (is (= "home" (:home cfg)))
    (is (= "zone" (:zone cfg)))
    (is (= "resource" (:defaultResource cfg)))
    (is (= 0 (:max-retries cfg)))
    (is (= false (:use-trash cfg)))
    (is (= (default-proxy-ctor) (:proxy cfg)))))


(deftest test-init-options
  (let [test-ctor (fn [])
        cfg       (init "host" "port" "user" "passwd" "home" "zone" "resource" 
                        :max-retries 1
                        :retry-sleep 2
                        :use-trash   true
                        :proxy-ctor  test-ctor)]
    (is (= 1 (:max-retries cfg)))
    (is (= 2 (:retry-sleep cfg)))
    (is (= true (:use-trash cfg)))
    (is (= (test-ctor) (:proxy cfg)))))


(deftest test-with-jargon
  (letfn [(->cfg [closed?-ref] {:host            "host" 
                                :port            "0" 
                                :username        "user" 
                                :password        "passwd" 
                                :home            "/zone/home/user" 
                                :zone            "zone" 
                                :defaultResource "resource"
                                :max-retries     0
                                :retry-sleep     0
                                :use-trash       false
                                :proxy           (->IRODSProxyStub (atom init-repo) closed?-ref)}]
    (testing "normal operation"
      (let [closed? (atom false)
            cfg     (->cfg closed?)]
        (with-jargon cfg [cm]
          (doall (map #(is (= (% cfg) (% cm))) 
                      (keys cfg)))
          (is (instance? IRODSAccount (:irodsAccount cm)))
          (is (instance? IRODSProxyStub (:proxy cm)))
          (is (instance? IRODSAccessObjectFactory (:accessObjectFactory cm)))
          (is (instance? CollectionAO (:collectionAO cm)))
          (is (instance? DataObjectAO (:dataObjectAO cm)))
          (is (instance? UserAO (:userAO cm)))
          (is (instance? UserGroupAO (:userGroupAO cm)))
          (is (instance? IRODSFileFactory (:fileFactory cm)))
          (is (instance? IRODSFileSystemAO (:fileSystemAO cm)))
          (is (instance? CollectionAndDataObjectListAndSearchAO (:lister cm)))
          (is (instance? QuotaAO (:quotaAO cm)))
          (is (not @closed?)))
        (is @closed?)))
    (testing "closes when exception thrown"
      (let [closed? (atom false)]
        (ss/try+
          (with-jargon (->cfg closed?) [_]
            (ss/throw+ "an exception"))
          (is false)
          (catch Object _ (is @closed?)))))))
          

(deftest test-dataobject-readable?
  (is (true? (dataobject-readable? (mk-cm) "user" "/zone/home/user/file"))))
  

(deftest test-list-user-perms
  (is (ss/try+ 
        (list-user-perms (mk-cm) "/zone/")
        true
        (catch FileNotFoundException _ false))))
  
  
(deftest test-list-paths
  (let [cm (mk-cm)]
    (is (= ["/zone/home/"] (list-paths cm "/zone/")))
    (is (ss/try+
          (list-paths cm "/zone/home/user/")
          false
          (catch Object _ true)))
    (is (ss/try+
          (list-paths cm "/zone/home/user/" :ignore-child-exns)
          true
          (catch Object _ false)))))


(deftest test-is-file?
  (is (true? (is-file? (mk-cm) "/zone/home/user/file"))))

    
(deftest test-is-dir? 
  (is (false? (is-dir? (mk-cm) "/zone/home/user/file"))))

    
(deftest test-is-linked-dir?
  (let [cm (mk-cm)]
    (is (true? (is-linked-dir? cm "/zone/home/user/link")))
    (is (false? (is-linked-dir? cm "/zone")))
    (is (false? (is-linked-dir? cm "zone/home/user/file")))
    (is (false? (is-linked-dir? cm "/zone/missing")))))
    

(deftest test-user-exists?
  (is (true? (user-exists? (mk-cm) "user"))))


(deftest test-is-readable?
  (is (true? (is-readable? (mk-cm) "user" "/zone/home/user/file"))))
