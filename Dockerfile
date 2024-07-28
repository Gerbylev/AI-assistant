FROM openjdk:17-slim

ENTRYPOINT ["sh", "/opt/cc-back/bin/docker-run.sh"]

VOLUME /opt/cc/cc-back/data
VOLUME /opt/cc/cc-back/index

VOLUME /etc/cc-back
VOLUME /var/log/cc-back

ADD target/cc-back         /opt/cc-back

