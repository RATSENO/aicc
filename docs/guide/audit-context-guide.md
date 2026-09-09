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

## 4. 사용법 — 서비스 코드에서 호출하기

아직 실제 `domain`/`mapper`/`service` 패키지는 없다(`sample` 패키지는 학습용 참고 구현일 뿐). 실제 비즈니스
코드가 생기면 이렇게 쓴다.

```java
import com.onestar.aicc.commons.audit.AuditContext;

// 등록(insert) 시
SomeEntity entity = SomeEntity.builder()
        .regId(AuditContext.getActorId())
        .modId(AuditContext.getActorId())
        .build();
someMapper.insert(entity);

// 수정(update) 시 — 등록자는 건드리지 않는다
existing.setModId(AuditContext.getActorId());
someMapper.update(existing);
```

**주의**: 수정 경로에서는 보통 `modId`(수정자)만 다시 세팅하고, `regId`(등록자)는 최초 등록 시점 값을 그대로
보존한다. update 로직을 작성할 때 실수로 등록자까지 덮어쓰지 않도록 주의할 것.

---

## 5. ThreadLocal 정리(clear)와 누수 주의

Tomcat은 워커 스레드(`http-nio-8080-exec-N`)를 요청마다 재사용한다. `AuditContext.clear()`를 호출하지
않으면 이전 요청에서 세팅된 actor id가 다음 요청으로 새어 들어갈 수 있다.

- `clear()`는 내부적으로 `ThreadLocal.remove()`를 호출한다 — `set(null)`은 `ThreadLocalMap`에 엔트리를
  남겨두므로 실제 누수 방지 효과가 없다.
- 정리는 오직 `AuditContextInterceptor.afterCompletion`에서만 일어난다. `afterCompletion`은 `preHandle`이
  `true`를 반환한 이상 컨트롤러/서비스가 예외를 던진 경우에도 항상 호출되므로, `finally`와 동일한 역할을 한다.
- `AuditContext.getActorId()`는 절대 `null`을 반환하지 않는다 — 인터셉터가 아예 실행되지 않은 경로(단위
  테스트 등)를 위한 코드 레벨 기본값(`AICC_BOT`)이 별도로 있다.

---

## 6. 검증 방법

### 단위 테스트

- `AuditContextTest` — `setActorId`/`getActorId` 왕복, 기본값 폴백, `clear()` 동작을 검증한다.
- `AuditContextInterceptorTest` — `MockHttpServletRequest`로 실제 서블릿 컨테이너 없이 헤더값별 분기
  (알려진 값 / 없음 / 모르는 값)와, `afterCompletion` 호출 후 실제로 정리되는지를 검증한다.

### 수동 확인 (curl)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

```bash
curl http://localhost:8080/api/v1/sample/books -H "X-AICC-Channel: CALLBOT"
curl http://localhost:8080/api/v1/sample/books
```

콘솔에서 다음과 같은 로그를 확인한다 (`local` 프로파일은 `com.onestar.aicc: debug`가 이미 켜져 있음).

```text
감사 컨텍스트 설정: uri=/api/v1/sample/books, actorId=AICC_CALLBOT
감사 컨텍스트 설정: uri=/api/v1/sample/books, actorId=AICC_BOT
```

`SampleBookService`/`BookEntity`에는 감사 컬럼이 없으므로, 이 확인은 "인터셉터가 실제 HTTP 요청에서
정상 동작했다"는 것만 증명한다. 실제 DB 쓰기에 값이 반영되는지는 실제 비즈니스 매퍼가 생겼을 때 확인한다.

---

## 7. 관련 파일

| 파일 | 역할 |
| --- | --- |
| `src/main/java/com/onestar/aicc/commons/audit/AuditContext.java` | ThreadLocal 기반 actor id 보관 |
| `src/main/java/com/onestar/aicc/commons/audit/AuditProperties.java` | `aicc.audit.*` 설정값 바인딩 |
| `src/main/java/com/onestar/aicc/commons/audit/AuditContextInterceptor.java` | 요청당 1회 actor id 판정/세팅/정리 |
| `src/main/java/com/onestar/aicc/config/WebMvcConfig.java` | 인터셉터를 `/api/**`에 등록 |
