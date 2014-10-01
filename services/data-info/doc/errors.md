# Errors

If a service call causes an exception that is not caught by the service itself
then data-info will respond with a standardized error message:

```json
{
    "success": false,
    "reason": reason-for-error
}
```

The HTTP status code that is returned will either be a 400 or a 500, depending
on which type of exception is caught.  In either case, the reason for the
error should be examined.  If the logging level is set to _error_ or lower
then the exception will be logged in data-info's log file along with a stack
trace.  This can be helpful in cases where the true cause of the error isn't
obvious at first.
