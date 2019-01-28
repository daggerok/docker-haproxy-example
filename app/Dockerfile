FROM openjdk:8u191-jre-alpine3.8
LABEL MAINTAINER='Maksim Kostromin https://github.com/daggerok'
RUN apk add --no-cache --update bash curl sudo busybox-suid openssh-client shadow \
 && adduser -h /home/appuser -s /bin/bash -D -u 1025 appuser wheel \
 && echo 'appuser ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers \
 && sed -i 's/.*requiretty$/Defaults !requiretty/' /etc/sudoers \
 && apk del --no-cache busybox-suid openssh-client shadow \
 && rm -rf /var/cache/apk/* /tmp/*
USER appuser
WORKDIR /home/appuser
VOLUME /home/appuser
ENTRYPOINT java -Djava.net.preferIPv4Stack=true \
                -XX:+UnlockExperimentalVMOptions \
                -XX:+UseCGroupMemoryLimitForHeap \
                -XshowSettings:vm \
                -jar ./app.jar
CMD /bin/bash
EXPOSE 8080
HEALTHCHECK --timeout=10s \
            CMD curl -f http://127.0.0.1:8080/ || exit 1
COPY --chown=appuser ./target/*.jar ./app.jar
