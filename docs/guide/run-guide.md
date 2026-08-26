# 로컬 서버 구동 가이드

## 1. 사전 준비물

- Java 11 (또는 호환 JDK)
- Maven

> 참고: 개발 환경에 따라 `mvn`/JDK가 PATH에 잡혀 있지 않을 수 있다. 그런 경우 IDE(IntelliJ 등)에
> 번들된 Maven/JDK 경로를 임시로 PATH/JAVA_HOME에 지정해 실행해도 된다. 예를 들어 IntelliJ가 설치돼
> 있다면 `<IntelliJ 설치 경로>\plugins\maven\lib\maven3\bin\mvn.cmd`, `<IntelliJ 설치 경로>\jbr` 또는
> `%USERPROFILE%\.jdks\<버전>`을 활용할 수 있다. 정상적으로 Java/Maven이 설치된 환경이라면 이 부분은
> 신경 쓸 필요 없다.

---

## 2. 실행 명령

```bash
mvn spring-boot:run
```

`application.yml`의 `spring.profiles.active: local`이 기본값이므로 프로파일 지정은 생략해도 된다.
명시적으로 지정하고 싶다면 다음과 같이 실행한다.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 3. DB — 별도 설치 불필요

`local` 프로파일은 H2 인메모리 DB로 자동 구동되도록 구성되어 있다(`application-local.yml`). 따라서
로컬에 MariaDB를 별도로 설치/기동할 필요가 없다. 애플리케이션 기동 시 `schema-h2.sql`(테이블 생성)과
`data-h2.sql`(샘플 도서 3건)이 자동 실행된다.

`dev`/`prod` 프로파일은 이 변경의 영향을 받지 않으며 그대로 실제 MariaDB를 사용한다.

---

## 4. 접속 URL

| 용도 | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 콘솔 (DB 직접 조회) | http://localhost:8080/h2-console |

H2 콘솔 로그인 정보는 다음과 같다.

```text
JDBC URL : jdbc:h2:mem:aicc
사용자명  : sa
비밀번호  : (빈 값)
```

---

## 5. 참고

동일한 내용과 함께 실제 테스트 시나리오(도서 등록/조회, 404/400 응답 확인 등)는
[`swagger-guide.md`](./swagger-guide.md) §14(실행 및 확인 방법)에도 정리되어 있다.
