# CLAUDE.md

이 파일은 이 저장소에서 작업할 때 Claude Code(claude.ai/code)가 참고할 가이드를 제공한다.

## 프로젝트 목적

AICC Interface Server — 기존 렌탈 백오피스 시스템과 AICC(컨택센터) 솔루션 사이에 위치하는 Spring Boot API
계층이다. AICC로부터 요청을 받아 검증하고, 필요에 따라 백오피스 API를 호출(OpenFeign 사용)하거나 DB를
조회(MyBatis 사용)한 뒤 JSON으로 응답한다. 전체 원본 스펙(한국어)은 `docs/PROJECT.md`, 배경/설계 관련 노트는
`docs/aicc-gateway-design.html`을 참고 — 아키텍처를 변경하기 전에 두 문서 모두 읽을 것.

## 기술 스택 (사용자의 명시적 승인 없이 업그레이드하지 말 것)

Java 11 / Spring Boot 2.7.18 / WAR 패키징 / MyBatis 2.1.3 / OpenFeign (Spring Cloud 2021.0.8) /
MariaDB JDBC 3.0.8 / springdoc-openapi-ui 1.8.0 / Jasypt 3.0.3 / Lombok.

강제 제약사항 (`docs/PROJECT.md` §18 기준): Spring Boot 3.x, Java 17+, Jakarta EE로 이전하지 **말 것**, 그리고
`springdoc-openapi-ui`를 `springdoc-openapi-starter-webmvc-ui`로 바꾸지 말 것 — 이것들은 Boot 3.x 전용이다.
새 의존성을 추가할 때는 왜 필요한지 설명하고, Boot 2.7.18/Java 11과의 호환성을 확인한 뒤, 반드시 사용자에게
먼저 물어볼 것.

## 명령어

```bash
mvn clean package                # 빌드 (테스트 포함)
mvn clean package -DskipTests    # 빌드, 테스트 생략
mvn spring-boot:run                                    # 실행 (기본값은 `local` 프로파일)
mvn spring-boot:run -Dspring-boot.run.profiles=local    # 실행, 프로파일 명시
mvn test                                                # 전체 테스트 실행
mvn test -Dtest=AiccApplicationTests                    # 단일 테스트 클래스 실행
mvn test -Dtest=AiccApplicationTests#contextLoads       # 단일 테스트 메소드 실행
```

빌드 결과물: `target/aicc-0.0.1-SNAPSHOT.war`.

`mvn`/`JAVA_HOME`이 PATH에 잡혀 있지 않다면, IntelliJ 설치본으로 임시로 대체할 수 있다 — 번들된 정확한 경로는
`docs/guide/run-guide.md` 참고.

## 로컬에서 실행하기

`local` 프로파일(기본값, `application-local.yml`)은 **H2 인메모리 DB**를 사용한다 — MariaDB 설치가 필요 없다.
`schema-h2.sql`과 `data-h2.sql`(클래스패스 루트)이 기동 시 자동 실행되어 샘플 데이터를 채운다. `dev`/`prod`
프로파일은 실제 MariaDB를 사용하며, 환경변수(`DB_USERNAME`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`, `DB_NAME`)로만
설정된다.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 콘솔 (local 전용): http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:aicc`, 사용자 `sa`, 비밀번호 없음

참고: `application-local.yml`에는 `spring.sql.init.encoding: UTF-8` 설정이 반드시 필요하다 — 없으면 Windows에서
(비-UTF-8 플랫폼 기본 인코딩 때문에) 한글 시드 데이터가 깨진다.

## 아키텍처

계층형, 단방향 흐름 — Controller에는 비즈니스 로직이 있으면 안 된다:

```
HTTP 요청 → Controller → Service → { MyBatis Mapper → DB, Feign Client → 백엔드 API } → Service → Response DTO → JSON
```

`com.onestar.aicc` 하위 패키지 구성:

| 패키지 | 역할 |
| --- | --- |
| `config` | Spring 설정 (`OpenApiConfig`, 추후 `FeignConfig`/`DatabaseConfig`) |
| `aop` | 횡단 관심사 AOP 애스펙트 — `TransactionLoggingAspect`가 `@Transactional` 경계 상태(신규 시작 vs 기존 트랜잭션 참여, 커밋/롤백)를 로그로 남김 |
| `commons` | 공통 유틸리티 — 현재는 `commons/response`: `ApiResponse<T>`(응답 래퍼), `PageResponse<T>`(페이지네이션), `ErrorResponse` |
| `controller` | HTTP 요청을 받아 `service`로 위임하고 응답을 반환 — 비즈니스 로직 없음 |
| `service` | 비즈니스 로직; `mapper`와 `client` 호출을 조율 |
| `mapper` | MyBatis `@Mapper` 인터페이스 (SQL은 Java가 아니라 `resources/mapper/**/*.xml`에 위치) |
| `domain` | DB 엔티티 / 도메인 객체 (MyBatis `type-aliases-package`) |
| `dto` | API 요청/응답 객체 |
| `client` | 백오피스/외부 API 호출용 OpenFeign 클라이언트 |
| `exception` | 예외 처리 (전역 `@RestControllerAdvice`가 이곳에 추가될 예정) |
| `sample` | 독립된 참고용 구현체일 뿐 — 아래 설명 참고. 실제 비즈니스 로직 아님. |

실제 비즈니스 패키지(`controller`, `service`, `mapper`, `domain`, `dto`, `client`, 최상위 `exception`)는 아직
존재하지 않는다 — 실제 비즈니스 요건이 들어올 때 생성한다. 그 전에 미리 비즈니스 로직을 만들어내지 말 것 —
`docs/PROJECT.md` §14에 따라 초기 범위는 인프라 배선 + 헬스 체크로 의도적으로 제한되어 있다.

MyBatis 설정: mapper XML 파일은 `resources/mapper/**/*.xml` 아래에 위치하며, `map-underscore-to-camel-case: true`가
켜져 있어 snake_case DB 컬럼이 camelCase Java 필드로 자동 매핑된다. 타입 별칭(type alias)은
`com.onestar.aicc.domain`을 기준으로 해석된다.

### `sample` 패키지

`sample/`(controller/service/mapper/dto/domain/exception, 그리고 `resources/mapper/sample/SampleBookMapper.xml`,
`schema-h2.sql`, `data-h2.sql`)는 **학습 전용 참고 구현체**다 — 실제 비즈니스 로직과 무관한 "Book" CRUD API다.
이 프로젝트의 관례(계층 구조, MyBatis mapper 스타일, `ApiResponse`/`PageResponse` 사용법, 예외 처리, Swagger
애노테이션까지 전 과정)를 보여주기 위한 목적으로만 존재한다. 실제 기능을 만들 때는 `sample` 자체를 확장하지 말고
그 패턴을 실제 패키지로 복사해서 쓸 것. 애노테이션 하나하나에 대한 전체 설명은 `docs/guide/swagger-guide.md`
참고.

`sample`의 `SampleExceptionHandler`는 일부러
`@RestControllerAdvice(basePackages = "com.onestar.aicc.sample")`로 범위를 제한해서, 추후 최상위 `exception`
패키지에 생길 전역 예외 핸들러와 충돌하지 않도록 했다 — 실제 코드는 패키지별로 여러 개가 아니라 전역 핸들러
하나만 사용해야 한다.

### Swagger/응답 코드를 건드리기 전에 알아둘 것

- `io.swagger.v3.oas.annotations.responses.ApiResponse`(애노테이션)와 이 프로젝트의
  `com.onestar.aicc.commons.response.ApiResponse<T>`(응답 래퍼 클래스)는 이름이 같다. 관례: Swagger 애노테이션은
  짧은 이름으로 import하고, 응답 래퍼 클래스는 전체 경로(FQN)로 참조한다.
- `GroupedOpenApi` 빈을 하나라도 등록하면 springdoc이 Swagger UI 드롭다운에 등록된 그룹*만* 보여준다 — "전체
  API" 그룹을 자동으로 추가해주던 동작이 사라진다. 그래서 `OpenApiConfig`는 좁은 범위의 그룹들과 함께
  `pathsToMatch("/**")`로 지정한 `all`이라는 명시적 그룹을 유지한다 — 새 그룹을 추가할 때도 이 그룹은 유지할 것.
- `HttpMessageNotReadableException`(잘못된 형식의 JSON 요청 본문)은 `@Valid`/`MethodArgumentNotValidException`
  처리 대상에 잡히지 않는다 — 별도로 처리하지 않으면 일반적인 500 에러로 흘러간다.

## 설정 / 시크릿

Jasypt가 민감한 설정 값을 암호화하며, 복호화 비밀번호는 환경변수 `JASYPT_ENCRYPTOR_PASSWORD`에서 가져온다. DB
등 환경별 값은 하드코딩하지 않고 프로파일별 환경변수로 주입한다 — `dev`/`prod`의 `application-*.yml` 참고.
