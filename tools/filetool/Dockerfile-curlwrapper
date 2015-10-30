FROM alpine

RUN apk add --update \
  curl \
  perl \
  && rm -rf /var/cache/apk/*

ADD curl_wrapper.pl /bin/curl_wrapper.pl

ENTRYPOINT ["curl_wrapper.pl"]

CMD ["-h"]
