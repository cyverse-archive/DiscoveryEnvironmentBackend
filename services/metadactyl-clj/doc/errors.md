# Errors

If a service call encounters an error, then an HTTP status code between 400 - 500 will be returned,
depending on which type of exception is caught. In any case, the reason for the error should be
examined. If the logging level is set to _error_ or lower then the exception will be logged in
metadactyl-clj's log file along with a stack trace. This can be helpful in cases where the true
cause of the error isn't obvious at first.
