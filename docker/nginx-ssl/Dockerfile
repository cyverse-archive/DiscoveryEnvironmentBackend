FROM nginx

#Adapted from https://www.digitalocean.com/community/tutorials/how-to-create-an-ssl-certificate-on-nginx-for-ubuntu-14-04
#Also adapted from http://superuser.com/a/226229
COPY nginx.conf /etc/nginx/nginx.conf
RUN mkdir -p /etc/nginx/ssl
RUN openssl req -new -x509 -nodes -days 365 -newkey rsa:4096 -subj "/C=US/ST=NotReal/L=ReallyNotReal/O=Rly/CN=NotAServer" -keyout /etc/nginx/ssl/nginx.key -out /etc/nginx/ssl/nginx.crt
