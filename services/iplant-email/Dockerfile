FROM discoenv/javabase

ADD target/iplant-email-standalone.jar /home/iplant/
ADD conf/* /home/iplant/
ENTRYPOINT ["java", "-cp", ".:iplant-email-standalone.jar", "iplant_email.core"]
CMD ["--help"]
