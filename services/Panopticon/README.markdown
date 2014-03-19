# Panopticon

A basic monitoring system for Condor. It alternates between calling condor_q and condor_history and updates the OSM with any analysis state changes that it detects.

Panopticon assumes that it is running as a user that can run the condor_q and condor_history commands. Additionally, Panopticon requires that Porklock be installed on the same box.

#Configuration Template

    panopticon.osm.url               = $osm_base$
    panopticon.osm.collection        = $osm_jobs_bucket$
    panopticon.condor.condor-config  = /etc/condor/condor_config
    panopticon.condor.condor-q       = /usr/bin/condor_q
    panopticon.condor.condor-history = /usr/bin/condor_history
    panopticon.app.num-instances     = 4

    #How long to sleep (in milliseconds) between iterations.
    panopticon.app.sleep-duration = $panopticon_sleep_time$

panopticon.osm.url - Setting that provides the base URL for the OSM. Should include port.

panopticon.osm.collection - Sets the bucket in the OSM that Panopticon reads job information from.

panopticon.condor.condor-config  - Sets the path to the config_config file.

panopticon.condor.condor-q - Sets the path to the condor_q executable.

panopticon.condor.condor-history - Sets the path to the condor_history executable.

panopticon.app.num-instances - Vestigial setting. Used to set the number of threads that panopticon used.

panopticon.app.sleep-duration - Read the comment above the setting.

See the iPlant Wiki for more information.


