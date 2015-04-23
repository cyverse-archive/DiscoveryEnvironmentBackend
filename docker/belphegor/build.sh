#!/bin/sh
curl -O https://everdene.iplantcollaborative.org/jenkins/job/DiscoveryEnvironment_UI_Beta/lastSuccessfulBuild/artifact/applications/belphegor/build/libs/wars/belphegor.war
docker build -t $DOCKER_USER/belphegor:dev .
docker push $DOCKER_USER/belphegor:dev
