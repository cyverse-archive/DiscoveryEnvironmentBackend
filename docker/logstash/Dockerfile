FROM logstash:1.5.2

RUN /opt/logstash/bin/plugin install logstash-filter-elasticsearch

# Download GeoIP Database
ADD http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz /usr/share/GeoIP/GeoIP.dat.gz
ADD http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz /usr/share/GeoIP/GeoLiteCity.dat.gz
RUN cd /usr/share/GeoIP && \
    zcat -f GeoIP.dat.gz > GeoIP.dat && \
        zcat -f GeoLiteCity.dat.gz > GeoIPCity.dat

