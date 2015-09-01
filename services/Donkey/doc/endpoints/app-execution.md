# Table of Contents

* [Application Execution Endpoints](#application-execution-endpoints)
    * [Obtaining Parameter Values for a Previously Executed Job](#obtaining-parameter-values-for-a-previously-executed-job)
    * [Obtaining Information to Rerun a Job](#obtaining-information-to-rerun-a-job)
    * [Submitting a Job for Execution](#submitting-a-job-for-execution)
    * [Listing Jobs](#listing-jobs)
    * [Deleting a Job](#deleting-a-job)
    * [Deleting Multiple Jobs](#deleting-multiple-jobs)
    * [Updating Analysis Information](#updating-analysis-information)
    * [Listing Analysis Steps](#listing-analysis-steps)
    * [Stopping a Running Analysis](#stopping-a-running-analysis)

# Application Execution Endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Obtaining Parameter Values for a Previously Executed Job

Secured Endpoint: GET /analyses/{analysis-id}/parameters

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

## Obtaining Information to Rerun a Job

*Secured Endpoint:* GET /analyses/{analysis-id}/relaunch-info

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

## Submitting a Job for Execution

Secured Endpoint: POST /analyses

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

## Listing Jobs

*Secured Endpoint:* GET /analyses

This service forwards all requests to metadactyl. Please see the metadactyl
documentation for more details.

## Deleting a Job

*Secured Endpoint:* DELETE /analyses/{analysis-id}

This service forwards all requests to metadactyl. Please see the metadactyl
documentation for more details.

## Deleting Multiple Jobs

*Secured Endpoint:* POST /analyses/shredder

This service forwards all requests to metadactyl. Please see the metadactyl
documentation for more details.

## Updating Analysis Information

*Secured Endpoint:* PATCH /analyses/{analysis-id}

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

## Listing Analysis Steps

*Secured Endpoint:* GET /analyses/{analysis-id}/steps

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

## Stopping a Running Analysis

*Secured Endpoint:* POST /analyses/{analysis-id}/stop

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.
