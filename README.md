Overview
========
A sample of bank implementation using Token Integration SDK.

Responds to TokenOS bank API requests with static data;
alter the model to integrate with bank systems.


IMPORTANT
=========
Regenerate certificates before deploying this in a production environment.
See `config/tls/README.md` for details.

Server
=======

Config
------
TokenOS and the bank use TLS for secure communication. Setting this up
involves generating and sharing cryptographic secrets.
These secrets are located in the `config/tls` directory.
`config/tls/README.md` has details.

The server responds to requests with static data.
This static data is configured in the `config/application.conf` file.

Build
------

To build the server run the command specified below. The server uses
gradle build tool.

```sh
./gradlew build
```

Run
------

The build produces shadow (fat) jar that can be run from the command line.
E.g., to run the server, passing the `--ssl` flag: 

```sh
java -jar build/libs/bank-sample-java-1.1.6-all.jar --ssl
```

Changing
========

As written, the service responds to TokenOS requests with static data
pulled from a configuration file. It does some accounting, recording
account entries in in-memory data structures; but it doesn't persist
its data; it "forgets" everything when reset.

This is useful for passing the TokenOS test suite; but the bank
should alter this code so that it actually interacts with bank systems.
This means changing the `model` code
(in `src/main/java/io/token/banksample/model`).
This code's interfaces are designed as a lowest-common-denominator
model of bank systems.
When altering this code to use real bank systems, the bank almost
certainly must add new methods and alter existing methods.
Rather than one "Accounting" abstraction, the bank might
have a few systems; it makes sense to add more classes to
the model to reflect this.

`src/main/java/io/token/banksample/model`
* `Accounting.java` - Accounting interface
* `impl/`
  * `AccountingImpl.java` - Accounting implementation

The bank needs to add interfaces to the model to work with bank
systems not included in the original model.
E.g., the service interfaces have places to check for fraud
screening and sanctions screening; but the model doesn't
implement those. It's up to the bank to write this code
"from scratch".

The bank will also change the `services` code
(in `src/main/java/io/token/banksample/services`).
Here, the interfaces stay the same: they're defined by TokenOS.
But the implementation that handles those methods should change
to use the model in the bank-specific correct manner.

`src/main/java/io/token/banksample/services`
* `AccountServiceImpl.java` - Account information
* `TransferServiceImpl.java` - Transfers
