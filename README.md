# portfolio-service
- A service to track the performance of a single person's portfolio
- The P/L and average price are calculated using the same method as described in [this Robinhood article](https://robinhood.com/us/en/support/articles/average-cost/)
- A user can have multiple orders for the same symbol, but we keep only 1 position for each symbol (e.g. IBKR does not allow multiple positions for the same symbol, they get collapsed into a single position)
- A group of symbols can be placed into a 'bucket'
- The service template used: [conjure-java-example](https://github.com/palantir/conjure-java-example)

## How to run locally
### Running the server with docker:
- docker image is available at [docker hub](https://hub.docker.com/r/chomutovskij/portfolio-service-server/tags)
- run the below 2 commands in the terminal:
  - `docker pull chomutovskij/portfolio-service-server:<version>`
  - `docker run -p 8345:8345 -p 8346:8346 chomutovskij/portfolio-service-server:<version>`
- in a separate terminal window, run the [curls](#apis), but first read the note on certificates just below

### Note on certificates
- The certificates are self-signed, which means you will have to pass in `-k` in the below curl commands (if you are calling the service via `https`)
- If you don't want to pass in `-k`, you can call the service via `http` (change the port number from `8345` to `8346`)
  - e.g. `curl -X GET -H "Content-Type: application/json" "http://localhost:8346/api/v1/dates/all/NVDA" | jq`
- For more information on how these certificates were generated, refer to the [certificates section](#certificates)

## APIs
### Available dates for a symbol (needed for [order request](#submit-a-historical-order-order-that-happened-in-the-past)):
```
curl -k -X GET -H "Content-Type: application/json" "https://localhost:8345/api/v1/dates/all/NVDA" | jq
```

### Managing the buckets:
#### create a new empty bucket (optional, bucket can also be created when an [order is submitted](#submit-a-historical-order-order-that-happened-in-the-past))
```
curl -k -X POST "https://localhost:8345/api/v1/buckets/create/BucketA"
```

#### list all buckets and their contents (without calculating the P/L)
```
curl -k -X GET -H "Content-Type: application/json" "https://localhost:8345/api/v1/buckets/all" | jq
```

#### delete a bucket (note that the positions won't be closed)
```
curl -k -X DELETE -H "Content-Type: application/json" "https://localhost:8345/api/v1/buckets/delete/BucketA"
```

### Managing positions:
#### submit a historical order (order that happened in the past)
```
curl -k -X POST -H "Content-Type: application/json" -d '{"type": "SELL", "symbol": "AMZN", "quantity": 3, "date": "2023-08-29T00:00:00Z", "buckets": ["BucketA"]}' "https://localhost:8345/api/v1/position/add"

curl -k -X POST -H "Content-Type: application/json" -d '{"type": "BUY", "symbol": "NVDA", "quantity": 10, "date": "2023-08-29T00:00:00Z", "buckets": ["BucketB"]}' "https://localhost:8345/api/v1/position/add"

curl -k -X POST -H "Content-Type: application/json" -d '{"type": "BUY", "symbol": "TSLA", "quantity": 5, "date": "2023-08-29T00:00:00Z", "buckets": []}' "https://localhost:8345/api/v1/position/add"
```

#### retrieve information about the position (single symbol holding)
```
curl -k -X GET -H "Content-Type: application/json" "https://localhost:8345/api/v1/position/stock?symbol=NVDA" | jq
```

#### retrieve information about the bucket (a collection of symbol holdings)
```
curl -k -X GET -H "Content-Type: application/json" "https://localhost:8345/api/v1/position/bucket?name=BucketB" | jq
```

#### add an existing position to one or more buckets (will create the buckets if needed)
```
curl -k -X PUT -H "Content-Type: application/json" -d '{"symbol": "NVDA", "buckets": ["BucketA"]}' "https://localhost:8345/api/v1/position/add_to_buckets"
```

#### remove an existing position from one or more buckets
```
curl -k -X PUT -H "Content-Type: application/json" -d '{"symbol": "NVDA", "buckets": ["BucketB"]}' "https://localhost:8345/api/v1/position/remove_from_buckets"
```

## Tools used for the service and repo structure

### Tools and Libraries

This service uses the following tools and libraries, please consult their respective documentation for more information.
* [conjure](https://github.com/palantir/conjure) - IDL for defining APIs once and generating client/server interfaces in different languages.
  * [conjure-java-runtime](https://github.com/palantir/conjure-java-runtime/) - conjure libraries for HTTP&JSON-based RPC using Retrofit, Feign, OkHttp as clients and Jetty/Jersey as servers
  * [conjure-java](https://github.com/palantir/conjure-java) - conjure generator for java clients and servers
  * [conjure-typescript](https://github.com/palantir/conjure-typescript) - conjure generator for typescript clients
* [gradle](https://gradle.org/) - a highly flexible build tool. Some of the gradle plugins applied are:
  *  [gradle-conjure](https://github.com/palantir/gradle-conjure) - a gradle plugin that contains tasks to generate conjure bindings.
  *  [gradle-baseline](https://github.com/palantir/gradle-baseline) - a gradle plugin for configuring code quality tools in builds and projects.
* [undertow](https://undertow.io/) - a simple framework for building web services

### Project Structure
* `portfolio-service-api` - a sub-project that defines portfolio-service APIs in Conjure and generates both java and typescript bindings.

    This is what the api project looks like:
    ```
    ├── portfolio-service-api
    │   ├── build.gradle
    │   ├── portfolio-service-api-dialogue
    │   ├── portfolio-service-api-objects
    │   ├── portfolio-service-api-typescript
    │   ├── portfolio-service-api-undertow
    │   └── src
    │       └── main
    │           └── conjure
    │               └── portfolio-service-api.yml
    ```
  * `build.gradle` - a gradle script that
    1. configures sub-projects with needed dependencies to generate java bindings. e.g. `portfolio-service-api-dialogue`
    2. configures `publishTypescript` task to generate `.npmrc` in the generated root folder, `portfolio-service-api-typescript/src` for publishing the generated npm module.
    3. modifies the `conjure` extension to specify the package name under which the npm module will be published.
  * `portfolio-service-api-dialogue` - the sub-project where all generated [service interfaces](portfolio-service-api/src/main/conjure/portfolio-service-api.yml#L93) live.
  * `portfolio-service-api-objects` - the sub-project where all generated [object classes](portfolio-service-api/src/main/conjure/portfolio-service-api.yml#L4) live.
  * `portfolio-service-api-typescript` - the sub-project where all generated typescript bindings live.
  * `src/main/conjure` - directory containing conjure definition yml files where recipe APIs are defined, please refer to [specification.md](https://github.com/palantir/conjure/blob/develop/docs/specification.md) for more details.

* `portfolio-service-server` - an Undertow application project that uses conjure generated Undertow binding for resource class implementation

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
  * `build.gradle` - configures the project with needed dependencies and applies the `gradle-conjure` and `application plugins`, so we can run the server locally or in IDE.
  * `src/main/java` - source classes for the Undertow application. e.g. `portfolioBookingResource.java` class `implements` the generated Undertow interface.
  * `test/main/java` - test source classes for simple integration tests that uses generated jersey interface for client interaction.
  * `var/conf/conf.yml` - the Undertow application configuration yml file

* `build.gradle` - the root level gradle script where a set of gradle plugins are configured, including [gradle-conjure](https://github.com/palantir/gradle-conjure).
* `settings.gradle` - the gradle settings file where all sub projects are configured.
* `versions.props` - a property file of the [nebula version recommender plugin](https://github.com/nebula-plugins/nebula-dependency-recommender-plugin) with which we can specify versions of project dependencies, including conjure generators.

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
