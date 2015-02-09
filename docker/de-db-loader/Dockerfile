FROM discoenv/facepalm

ADD setup-dev-database.sh /setup-dev-database.sh
ADD update-dev-database.sh /update-dev-database.sh
ADD https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend/releases/download/1.9.4-RELEASE/database.tar.gz /database.tar.gz

CMD ["/setup-dev-database.sh"]