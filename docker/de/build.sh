#!/bin/sh
curl -O https://everdene.iplantcollaborative.org/jenkins/job/DiscoveryEnvironment_UI_Dev/lastSuccessfulBuild/artifact/applications/de-webapp/build/libs/wars/de.war
docker build -t discoenv/de .
docker push discoenv/de
