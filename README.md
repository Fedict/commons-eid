# README for Commons eID Project

This project contains the source code tree of Commons eID.
The source code is hosted at: https://github.com/Fedict/commons-eid.

The source code of the Commons eID Project is licensed under GNU LGPL v3.0.
The license conditions can be found in the file: LICENSE.

## Build

The following is required for compiling the Commons eID software:
* Oracle Java 1.8
* Apache Maven 3

The project can be build via:
```
	mvn clean install
```

## Components

* [commons-eid-client](docs/commons-eid-client.md): actual PC/SC based eID code.
* [commons-eid-consumer](docs/commons-eid-consumer.md): consumer classes for performing eID operations.
* commons-eid-dialogs: default implementations of the required dialogs.
* [commons-eid-jca](docs/commons-eid-jca.md): a JCA security provider using the eID card.
* commons-eid-jca-all: a JCA security provider including all required dependencies.
* commons-eid-tests: integration tests for the Commons eID project.