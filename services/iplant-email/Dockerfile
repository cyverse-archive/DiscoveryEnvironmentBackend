FROM discoenv/javabase

ADD target/iplant-email-standalone.jar /home/iplant/
ADD conf/* /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:iplant-email-standalone.jar", "iplant_email.core"]
CMD ["--help"]
