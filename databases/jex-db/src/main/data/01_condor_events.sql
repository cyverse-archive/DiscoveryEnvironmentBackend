INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('000', 'Job submitted', 'This event occurs when a user submits a job. It is the first event you will see for a job, and it should only occur once.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('001', 'Job executing', 'This shows up when a job is running. It might occur more than once.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('002', 'Error in executable', 'The job could not be run because the executable was bad.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('003', 'Job was checkpointed', 'The job''s complete state was written to a checkpoint file. This might happen without the job being removed from a machine, because the checkpointing can happen periodically.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('004', 'Job evicted from machine', 'A job was removed from a machine before it finished, usually for a policy reason. Perhaps an interactive user has claimed the computer, or perhaps another job is higher priority.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('005', 'Job terminated', 'The job has completed.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('006', 'Image size of job updated', 'An informational event, to update the amount of memory that the job is using while running. It does not reflect the state of the job.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('007', 'Shadow exception', 'The condor_shadow, a program on the submit computer that watches over the job and performs some services for the job, failed for some catastrophic reason. The job will leave the machine and go back into the queue.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('008', 'Generic log event', 'Not used.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('009', 'Job aborted', 'The user canceled the job.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('010', 'Job was suspended', 'The job is still on the computer, but it is no longer executing. This is usually for a policy reason, such as an interactive user using the computer.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('011', 'Job was unsuspended', 'The job has resumed execution, after being suspended earlier.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('012', 'Job was held', 'The job has transitioned to the hold state. This might happen if the user applies the condor_hold command to the job.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('013', 'Job was released', 'The job was in the hold state and is to be re-run.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('014', 'Parallel node executed', 'A parallel universe program is running on a node.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('015', 'Parallel node terminated', 'A parallel universe program has completed on a node.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('016', 'POST script terminated', 'A node in a DAGMan work flow has a script that should be run after a job. The script is run on the submit host. This event signals that the post script has completed.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('017', 'Job submitted to Globus', 'A grid job has been delegated to Globus (version 2, 3, or 4). This event is no longer used.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('018', 'Globus submit failed', 'The attempt to delegate a job to Globus failed.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('019', 'Globus resource up', 'The Globus resource that a job wants to run on was unavailable, but is now available. This event is no longer used.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('020', 'Detected Down Globus Resource', 'The Globus resource that a job wants to run on has become unavailable. This event is no longer used.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('021', 'Remote error', 'The condor_starter (which monitors the job on the execution machine) has failed.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('022', 'Remote system call socket lost', 'The condor_shadow and condor_starter (which communicate while the job runs) have lost contact.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('023', 'Remote system call socket reestablished', 'The condor_shadow and condor_starter (which communicate while the job runs) have been able to resume contact before the job lease expired.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('024', 'Remote system call reconnect failure', 'The condor_shadow and condor_starter (which communicate while the job runs) were unable to resume contact before the job lease expired.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('025', 'Grid Resource Back Up', 'A grid resource that was previously unavailable is now available.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('026', 'Detected Down Grid Resource', 'The grid resource that a job is to run on is unavailable.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('027', 'Job submitted to grid resource', 'A job has been submitted, and is under the auspices of the grid resource.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('028', 'Job ad information event triggered.', 'Extra job ClassAd attributes are noted. This event is written as a supplement to other events when the configuration parameter EVENT_LOG_JOB_AD_INFORMATION_ATTRS is set.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('029', 'The job''s remote status is unknown', 'No updates of the job''s remote status have been received for 15 minutes.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('030', 'The job''s remote status is known again', 'An update has been received for a job whose remote status was previous logged as unknown.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('031', 'Job stage in', 'A grid universe job is doing the stage in of input files.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('032', 'Job stage out', 'A grid universe job is doing the stage out of output files.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('033', 'Job ClassAd attribute update', 'A Job ClassAd attribute is changed due to action by the condor_schedd daemon. This includes changes by condor_prio.');

INSERT INTO condor_events (event_number, event_name, event_desc)
  VALUES ('034', 'Pre Skip event', 'For DAGMan, this event is logged if a PRE SCRIPT exits with the defined PRE_SKIP value in the DAG input file. This makes it possible for DAGMan to do recovery in a workflow that has such an event, as it would otherwise not have any event for the DAGMan node to which the script belongs, and in recovery, DAGMan''s internal tables would become corrupted.');
