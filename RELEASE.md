# Releasing the project

This file describes how to release commons-eid to Maven Central.

## Prerequisites

See https://central.sonatype.org/pages/apache-maven.html.

* Configure the OSSRH server in your Maven `settings.xml`. Make sure to encrypt the Jira password (see https://maven.apache.org/guides/mini/guide-encryption.html).
```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>your-jira-id</username>
      <password>your-jira-pwd</password>
    </server>
  </servers>
</settings>
```

* Configure GPG and create a key. See https://central.sonatype.org/pages/working-with-pgp-signatures.html for detailed steps.

* Use JDK8 for building.

## Performing a snapshot release

Execute the command `mvn clean deploy -Prelease`.
The snapshot release will be available in https://oss.sonatype.org/content/repositories/snapshots/be/bosa/commons-eid/.

## Performing a release

Execute the following commands:
```bash
mvn release:prepare -DautoVersionSubmodules=true -Prelease
mvn release:perform -Prelease
```
The last command will output a staging repository id. This is required in later steps.

Now verify the artifacts at the Nexus staging repository (https://oss.sonatype.org/content/repositories/staging/be/bosa/commons-eid/commons-eid-client/).

If the artifacts are okay, release them using:
```bash
mvn nexus-staging:release -Prelease -DstagingRepositoryId=<stagingRepositoryId>
```

Otherwise drop them with:
```bash
mvn nexus-staging:drop -Prelease -DstagingRepositoryId=<stagingRepositoryId>
```
