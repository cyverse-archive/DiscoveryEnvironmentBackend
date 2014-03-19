(ns clj-jargon.cart
  (:use [clj-jargon.validations])
  (:import [org.irods.jargon.datautils.shoppingcart FileShoppingCart]
           [org.irods.jargon.datautils.shoppingcart ShoppingCartEntry]
           [org.irods.jargon.datautils.shoppingcart ShoppingCartServiceImpl]
           [org.irods.jargon.datautils.datacache DataCacheServiceFactoryImpl]))

(defn shopping-cart
  [filepaths]
  (doseq [fp filepaths] (validate-path-lengths fp))

  (let [cart (FileShoppingCart/instance)]
    (loop [fps filepaths]
      (.addAnItem cart (ShoppingCartEntry/instance (first fps)))
      (if (pos? (count (rest fps)))
        (recur (rest fps))))
    cart))

(defn temp-password
  [cm user]
  (.getTemporaryPasswordForASpecifiedUser (:userAO cm) user))

(defn cart-service
  [cm]
  (ShoppingCartServiceImpl.
    (:accessObjectFactory cm)
    (:irodsAccount cm)
    (DataCacheServiceFactoryImpl. (:accessObjectFactory cm))))

(defn store-cart
  [cm user cart-key filepaths]
  (doseq [fp filepaths] (validate-path-lengths fp))
  (.serializeShoppingCartAsSpecifiedUser
    (cart-service cm)
    (shopping-cart filepaths)
    cart-key
    user))
