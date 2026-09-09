# 등록자/수정자(Audit Actor) 컨텍스트 가이드

## 1. 이 기능이 뭔가요

기존 백오피스 화면 시스템에서 "로그인한다"는 것은, 사용자가 인증에 성공하면 서버가 그 사용자를 식별하는
상태 정보(세션)를 서버 측(이 프로젝트의 경우 Redis)에 만들고, 브라우저에는 그 세션을 가리키는 식별자만
쿠키(예: JSESSIONID)로 내려주는 방식이다. 이후 브라우저가 보내는 모든 요청에 그 쿠키가 자동으로 실려서,
서버는 "쿠키 → 세션 → 지금 요청한 사람이 누구인지"를 매 요청마다 알아낼 수 있다. 게시글을 등록/수정할 때
**등록자/수정자** 컬럼에 들어가는 값은 대부분 이 세션에 저장된 로그인 사용자 ID를 그대로 쓴 것이다.

문제는 AICC 콜봇/챗봇이 이 API 서버를 호출할 때는 "로그인"이라는 행위 자체가 없다는 것이다 — 브라우저도
아니고 세션 쿠키도 없는, 완전히 stateless한 서버-투-서버 호출이다. 그래서 세션에서 사용자 ID를 꺼내오는
기존 방식이 원천적으로 성립하지 않는다.

> 참고: `docs/aicc-gateway-design.html` §05는 이와 별개로, 아직 만들지 않은 "Gateway" 컴포넌트가 백오피스의
> 기존 화면용 세션-쿠키 API를 "AICC 전용 시스템 계정"으로 대신 호출해주는 방식을 검토 중이라고 적어뒀다.
> 이건 이 서버가 MyBatis로 백오피스 DB에 **직접** 쓰는 경로와는 다른 얘기다 — 이 가이드가 다루는 건 후자다.

이 문제를 풀기 위해, 요청이 들어올 때 "이 호출이 누구를 대신해서 온 것인지(actor id)"를 한 번 판정해서
요청 처리 내내 공통으로 꺼내 쓸 수 있게 해주는 게 `AuditContext`다.

---

## 2. 전체 흐름

```
HTTP 요청 (헤더에 채널 정보 포함)
    ↓
AuditContextInterceptor.preHandle
    - 헤더값 읽어서 actor id 판정
    - AuditContext(ThreadLocal)에 세팅
    ↓
Controller → Service → Mapper
    - 등록자/수정자를 채울 때 AuditContext.getActorId()로 조회만 하면 됨
    ↓
AuditContextInterceptor.afterCompletion
    - AuditContext.clear() — 반드시 정리
```

`TransactionLoggingAspect`가 `@Transactional` 메소드 경계마다 관여하는 것과 비슷하게, 이 인터셉터는 HTTP
요청 하나당 딱 한 번(진입 시 세팅, 종료 시 정리) 관여한다.

---

## 3. 설정값

`src/main/resources/application.yml`:

```yaml
aicc:
  audit:
    channel-header: X-AICC-Channel
    channel-actor-map:
      CALLBOT: AICC_CALLBOT
      CHATBOT: AICC_CHATBOT
    default-actor-id: AICC_BOT
```

| 키 | 의미 |
| --- | --- |
| `aicc.audit.channel-header` | AICC가 콜봇/챗봇 구분을 실어 보내는 HTTP 헤더 이름 |
| `aicc.audit.channel-actor-map` | 헤더 값(대문자 기준) → 등록자/수정자 컬럼에 쓸 actor id 매핑 |
| `aicc.audit.default-actor-id` | 헤더가 없거나 매핑에 없는 값일 때 쓸 기본값 |

**AICC와 실제 헤더 이름/값이 아직 확정되지 않았다.** 확정되면 이 `application.yml` 블록만 고치면 된다 —
`AuditContextInterceptor`나 서비스 코드는 손댈 필요가 없다.

---

## 4. 사용법 — domain 엔티티가 `BaseAuditEntity`를 상속받기만 하면 끝

처음에는 서비스 코드가 `AuditContext.getActorId()`를 직접 호출해서 값을 채우는 방식으로 안내했지만, 지금은
그럴 필요가 없다. MyBatis insert/update 대상 domain 엔티티가 `com.onestar.aicc.commons.audit.BaseAuditEntity`
(`regId`/`modId` 필드 보유)를 상속받기만 하면, insert/update가 실제로 실행되는 순간
`AuditColumnMyBatisInterceptor`가 자동으로 값을 채워준다. **서비스 코드는 더 이상 아무것도 할 필요가 없다.**

```java
// domain 엔티티는 BaseAuditEntity만 상속받으면 된다
public class SomeEntity extends BaseAuditEntity {
    private Long id;
    private String name;
    // ...
}

// 서비스 코드는 평소처럼 mapper만 호출하면 regId/modId가 자동으로 채워진다
SomeEntity entity = SomeEntity.builder()
        .name(request.getName())
        .build();
someMapper.insert(entity);   // 실행되는 순간 regId, modId가 자동 세팅됨

someMapper.update(existing); // 실행되는 순간 modId만 갱신됨 (regId는 그대로 보존)
```

실제 참고 구현은 `sample` 패키지의 `BookEntity`/`SampleBookMapper`다 — `BookEntity extends BaseAuditEntity`로
선언돼 있고, `SampleBookMapper.xml`의 `insertBook`/`updateBook`이 각각 `reg_id`/`mod_id` 컬럼을 다룬다.

---

## 5. MyBatis 인터셉터가 하는 일

`AuditColumnMyBatisInterceptor`는 MyBatis의 `Executor.update(MappedStatement, Object)` 호출을 가로챈다
(INSERT/UPDATE/DELETE가 모두 이 메소드를 거친다).

- **`MappedStatement.getSqlCommandType()`으로 INSERT/UPDATE/DELETE를 구분**한다. INSERT/UPDATE만 처리하고,
  DELETE는 그대로 통과시킨다.
- **INSERT**: `regId`, `modId` 둘 다 `AuditContext.getActorId()`로 세팅.
- **UPDATE**: `modId`만 세팅. `regId`(등록자)는 최초 등록 시점 값을 그대로 보존해야 하므로 절대 건드리지
  않는다 — 게다가 `SampleBookMapper.xml`의 `updateBook` SQL 자체도 `reg_id` 컬럼을 아예 언급하지 않아서,
  Java 객체에 실수로 `regId`가 세팅돼 있어도 DB에는 영향이 없는 이중 안전장치가 있다.
- **파라미터 판정**: `@Param` 없이 엔티티 하나만 받는 mapper 메소드(예: `insertBook(BookEntity book)`)는
  파라미터가 곧 `BaseAuditEntity` 인스턴스라 바로 판정된다. `@Param`을 쓰는 메소드는 MyBatis가 파라미터를
  `Map`으로 감싸므로, 그 안에서 `BaseAuditEntity` 타입 값을 찾는다 — **정확히 하나만 있어야** 자동 세팅이
  되고, 0개나 2개 이상이면 애매하므로 추측하지 않고 경고 로그만 남긴 채 건너뛴다.

---

## 6. ThreadLocal 정리(clear)와 누수 주의

Tomcat은 워커 스레드(`http-nio-8080-exec-N`)를 요청마다 재사용한다. `AuditContext.clear()`를 호출하지
않으면 이전 요청에서 세팅된 actor id가 다음 요청으로 새어 들어갈 수 있다.

- `clear()`는 내부적으로 `ThreadLocal.remove()`를 호출한다 — `set(null)`은 `ThreadLocalMap`에 엔트리를
  남겨두므로 실제 누수 방지 효과가 없다.
- 정리는 오직 `AuditContextInterceptor.afterCompletion`에서만 일어난다. `afterCompletion`은 `preHandle`이
  `true`를 반환한 이상 컨트롤러/서비스가 예외를 던진 경우에도 항상 호출되므로, `finally`와 동일한 역할을 한다.
- `AuditContext.getActorId()`는 절대 `null`을 반환하지 않는다 — 인터셉터가 아예 실행되지 않은 경로(단위
  테스트 등)를 위한 코드 레벨 기본값(`AICC_BOT`)이 별도로 있다.

---

## 7. 검증 방법

### 단위 테스트

- `AuditContextTest` — `setActorId`/`getActorId` 왕복, 기본값 폴백, `clear()` 동작을 검증한다.
- `AuditContextInterceptorTest` — `MockHttpServletRequest`로 실제 서블릿 컨테이너 없이 헤더값별 분기
  (알려진 값 / 없음 / 모르는 값)와, `afterCompletion` 호출 후 실제로 정리되는지를 검증한다.
- `AuditColumnMyBatisInterceptorTest` — INSERT 시 regId/modId 둘 다 세팅, UPDATE 시 modId만 세팅(regId
  보존), DELETE는 아무것도 안 건드림, `Map` 파라미터에서 정확히 하나만 매칭되면 세팅되고 0개/2개 이상이면
  건너뛰는지를 검증한다.

### 수동 확인 (curl) — DB 반영까지 end-to-end 확인

`sample` 패키지(`BookEntity`)에 실제로 `reg_id`/`mod_id` 컬럼이 반영돼 있으므로, HTTP 헤더 → DB 저장까지
전체 흐름을 눈으로 확인할 수 있다.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

```bash
# 1. 콜봇으로 등록
curl -X POST http://localhost:8080/api/v1/sample/books \
  -H "Content-Type: application/json" \
  -H "X-AICC-Channel: CALLBOT" \
  -d '{"title":"테스트 도서","author":"테스터","price":10000,"status":"AVAILABLE"}'
# → 응답에 "regId":"AICC_CALLBOT","modId":"AICC_CALLBOT"

# 2. 조회로 재확인 (헤더 없이 — DB에서 읽어온 값임을 증명)
curl http://localhost:8080/api/v1/sample/books/4

# 3. 챗봇으로 수정
curl -X PUT http://localhost:8080/api/v1/sample/books/4 \
  -H "Content-Type: application/json" \
  -H "X-AICC-Channel: CHATBOT" \
  -d '{"title":"테스트 도서(수정)","author":"테스터","price":12000,"status":"AVAILABLE"}'
# → 응답에 "regId":"AICC_CALLBOT"(보존), "modId":"AICC_CHATBOT"(변경)

# 4. 다시 조회로 재확인
curl http://localhost:8080/api/v1/sample/books/4
```

H2 콘솔(`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:aicc`, 사용자 `sa`, 빈 비밀번호)에서
`SELECT book_id, reg_id, mod_id FROM book;`로 직접 확인해도 된다 — 시드 데이터(1~3번)는 인터셉터를 거치지
않고 들어간 것이므로 `NULL`/`NULL`이고, 새로 등록/수정한 행만 값이 채워져 있어야 정상이다.

---

## 8. 관련 파일

| 파일 | 역할 |
| --- | --- |
| `src/main/java/com/onestar/aicc/commons/audit/AuditContext.java` | ThreadLocal 기반 actor id 보관 |
| `src/main/java/com/onestar/aicc/commons/audit/AuditProperties.java` | `aicc.audit.*` 설정값 바인딩 |
| `src/main/java/com/onestar/aicc/commons/audit/AuditContextInterceptor.java` | 요청당 1회 actor id 판정/세팅/정리 (HTTP `HandlerInterceptor`) |
| `src/main/java/com/onestar/aicc/config/WebMvcConfig.java` | `AuditContextInterceptor`를 `/api/**`에 등록 |
| `src/main/java/com/onestar/aicc/commons/audit/BaseAuditEntity.java` | insert/update 대상 domain 엔티티가 상속받는 regId/modId 베이스 클래스 |
| `src/main/java/com/onestar/aicc/commons/audit/AuditColumnMyBatisInterceptor.java` | insert/update 실행 시 regId/modId 자동 세팅 (MyBatis `Interceptor`) |
