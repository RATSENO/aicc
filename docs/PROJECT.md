# AICC 프로젝트 구성 및 생성 요구사항

## 1. 프로젝트 목적

기존 렌탈 백오피스 시스템과 AICC 솔루션 사이에서 API 연계를 담당하는 Spring Boot 기반 AICC Interface Server 프로젝트를 생성한다.

이 프로젝트는 향후 다음과 같은 역할을 수행한다.

- AICC 솔루션의 요청을 수신한다.
- 요청 데이터를 검증한다.
- 필요한 경우 기존 백오피스 API를 호출한다.
- 필요한 경우 DB를 조회한다.
- 결과를 AICC 솔루션이 사용할 수 있는 JSON 형태로 반환한다.
- OpenAPI/Swagger를 통해 API 문서를 제공한다.

---

# 2. 기본 기술 스택

프로젝트는 다음 기술을 반드시 사용한다.


| 구분                       | 기술                | 버전                      |
| ------------------------ | ----------------- | ----------------------- |
| Language                 | Java              | 11                      |
| Framework                | Spring Boot       | 2.7.18                  |
| Build                    | Maven             | Maven Wrapper 사용 가능     |
| Packaging                | WAR               | war                     |
| Web                      | Spring MVC        | Spring Boot Starter Web |
| DB                       | MariaDB           | JDBC Driver 3.0.8       |
| ORM/SQL Mapper           | MyBatis           | 2.1.3                   |
| Connection Pool          | HikariCP          | Spring Boot 관리 버전       |
| HTTP Client              | OpenFeign         | Spring Cloud 2021.0.8   |
| API Documentation        | springdoc-openapi | 1.8.0                   |
| JSON                     | Jackson           | Spring Boot 관리 버전       |
| Logging SQL              | log4jdbc          | 1.16                    |
| Configuration Encryption | Jasypt            | 3.0.3                   |
| Boilerplate Reduction    | Lombok            | Spring Boot 관리 버전       |


Spring Boot 3.x 또는 Java 17 이상으로 변경하지 않는다.

---



# 3. Maven 프로젝트 설정



## 3.1 프로젝트 기본 정보

```xml
<groupId>com.onestar</groupId>
<artifactId>aicc</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>war</packaging>
```

프로젝트 이름은 `aicc`로 한다.

---



# 4. pom.xml 구성

다음 dependency를 사용한다.

## 4.1 Spring Web

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

REST API 서버 구현에 사용한다.

---



## 4.2 Lombok

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

Getter, Setter, Builder, Constructor 등의 보일러플레이트 코드를 줄이는 용도로 사용한다.

---



## 4.3 Spring Boot Configuration Processor

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

`@ConfigurationProperties` 기반 설정을 사용할 수 있도록 한다.

---



## 4.4 Tomcat

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

WAR 형태로 외부 Tomcat 환경에 배포할 수 있도록 구성한다.

---



## 4.5 Spring Boot Test

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---



## 4.6 OpenFeign

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

기존 백오피스 API 또는 외부 시스템 API 호출에 사용한다.

예:

```text
AICC
  ↓
OpenFeign
  ↓
기존 백오피스 API
```

---



## 4.7 Jackson

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

JSON ↔ Java Object 변환에 사용한다.

---



## 4.8 MariaDB JDBC Driver

```xml
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
</dependency>
```

MariaDB 데이터베이스 연결에 사용한다.

버전은 dependencyManagement에서 `3.0.8`로 관리한다.

---



## 4.9 HikariCP

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

DB Connection Pool에 사용한다.

---



## 4.10 MyBatis

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.3</version>
</dependency>
```

DB 접근 및 SQL 실행에 사용한다.

MyBatis XML Mapper를 기본 방식으로 사용한다.

---



## 4.11 log4jdbc

```xml
<dependency>
    <groupId>org.bgee.log4jdbc-log4j2</groupId>
    <artifactId>log4jdbc-log4j2-jdbc4.1</artifactId>
    <version>1.16</version>
</dependency>
```

SQL 실행 로그 확인에 사용한다.

---



## 4.12 Jasypt

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

DB 계정, 비밀번호, API 인증정보 등의 민감한 설정값 암호화에 사용한다.

---



## 4.13 Springdoc OpenAPI

Spring Boot 2.7.18이므로 Springdoc 1.x 계열을 사용한다.

반드시 다음 dependency를 사용한다.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.8.0</version>
</dependency>
```

Swagger UI 및 OpenAPI 문서 제공에 사용한다.

기본적으로 다음 URL에서 접근할 수 있도록 한다.

```text
/swagger-ui.html
/v3/api-docs
```

---



# 5. Spring Cloud Dependency Management

Spring Cloud 버전은 다음과 같이 관리한다.

```xml
<dependencyManagement>
    <dependencies>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.8</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>org.mariadb.jdbc</groupId>
            <artifactId>mariadb-java-client</artifactId>
            <version>3.0.8</version>
        </dependency>

    </dependencies>
</dependencyManagement>
```

OpenFeign과 Spring Boot의 호환성을 고려하여 Spring Cloud `2021.0.8`을 사용한다.

---



# 6. Maven Plugin



## 6.1 Maven Compiler Plugin

Java 11로 컴파일한다.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <forceJavacCompilerUse>true</forceJavacCompilerUse>
        <source>11</source>
        <target>11</target>
    </configuration>
</plugin>
```

---



## 6.2 Spring Boot Maven Plugin

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <executable>true</executable>
    </configuration>
</plugin>
```

---



# 7. 프로젝트 패키지 구조

다음 구조를 기본으로 생성한다.

```text
aicc
│
├── pom.xml
├── README.md
├── .gitignore
│
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── onestar
    │   │           └── aicc
    │   │               │
    │   │               ├── AiccApplication.java
    │   │               │
    │   │               ├── config
    │   │               │   └── OpenApiConfig.java
    │   │               │
    │   │               └── commons
    │   │
    │   └── resources
    │       │
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       │
    │       └── logback-spring.xml
    │
    └── test
        └── java
            └── com
                └── onestar
                    └── aicc
                        └── AiccApplicationTests.java
```

> 참고: `controller` / `service` / `mapper` / `domain` / `dto` / `client` / `exception` / `sample` 패키지와
> `resources/mapper` 디렉터리는 실제 업무 요건이 정해지면 생성한다. 현재는 존재하지 않는다.

---



# 8. 각 패키지 역할



## controller

외부에서 들어오는 HTTP 요청을 받는다.

```text
AICC
 ↓
Controller
```

Controller에서는 가능한 한 비즈니스 로직을 작성하지 않는다.

---



## service

실제 업무 로직을 담당한다.

```text
Controller
    ↓
Service
    ↓
Mapper / Feign
```

---



## mapper

MyBatis Mapper Interface를 작성한다.

예:

```java
@Mapper
public interface CustomerMapper {

    Customer selectCustomer(String customerId);

}
```

---



## resources/mapper

실제 SQL을 XML로 관리한다.

예:

```text
CustomerMapper.xml
OrderMapper.xml
ContractMapper.xml
```

---



## client

OpenFeign Client를 관리한다.

예:

```text
client
 ├── CustomerClient.java
 ├── OrderClient.java
 └── ContractClient.java
```

기존 백오피스 API 호출을 담당한다.

---



## dto

API 요청/응답 객체를 관리한다.

예:

```text
CustomerRequest
CustomerResponse
OrderRequest
OrderResponse
```

---



## domain

DB Entity 또는 업무 도메인 객체를 관리한다.

---



## config

Spring 설정을 관리한다.

예:

```text
DatabaseConfig
FeignConfig
OpenApiConfig
```

---



## exception

공통 예외 및 예외 처리 로직을 관리한다.

예:

```text
GlobalExceptionHandler
BusinessException
ErrorCode
```

---



# 9. API 기본 구조

API는 다음 구조를 기본으로 한다.

```text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
 ┌───┴────┐
 ↓        ↓
MyBatis  Feign
 ↓        ↓
DB       Backend API
     ↓
Service
     ↓
Response DTO
     ↓
JSON Response
```

---



# 10. OpenFeign 설정

OpenFeign을 활성화한다.

Application 클래스에 다음과 같은 설정을 적용한다.

```java
@EnableFeignClients
@SpringBootApplication
public class AiccApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiccApplication.class, args);
    }
}
```

---



# 11. Swagger / OpenAPI 설정

Swagger UI를 사용할 수 있도록 OpenAPI 설정 클래스를 생성한다.

```text
config/OpenApiConfig.java
```

API 문서의 기본 정보는 다음과 같이 설정한다.

```text
Title:
AICC API

Description:
AICC Interface Server API

Version:
0.0.1
```

---



# 12. application.yml

기본적인 설정 구조를 다음과 같이 만든다.

```yaml
spring:
  application:
    name: aicc

  datasource:
    driver-class-name: net.sf.log4jdbc.sql.jdbcapi.DriverSpy
    url: jdbc:log4jdbc:mariadb://localhost:3306/onestar
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher

mybatis:
  mapper-locations: classpath:/mapper/**/*.xml
  type-aliases-package: com.onestar.aicc.domain
  configuration:
    map-underscore-to-camel-case: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

실제 DB 주소, 계정, 비밀번호 등의 환경별 설정은 환경변수 또는 profile별 설정으로 분리한다.

---



# 13. Profile 구조

다음 환경을 지원한다.

```text
application.yml
application-local.yml
application-dev.yml
application-prod.yml
```

환경별로 다음 내용을 분리할 수 있도록 한다.

```text
DB URL
DB 계정
DB 비밀번호
Backend API URL
Feign 설정
Jasypt 설정
Logging 설정
```

---



# 14. 초기 구현 범위

프로젝트 생성 시 실제 비즈니스 기능을 임의로 구현하지 않는다.

우선 다음까지만 구현한다.

### 1단계

- Maven 프로젝트 생성
- pom.xml 구성
- Spring Boot 실행
- Java 11 설정
- WAR 설정
- MariaDB 연결 설정
- MyBatis 설정
- OpenFeign 설정
- Swagger 설정
- Profile 설정
- 공통 예외 처리 기본 구조
- 기본 Controller 테스트 API

---



# 15. Health Check API

프로젝트가 정상적으로 실행되는지 확인할 수 있도록 간단한 API를 만든다.

```text
GET /api/v1/health
```

응답 예:

```json
{
  "status": "UP"
}
```

Swagger UI에서 해당 API가 표시되도록 한다.

---



# 16. Swagger 확인

애플리케이션 실행 후 다음 URL을 통해 API 문서를 확인할 수 있어야 한다.

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

단, `server.servlet.context-path`가 설정된 경우 해당 context path를 URL에 포함한다.

---



# 17. 코딩 규칙

다음 원칙을 준수한다.

### Controller

Controller에는 비즈니스 로직을 작성하지 않는다.

```text
Controller
→ Request 수신
→ Service 호출
→ Response 반환
```



### Service

업무 로직을 담당한다.

### Mapper

DB SQL 호출만 담당한다.

### Feign Client

외부 API 호출만 담당한다.

### DTO

외부 API Request/Response 데이터를 표현한다.

---



# 18. 의존성 추가 원칙

현재 정의된 dependency 이외의 라이브러리를 임의로 추가하지 않는다.

새로운 라이브러리가 필요한 경우:

1. 왜 필요한지 설명한다.
2. 현재 Spring Boot 2.7.18 / Java 11 환경과 호환되는지 확인한다.
3. 사용자에게 추가 여부를 확인한다.

특히 다음 기술로 임의 변경하지 않는다.

- Spring Boot 3.x
- Java 17
- Jakarta EE
- springdoc-openapi-starter-webmvc-ui

현재 프로젝트는 Spring Boot 2.7.18 기반이므로 Springdoc 1.x 계열인 `springdoc-openapi-ui:1.8.0`을 사용한다.

---



# 19. 빌드 및 실행

프로젝트 생성 후 다음 명령으로 빌드할 수 있어야 한다.

```bash
mvn clean package
```

테스트를 제외하고 빌드할 경우:

```bash
mvn clean package -DskipTests
```

생성되는 WAR 파일:

```text
target/aicc-0.0.1-SNAPSHOT.war
```

---



# 20. Claude Code 작업 지시

Claude Code는 이 문서를 기준으로 프로젝트를 생성한다.

초기 셋업이기 때문에 상기 내용은 언제든 변경될 수 있다.

작업 순서는 다음과 같다.

```text
1. 프로젝트 디렉터리 확인
       ↓
2. pom.xml 생성
       ↓
3. Maven dependency 확인
       ↓
4. Spring Boot Main Class 생성
       ↓
5. package 구조 생성
       ↓
6. application.yml 생성
       ↓
7. MyBatis 기본 설정
       ↓
8. MariaDB DataSource 설정
       ↓
9. OpenFeign 설정
       ↓
10. OpenAPI/Swagger 설정
       ↓
11. Global Exception 기본 구조 생성
       ↓
12. Health Check API 생성
       ↓
13. 테스트 코드 작성
       ↓
14. mvn clean package 실행
       ↓
15. 빌드 오류 수정
       ↓
16. 최종 프로젝트 구조 확인
```

프로젝트 생성 과정에서 각 단계를 완료할 때마다 변경된 파일과 변경 이유를 간단하게 설명한다.

기존 파일이 존재하는 경우 임의로 삭제하거나 덮어쓰지 않는다.

비즈니스 로직은 임의로 추가하지 않는다.

---



# 21. 최종 목표

최종적으로 다음과 같은 기본 구조가 동작해야 한다.

```text
                 AICC Solution
                       │
                       │ HTTP/JSON
                       ▼
              ┌─────────────────┐
              │       AICC      │
              │ Interface Server│
              └────────┬────────┘
                       │
              ┌────────┴────────┐
              │                 │
              ▼                 ▼
         MyBatis             OpenFeign
              │                 │
              ▼                 ▼
          MariaDB          기존 백오피스
                              API
```

이후 실제 AICC 업무 기능을 추가할 때 다음과 같은 형태로 확장한다.

```text
AICC
 │
 ├── 고객 조회
 ├── 계약 조회
 ├── 주문 조회
 ├── 상품 조회
 ├── 납부 조회
 ├── 상담 조회
 └── 기타 업무 API
       │
       ├── DB 조회
       └── 기존 백오피스 API 호출
```

이 프로젝트의 핵심 목적은 **AICC 솔루션과 기존 렌탈 백오피스 시스템 사이의 안정적인 Interface/API 계층을 제공하는 것**이다.  

# 99. 참고 필수

- [논의 및 참고 사항](../aicc-gateway-design.html)

