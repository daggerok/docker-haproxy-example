FROM haproxy:1.7
HEALTHCHECK CMD true
RUN apt update -yqq \
 && apt install -yqq --no-install-recommends iputils-ping curl
COPY ./haproxy.cfg /usr/local/etc/haproxy/haproxy.cfg
