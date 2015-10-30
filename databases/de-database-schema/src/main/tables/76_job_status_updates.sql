SET search_path = public, pg_catalog;

--
-- Records job status updates
--
CREATE TABLE job_status_updates (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),

    -- corresponds to the external_id field in the job_steps table
    external_id character varying(64) NOT NULL,

    -- the update message. needs to be freeform since we can't guarantee
    -- the length
    message text NOT NULL,

    -- The actual status field associated with this update. again, we can't
    -- guarantee the format from external job sites, so this ends up being
    -- a text field
    status text NOT NULL,

    -- The ip address that the update was sent from. This should be set to the
    -- condor node ip address or to the agave callback source ip.
    sent_from inet NOT NULL,

    -- Send date is the date that the update was actually sent, not the date
    -- that the record was created.
    sent_on timestamp NOT NULL,

    -- The date that the record was created by whichever service adds the record.
    -- This is not the date that the update was sent.
    created_date timestamp NOT NULL DEFAULT now()
)
