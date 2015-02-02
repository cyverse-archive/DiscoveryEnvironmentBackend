FROM tomcat:8

ADD belphegor.war /usr/local/tomcat/webapps/

RUN mkdir /usr/local/tomcat/webapps/belphegor/ && cd /usr/local/tomcat/webapps/belphegor/ && unzip -o /usr/local/tomcat/webapps/belphegor.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
