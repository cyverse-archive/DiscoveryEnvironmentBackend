(ns metadactyl.user)

(def
  ^{:doc "The authenticated user or nil if the service is unsecured."
    :dynamic true}
   current-user nil)
