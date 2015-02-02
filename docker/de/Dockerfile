FROM tomcat:8

ADD de.war /usr/local/tomcat/webapps/

RUN mkdir /usr/local/tomcat/webapps/de/ && cd /usr/local/tomcat/webapps/de/ && unzip -o /usr/local/tomcat/webapps/de.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
