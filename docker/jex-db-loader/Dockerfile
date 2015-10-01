FROM discoenv/facepalm:dev

COPY setup-dev-database.sh /bin/setup-dev-database.sh
COPY update-dev-database.sh /bin/update-dev-database.sh

ENTRYPOINT ["/bin/bash"]

CMD ["/bin/setup-dev-database.sh"]
