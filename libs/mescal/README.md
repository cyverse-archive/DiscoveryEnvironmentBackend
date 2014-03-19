# mescal

A Clojure library designed to provide a client library for the iPlant Foundation
API and, eventually, Agave. Currently, only the interface to the iPlant
Foundation API (labeled, "Agave version 1," in this library) is implemented.

## Usage

For general interactions:

```clojure
(use 'mescal.core)

;; Create the client.
(def agave (agave-client-v1 base-url proxy-user proxy-password user))

;; Lists information about all systems.
(.listSystems agave)

;; Lists information about all public apps.
(.listPublicApps agave)

;; Counts the number of public apps.
(.countPublicApps agave)

;; Lists the private apps for the current user.
(.listMyApps agave)

;; Counts the private apps for the current user.
(.countMyApps agave)

;; Obtains detailed information about a single app.
(.getApp agave app-id)

;; Submits a job.
(.submitJob agave params)     ;; App ID provided in `params`.
(.submitJob agave app params) ;; App JSON provided in `app` parameter.

;; Lists existing jobs for the current user.
(.listJob agave job-id)
(.listJobs agave)
(.listJobs agave job-ids)
```

For use by the DE:

```clojure
(use 'mescal.de)

;; Create the client.
(def agave
 (de-agave-client-v1 base-url proxy-user proxy-pass user jobs-enabled?
                     irods-home))

;; Lists high-level information about public apps.
(.publicAppGroup agave)

;; Lists public apps.
(.listPublicApps agave)

;; Obtains detailed information about a single app.
(.getApp agave app-id)

;; Retrieves a manufactured "deployed component" for an app.
(.getAppDeployedComponent agave app-id)

;; Obtains details about an app.
(.getAppDetails agave app-id)

;; Submits a job for execution.
(.submitJob agave job-submission)

;; Lists information about previously submitted jobs.
(.listJobs agave)
(.listJobs agave job-ids)
(.listRawJob agave job-id)
(.listJobIds agave)
(.getJobParams agave job-id)
(.getAppRerunInfo agave job-id)

;; Translates the status of a job to something the DE can use.
(.translateJobStatus agave status)
```

## License

http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
