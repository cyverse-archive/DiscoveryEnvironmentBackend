# Dingle

Some basic process automation for the iPlant QA Drops. You will need the following to do anything interesting:

* Commit access to the git repos.
* Passwordless SSH access to the dev machines.
* sudo access on the dev machines.
* Jenkins tokens for the jobs you want to trigger.

This project is meant to be used from Leiningen's REPL. 


## Setup

Create a file at ~/.dingle/config.clj. It should look like the following

```
(ns dingle.config)

(def config
  (hash-map
    :ssh-user "<your ssh user>"
    :ssh-password "<your ssh password>"
    :sudo-password "<your sudo password>"
    :github-base-url "git@github.com:iPlantCollaborativeOpenSource/"
    
    :jenkins-url "<your jenkins url>"
    :jenkins-token "<your jenkins token>"

    :rpm-host "<hostname of machine hosting rpms>"
    :rpm-host-port <integer port to connect to rpm-host on>
    :rpm-host-user "<your rpm-host user>"
    :rpm-base-dir "<base directory containing rpm repos>"
    :rpm-dev-dir "<relative path to the dev repo>"
    :rpm-qa-dir "<relative path to the qa repo>"
    :rpm-stage-dir "<relative path to the stage repo>"
    :rpm-prod-dir "<relative path to the prod repo>"

    :yum-host "host-subscribed-to-repos"
    :yum-port "22"
    :yum-user "a-user"

    :yum-dev-repo "dev"
    :yum-qa-repo "qa"
    :yum-uat-repo "stage"
    :yum-prod-repo "prod"
  
    :rpm-names ["conrad"
                "donkey"
                "metadactyl"
                "porklock"
                "iplant-email"
                "iplant-service-config"
                "jex"
                "iplant-ncl"
                "nibblonian"
                "notificationagent"
                "osm"
                "panopticon"
                "scruffian"
                "iplant-zoidberg"
                "iplant-clavin"
                "facepalm"]
    
    :prereq-repos  ["clj-jargon.git"
                    "iplant-clojure-commons.git"]
    
    :prereq-jobs   ["clj-jargon"
                    "iplant-clojure-commons"]
    
    :list-of-repos ["iplant-clojure-commons.git"
                    "clj-jargon.git"
                    "Nibblonian.git"
                    "facepalm.git"
                    "OSM.git"
                    "metadactyl-clj.git"
                    "iplant-email.git"
                    "JEX.git"
                    "Panopticon.git"
                    "filetool.git"
                    "Scruffian.git"
                    "Donkey.git"
                    "Conrad.git"]
    
    :list-of-services ["nibblonian"
                       "metadactyl"
                       "scruffian"
                       "panopticon"
                       "donkey"
                       "jex"
                       "notificationagent"
                       "osm"
                       "iplant-email"
                       "conrad"]))
```

Substitute the values for your accounts in the above as appropriate. Do *NOT* check them in to a public github repo.

## Doing stuff

Assuming you've got the config file in place and Dingle checked out, run 'lein repl' from the top level Dingle directory. The first thing you should do is run '(configure)'.

Here's an example workflow:

```
(configure)
(merge-and-tag-prereqs "a-git-tag-string")
(build-prereqs)
(merge-and-tag-repos "a-git-tag-string")
```

Right now, none of the functions that interact with Jenkins are blocking, so you'll have to watch Jenkins to tell when builds are complete. This will be fixed in the future, most likely.

Major workflow functions are located in the dingle.core namespace and should have doc-strings describing them. Use (doc) from leiningen's REPL judiciously.
