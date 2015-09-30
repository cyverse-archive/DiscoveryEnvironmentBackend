(ns clojure-commons.progress
  (:require [clj-time.core :as ct])
  (:import [clojure.lang IFn]
           [org.joda.time PeriodType Interval]
           [org.joda.time.format PeriodFormatterBuilder PeriodFormatter]))


(def ^:private ^PeriodFormatter period-formatter
  (-> (PeriodFormatterBuilder.)
      (.appendDays)
      (.appendSuffix " day", " days")
      (.appendSeparator ", ")
      (.appendHours)
      (.appendSuffix " hour", " hours")
      (.appendSeparator ", ")
      (.appendMinutes)
      (.appendSuffix " minute", " minutes")
      (.appendSeparator ", ")
      (.printZeroRarelyLast)
      (.appendSeconds)
      (.appendSuffix " second", " seconds")
      (.toFormatter)))


(defn- create-notifier
  [notify notify-step transform]
  (let [idx-start    (ref (ct/now))
        idx-count    (ref 0)
        notify-count (ref notify-step)
        wholeSeconds (.withMillisRemoved (PeriodType/standard))
        get-interval (fn [] (.print period-formatter
                                    (.toPeriod (ct/interval @idx-start (ct/now)) wholeSeconds)))]
    (fn [entries]
      (let [r (transform entries)]
        (dosync
          (let [c (alter idx-count (partial + (count entries)))]
            (when (>= c @notify-count)
              (notify (str "over " @notify-count " processed in " (get-interval)))
              (alter notify-count (partial + notify-step)))))
        r))))


(defn ^IFn notifier
  "This function wraps a given function intended for sequence mapping with a monad that writes
   progress messages using another given function.

   Parameters:
     notifications-enabled? - a flag indicating whether or not to write progress messages
     notify                 - a function used to write progress messages to. It must accept a string
                              as its only parameter.
     notify-step            - the number of sequence elements to pass to the transform before
                              writing a progress message.
     transform              - Optional. This is the sequence mapping function. It must accept a
                              sequence as its only argument.  If it is not provided, identity will
                              be used.

   Returns:
     It returns a function that accepts a sequence as its only argument. It returns whatever
     transform returns."
  ([^Boolean notifications-enabled? ^IFn notify ^Integer notify-step]
   (notifier notifications-enabled? notify notify-step identity))

  ([^Boolean notifications-enabled? ^IFn notify ^Integer notify-step ^IFn transform]
   (if notifications-enabled?
     (create-notifier notify notify-step transform)
     transform)))
