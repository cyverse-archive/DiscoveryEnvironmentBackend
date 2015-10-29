FROM alpine:3.2

ADD https://circle-artifacts.com/gh/andyshinn/alpine-pkg-glibc/6/artifacts/0/home/ubuntu/alpine-pkg-glibc/packages/x86_64/glibc-2.21-r2.apk /tmp/
RUN apk --update add ca-certificates && apk add --allow-untrusted /tmp/glibc-2.21-r2.apk

COPY ./bin/* /bin/
CMD [ "/bin/sh" ]
