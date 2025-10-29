# Spring Boot Starter to consume Terrakube Api

## Introduction

This repository is for Spring Boot Starters of Terrakube feign Client.

### Build library

To build the library locally use the following command.

```
mvn install -Dgpg.skip
```

## Client library

Simply add the following dependency to your project's `pom.xml` will enable you to use the `TerrakubeClient` class.

```xml
<dependency>
    <groupId>io.terrakube.client</groupId>
    <artifactId>terrakube-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Example
```java
class ApiClientStarterSampleApplicationTests {

    @Autowired
    TerrakubeClient terrakubeClient;

    @Test
    void contextLoads() {
        terrakubeClient.getAllOrganizations().getData().size();
    }
}
```

```
io.terrakube.client.url=http://localhost:8080
io.terrakube.client.enableSecurity = true
io.terrakube.client.secretKey=JlJkNlVSQHEmOVhYU3heSHckY1pIQSMkenRVWXp2QEU=
io.terrakube.client.credentialType=INTERNAL
```

## Supported Properties 
| Name                               | Description                                  |
|:-----------------------------------|----------------------------------------------|
| io.terrakube.client.url            | Terrakube API Url                            |
| io.terrakube.client.enableSecurity | Add bearer token in every request to the API |
| io.terrakube.client.credentialType | PAT (Personal Access Token)                  |
| io.terrakube.client.secretKey      | PAT Token from Terrakube                     |

