FROM centos:centos6
MAINTAINER John Wregglesworth <wregglej@gmail.com>

##RUN rpm -Uvh http://ftp.linux.ncsu.edu/pub/epel/5/i386/epel-release-5-4.noarch.rpm
RUN yum install -y git-core java-1.7.0-openjdk-devel java-1.7.0-openjdk mercurial curl wget ruby ruby-devel rubygems  gcc rpm-build gcc g++ make tar
RUN mkdir -p /opt/go && \
    curl -O https://storage.googleapis.com/golang/go1.3.3.linux-amd64.tar.gz && \
    tar xzf go1.3.3.linux-amd64.tar.gz -C /opt/go --strip-components=1
RUN mkdir -p /opt/gopath
ENV GOPATH /opt/gopath
ENV GOROOT /opt/go
ENV PATH /bin:/usr/bin:/usr/local/bin:/sbin/:/usr/sbin:/opt/go/bin:/opt/gopath/bin
RUN go get github.com/tools/godep
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod a+x /usr/bin/lein
RUN gem install fpm
