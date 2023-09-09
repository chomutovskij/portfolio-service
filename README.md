# portfolio-service
- A service to track the performance of a single person's portfolio
- A user can have multiple orders for the same symbol, but we keep only 1 position for each symbol
- A group of symbols can be placed into a 'bucket'
- The service template used: [conjure-java-example](https://github.com/palantir/conjure-java-example)

## How to run locally
### Running the server
- clone the repo
- `cd portfolio-service`
- set `JAVA_HOME` to java 15
- `./gradlew run`

## APIs
### Available dates per symbol:
```
curl -X GET -H "Content-Type: application/json" "http://localhost:8346/api/v1/dates/all/GS" | jq
```

### Retrieving information about the device:
```
curl -X POST "http://localhost:8346/api/v1/management/create/Samsung%20Galaxy%20S5"
curl -X DELETE "http://localhost:8346/api/v1/management/delete/11"
curl -X DELETE "http://localhost:8346/api/v1/management/delete/all"
curl -X GET -H "Content-Type: application/json" "http://localhost:8346/api/v1/info/byname/Samsung%20Galaxy%20S9" | jq
curl -X GET -H "Content-Type: application/json" "http://localhost:8346/api/v1/info/byid/2" | jq
curl -X GET -H "Content-Type: application/json" "http://localhost:8346/api/v1/info/all" | jq
curl -X GET -H "Content-Type: application/json" "http://localhost:8346/api/v1/info/all/available" | jq
```

### Reserving and returning the device:
#### using the device name
```
curl -X PUT -H "Content-Type: application/json" -d '{"person": "Andrej", "deviceName": "Nokia 3310"}' "http://localhost:8346/api/v1/booking/reserve"
curl -X PUT -H "Content-Type: application/json" -d '{"person": "Andrej", "deviceName": "Nokia 3310"}' "http://localhost:8346/api/v1/booking/return"
```

#### using the device ID
```
curl -X PUT -H "Content-Type: application/json" -d '{"person": "Andrej", "deviceId": 10}' "http://localhost:8346/api/v1/booking/reserve"
curl -X PUT -H "Content-Type: application/json" -d '{"person": "Andrej", "deviceId": 10}' "http://localhost:8346/api/v1/booking/return" 
```

### Tools and Libraries

This service uses the following tools and libraries, please consult their respective documentation for more information.
* [conjure](https://github.com/palantir/conjure) - IDL for defining APIs once and generating client/server interfaces in different languages.
    * [conjure-java-runtime](https://github.com/palantir/conjure-java-runtime/) - conjure libraries for HTTP&JSON-based RPC using Retrofit, Feign, OkHttp as clients and Jetty/Jersey as servers
    * [conjure-java](https://github.com/palantir/conjure-java) - conjure generator for java clients and servers 
    * [conjure-typescript](https://github.com/palantir/conjure-typescript) - conjure generator for typescript clients
* [gradle](https://gradle.org/) - a highly flexible build tool. Some of the gradle plugins applied are:
     *  [gradle-conjure](https://github.com/palantir/gradle-conjure) - a gradle plugin that contains tasks to generate conjure bindings.
     *  [gradle-baseline](https://github.com/palantir/gradle-baseline) - a gradle plugin for configuring code quality tools in builds and projects.
* [dropwizard](https://www.dropwizard.io/en/stable/) - a simple framework for building web services

### Project Structure

* `portfolio-service-api` - a sub-project that defines portfolio-service APIs in Conjure and generates both java and typescript bindings.

    This is what the api project looks like:
    ```
    ├── portfolio-service-api
    │   ├── build.gradle
    │   ├── portfolio-service-api-jersey
    │   ├── portfolio-service-api-objects
    │   ├── portfolio-service-api-typescript
    │   └── src
    │       └── main
    │           └── conjure
    │               └── portfolio-service-api.yml
    ```
    * build.gradle - a gradle script that 
        1. configures sub-projects with needed dependencies to generate java bindings. e.g. `portfolio-service-api-jersey`
        2. configures `publishTypescript` task to generate `.npmrc` in the generated root folder, `portfolio-service-api-typescript/src` for publishing the generated npm module.
        3. modifies the `conjure` extension to specify the package name under which the npm module will be published.
    * portfolio-service-api-jersey - the sub-project where all generated [service interfaces](portfolio-service-api/src/main/conjure/portfolio-service-api.yml#L51) live.
    * portfolio-service-api-objects - the sub-project where all generated [object classes](portfolio-service-api/src/main/conjure/portfolio-service-api.yml#L4) live.
    * portfolio-service-api-typescript - the sub-project where all generated typescript bindings live.
    * src/main/conjure - directory containing conjure definition yml files where recipe APIs are defined, please refer to [specification.md](https://github.com/palantir/conjure/blob/develop/docs/specification.md) for more details.

* `portfolio-service-server` - a dropwizard application project that uses conjure generated jersey binding for resource class implementation

    This is what the server project looks like:
    ```
    ├── portfolio-service-server
    │   ├── build.gradle
    │   ├── src
    │   │   ├── main/java
    │   │   └── test/java
    │   └── var
    │       └── conf
    │           └── conf.yml
    ```
    * build.gradle - configures the project with needed dependencies and applies the `gradle-conjure` and `application plugins`, so we can run the server locally or in IDE.
    * src/main/java - source classes for the dropwizard application. e.g. RecipeBookResource.java class `implements` the generated Jersey interface.
    * test/main/java - test source classes for simple integration tests that uses generated jersey interface for client interaction.
    * var/conf/conf.yml - the dropwizard application configuration yml file

* build.gradle - the root level gradle script where a set of gradle plugins are configured, including [gradle-conjure](https://github.com/palantir/gradle-conjure).
* settings.gradle - the gradle settings file where all sub projects are configured.
* versions.props - a property file of the [nebula version recommender plugin](https://github.com/nebula-plugins/nebula-dependency-recommender-plugin) with which we can specify versions of project dependencies, including conjure generators.

## Development

### Useful Gradle Commands:

* `./gradlew tasks` for tasks available in this project.
* `./gradlew idea` for IntelliJ
* `./gradlew eclipse` for Eclipse
* `./gradlew run` for running the server or use IDE to debug it

## Certificates
```
/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\
/!\ Please do not use these certificates in prod!  /!\
/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\
```
The certificates presented with this server are an example so that the tests run in HTTPs. These were generated by following the steps below. All passwords are "changeit"

```
keytool -genkey -alias bmc -keyalg RSA -keystore keystore.jks -keysize 2048 -dname "CN=localhost,OU=AQ,O=AQ,C=AQ" -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -validity 3650

openssl req -new -x509 -keyout ca-key -out ca-cert

keytool -keystore KeyStore.jks -alias bmc -certreq -file cert-file

openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:changeit

keytool -keystore KeyStore.jks -alias CARoot -import -file ca-cert

keytool -keystore KeyStore.jks -alias bmc -import -file cert-signed

keytool -keystore truststore.jks -alias bmc -import -file ca-cert
```
