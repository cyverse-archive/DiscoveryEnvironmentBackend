FROM centos:centos6
MAINTAINER John Wregglesworth <wregglej@gmail.com>

##RUN rpm -Uvh http://ftp.linux.ncsu.edu/pub/epel/5/i386/epel-release-5-4.noarch.rpm
RUN yum install -y git-core java-1.7.0-openjdk-devel java-1.7.0-openjdk mercurial curl wget ruby ruby-devel rubygems  gcc rpm-build gcc g++ make tar which
RUN mkdir -p /opt/go && \
    curl -O https://storage.googleapis.com/golang/go1.3.3.linux-amd64.tar.gz && \
    tar xzf go1.3.3.linux-amd64.tar.gz -C /opt/go --strip-components=1
RUN mkdir -p /opt/maven && \
    curl -O http://apache.mirrors.hoobly.com/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz && \
    tar xzf apache-maven-3.2.3-bin.tar.gz -C /opt/maven --strip-components=1
RUN mkdir -p /opt/nodejs && \
    curl -O http://nodejs.org/dist/v0.10.32/node-v0.10.32-linux-x64.tar.gz && \
    tar xzf node-v0.10.32-linux-x64.tar.gz -C /opt/nodejs --strip-components=1
RUN mkdir -p /opt/gopath
ENV GOPATH /opt/gopath
ENV GOROOT /opt/go
ENV LEIN_ROOT 1
ENV PATH /bin:/usr/bin:/usr/local/bin:/sbin/:/usr/sbin:/opt/go/bin:/opt/gopath/bin:/opt/maven/bin:/opt/nodejs/bin
RUN go get github.com/tools/godep
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
ADD build_profiles.clj /.lein/profiles.clj

RUN chmod a+x /usr/bin/lein
RUN gem install fpm
RUN npm install -g grunt-cli
RUN mkdir -p /usr/src/redhat/BUILD
RUN mkdir -p /usr/src/redhat/RPMS
RUN mkdir -p /usr/src/redhat/SOURCES
RUN mkdir -p /usr/src/redhat/SPECS
RUN mkdir -p /usr/src/redhat/SRPMS
