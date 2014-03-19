(ns infosquito.exceptions
  "This namespace includes exception handling logic.")


(defn fmt-throw-context
  "Formats a slingshot throw context for display or logging.  It displays the root cause and the
   stack trace.

   Parameters:
     throw-context - The slingshot throw context.

   Returns:
     It returns the context formatted as a string."
  [{:keys [object stack-trace]}]
  (reduce #(str %1 \newline \tab "at " %2) (str "unexpected error - " object) stack-trace))
