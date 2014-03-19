# Table of Contents

* [Updated Application Metadata Endpoints](#updated-application-metadata-endpoints)
    * [Updating or Importing a Single-Step App](#updating-or-importing-a-single-step-app)
    * [Obtaining an App Representation for Editing](#obtaining-an-app-representation-for-editing)
    * [Obtaining App Information for Job Submission](#obtaining-app-information-for-job-submission)
    * [Previewing Command Line Arguments](#previewing-command-line-arguments)

# Updated Application Metadata Endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Updating or Importing a Single-Step App

*Secured Endpoint:* POST /secured/update-app

*Delegates to metadactyl:* POST /secured/update-app

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Obtaining an App Representation for Editing

*Secured Endpoint:* GET /secured/edit-app/{app-id}

*Delegates to metadactyl:* GET /secured/edit-app/{app-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Obtaining App Information for Job Submission

*Secured Endpoint:* GET /secured/app/{app-id}

*Delegates to metadactyl:* GET /secured/app/{app-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Previewing Command Line Arguments

*Unsecured Endpoint:* POST /arg-preview

*Delegates to metadactyl:* POST /arg-preview

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.
