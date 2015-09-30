(ns clj-jargon.cart
  (:use [clj-jargon.validations])
  (:import [org.irods.jargon.datautils.shoppingcart FileShoppingCart]
           [org.irods.jargon.datautils.shoppingcart ShoppingCartEntry]
           [org.irods.jargon.datautils.shoppingcart ShoppingCartService]
           [org.irods.jargon.datautils.shoppingcart ShoppingCartServiceImpl]
           [org.irods.jargon.datautils.datacache DataCacheServiceFactoryImpl]
           [org.irods.jargon.core.pub UserAO]))

(defn ^FileShoppingCart shopping-cart
  [filepaths]
  (doseq [fp filepaths] (validate-path-lengths fp))

  (let [cart (FileShoppingCart/instance)]
    (loop [fps filepaths]
      (.addAnItem cart (ShoppingCartEntry/instance (first fps)))
      (if (pos? (count (rest fps)))
        (recur (rest fps))))
    cart))

(defn ^String temp-password
  [{^UserAO user-ao :userAO} user]
  (.getTemporaryPasswordForASpecifiedUser user-ao user))

(defn ^ShoppingCartService cart-service
  [{ao-factory :accessObjectFactory irods-account :irodsAccount}]
  (ShoppingCartServiceImpl. ao-factory irods-account
    (DataCacheServiceFactoryImpl. ao-factory)))

(defn ^String store-cart
  [cm user cart-key filepaths]
  (doseq [fp filepaths] (validate-path-lengths fp))
  (.serializeShoppingCartAsSpecifiedUser
    (cart-service cm)
    (shopping-cart filepaths)
    cart-key
    user))
