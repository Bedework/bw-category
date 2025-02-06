# bw-category
Category server

## Using
The category service allows retrieval of categories by href or 
searching based on strings and filters.

### Retrieve a category

http://localhost:8080/bwcat/category/dmoz/Sports/

### Search for a category with a string

## Deploying
```
cd bw-category-ear
mvn bw-deploy:deploy-ears
```

## Requirements

1. JDK 17
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release use the release script:

> ./bedework/build/quickstart/linux/util-scripts/release.sh <module-name> "<release-version>" "<new-version>-SNAPSHOT"

When prompted, indicate all updates are committed

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.0
* First version committed
* Allow for long and double types in json.
* Use new json library property to avoid null values.

### 4.0.1
* Update library versions
* Simplify the configuration utilities.
* Move Indexing mbean out of opensearch 
cpackage to remove unnecessary dependencies.

### 4.0.2
* Update library versions
* Omitted to remove bw-xml as a dependency

### 4.0.3
* Update library versions
* Updates for opensearch 2.18.0
* make CategoryException subclass of RuntimeException

### 4.0.4
* Update library versions
* Repackage category server
* Remove last traces of elastic search from class names. Make some more generic.
* Move response classes and ToString into bw-base module.
