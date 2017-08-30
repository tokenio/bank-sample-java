Overview
========
A sample of bank implementation using Token Integration SDK.

IMPORTANT
=========
We need to regenerate certificates before deploying this in a prod environment.

Service
=======

Config
------
The certificate, private key and the trusted certificates are located in the
`service/config` directory. The trusted certificates file includes Token
development and staging certificates.

Build
------

To build the service run the command specified below. The service uses
gradle build tool.

```sh
./gradlew build
```

Run
------

To run the service locally run the command specified below. The service
is going to run on port 9300 for gRPC.

```sh
./gradlew run
```
