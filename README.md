# AICC Interface Server

기존 백오피스 시스템과 AICC 솔루션 사이의 API 연계를 담당하는 Spring Boot 기반 Interface Server.

## 기술 스택


| 구분                | 기술                | 버전                      |
| ----------------- | ----------------- | ----------------------- |
| Language          | Java              | 11                      |
| Framework         | Spring Boot       | 2.7.18                  |
| Build             | Maven             | war packaging           |
| Web               | Spring MVC        | spring-boot-starter-web |
| DB                | MariaDB           | JDBC Driver 3.0.8       |
| ORM               | MyBatis           | 2.1.3                   |
| HTTP Client       | OpenFeign         | Spring Cloud 2021.0.8   |
| API Docs          | springdoc-openapi | 1.8.0                   |
| Config Encryption | Jasypt            | 3.0.3                   |


## 빌드

```bash
mvn clean package
mvn clean package -DskipTests
```

빌드 결과물: `target/aicc-0.0.1-SNAPSHOT.war`

## 실행

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```



## Profile

- `local` : 로컬 개발 환경 (기본 활성 프로파일)
- `dev` : 개발 서버 환경
- `prod` : 운영 환경

DB 접속 정보 등은 환경변수(`DB_USERNAME`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`, `DB_NAME`)로 주입한다.

## API 문서

애플리케이션 실행 후:

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```



## Health Check

```
GET /api/v1/health
```

```json
{
  "status": "UP"
}
```



## 패키지 구조

```
com.onestar.aicc
├── AiccApplication.java
├── config      : Spring 설정 (OpenApiConfig 등)
├── commons     : 공통 유틸리티
├── controller  : HTTP 요청 수신, Service 호출
├── service     : 업무 로직
├── mapper      : MyBatis Mapper Interface
├── domain      : DB Entity / 업무 도메인 객체
├── dto         : API Request/Response 객체
├── client      : OpenFeign Client
├── exception   : 공통 예외 처리
└── sample      : 샘플 코드
```

