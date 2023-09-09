FROM openjdk:15

RUN set -eux; \
    mkdir -p /opt/palantir/services;

ADD portfolio-service-server/ /opt/palantir/services/portfolio-service-server

RUN chmod +x /opt/palantir/services/portfolio-service-server/bin/portfolio-service-server

EXPOSE 8345

WORKDIR /opt/palantir/services/portfolio-service-server/
ENTRYPOINT ["./bin/portfolio-service-server", "server", "var/conf/conf.yml"]
