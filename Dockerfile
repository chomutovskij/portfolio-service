FROM openjdk:15

RUN microdnf install findutils

RUN mkdir /portfolio-service

COPY . portfolio-service

EXPOSE 8345 8346

WORKDIR /portfolio-service

RUN ./gradlew build -x:portfolio-service-server:test

CMD ./gradlew run -x:portfolio-service-server:test
