# 트랜잭션 경계 로깅 가이드

## 1. 이 기능이 뭔가요

서비스가 서비스를 호출하는 구조가 되면, "이 메소드 호출들이 결국 하나의 트랜잭션으로 묶이는 건지, 서로
분리된 트랜잭션인지", 그리고 "어디서 예외가 나서 롤백이 됐는지"를 코드만 보고 파악하기 어려워진다.

`TransactionLoggingAspect`(`src/main/java/com/onestar/aicc/aop/TransactionLoggingAspect.java`)는 이걸
로그 한 줄씩으로 바로 보여주는 AOP 애스펙트다. `@Transactional` 메소드가 호출될 때마다 다음을 로그로
남긴다.

- 이 호출이 **새 트랜잭션을 시작**하는 건지, **이미 진행 중인 트랜잭션에 참여**하는 건지
- 결과적으로 **커밋**됐는지 **롤백**됐는지 (실제 예외 타입과 Spring의 롤백 규칙까지 반영)
- 같은 트랜잭션에 묶인 호출들을 하나의 `trace id`(예: `tx-1`)로 연결해서 보여줌

---

## 2. 사용법 — `@TransactionTrace` 붙이기

이 기능은 **기본적으로 꺼져 있다**. 아무 `@Transactional` 메소드에나 로그가 찍히는 게 아니라, 확인하고
싶은 흐름의 **최초 진입 지점(entry point)** 메소드에 `@TransactionTrace` 애노테이션을 붙였을 때만 켜진다.

```java
import com.onestar.aicc.aop.TransactionTrace;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    @Transactional
    @TransactionTrace   // 이 메소드부터 시작되는 호출 흐름 전체가 로그로 추적된다
    public void placeOrder(OrderRequest request) {
        paymentService.charge(request);   // 내부에서 호출하는 다른 @Transactional 메소드도
        inventoryService.decreaseStock(request);   // 같은 trace id로 자동으로 함께 로그에 남는다
    }
}
```

- **반드시 `@Transactional`과 함께 붙여야 한다.** `@TransactionTrace`만 있고 `@Transactional`이 없으면
  애초에 트랜잭션 경계 자체가 없으므로 아무 효과가 없다.
- `@TransactionTrace`가 붙은 메소드 **안에서** 연쇄적으로 호출되는 다른 `@Transactional` 메소드들(다른
  서비스 빈이어도 상관없음)은 별도로 애노테이션을 붙이지 않아도 같은 흐름의 일부로 자동으로 로그에
  남는다.
- 반대로 `@TransactionTrace`가 없는 메소드는 `@Transactional`이 붙어 있어도, 그리고 그 흐름이 추적 중인
  다른 흐름의 일부가 아니라면 로그를 전혀 남기지 않는다. 확인이 필요한 서비스에만 골라서 켜는 용도다.

---

## 3. 실제 예시 — `SampleBookService`

`src/main/java/com/onestar/aicc/sample/service/SampleBookService.java`에 이미 적용돼 있다.

```java
@Transactional
@TransactionTrace
public BookResponse update(Long bookId, BookRequest request) { ... }   // 추적 켜짐

@Transactional
public void delete(Long bookId) { ... }                                // 추적 꺼짐 (비교용)
```

`local` 프로파일로 앱을 띄운 뒤 두 API를 호출해보면 차이를 바로 확인할 수 있다.

```bash
# 추적 켜짐 — 로그가 찍힌다
curl -X PUT http://localhost:8080/api/v1/sample/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Effective Java","author":"Joshua Bloch","price":39000,"status":"AVAILABLE"}'

# 추적 꺼짐 — 로그가 전혀 안 찍힌다
curl -X DELETE http://localhost:8080/api/v1/sample/books/2
```

`update()` 호출 시 콘솔에 다음과 같이 찍힌다.

```text
[tx-1] 트랜잭션 시작(NEW) SampleBookService.update(..) 전파방식=REQUIRED 읽기전용=false 스레드=http-nio-8080-exec-1
[tx-1] 커밋 완료 SampleBookService.update(..) 소요시간=71ms
```

존재하지 않는 `bookId`로 호출해서 `SampleNotFoundException`을 유도하면 이렇게 롤백이 찍힌다.

```text
[tx-2] 트랜잭션 시작(NEW) SampleBookService.update(..) 전파방식=REQUIRED 읽기전용=false 스레드=http-nio-8080-exec-3
[tx-2] 롤백 SampleBookService.update(..) 소요시간=2ms 예외=com.onestar.aicc.sample.exception.SampleNotFoundException: ...
```

`delete()`는 `@TransactionTrace`가 없으므로 호출해도 `TransactionLoggingAspect` 관련 로그가 전혀 남지
않는다.

---

## 4. 로그 한 줄씩 읽는 법

| 로그 문구 | 의미 |
| --- | --- |
| `트랜잭션 시작(NEW)` | 이 호출 시점에 활성 트랜잭션이 없었다 — 이 호출이 새 물리 트랜잭션을 연다. |
| `트랜잭션 시작(SUSPENDED_NEW)` | `REQUIRES_NEW`처럼 바깥 트랜잭션을 잠시 멈추고 독립된 새 트랜잭션을 연다. |
| `기존 트랜잭션에 참여` | 이미 진행 중인 트랜잭션이 있어서 새로 시작하지 않고 거기에 묶여 들어간다. |
| `커밋 완료` | 이 호출이 물리 트랜잭션의 경계였고, 정상적으로 커밋됐다. |
| `참여 호출 정상 종료` (DEBUG) | 참여 호출이 정상 종료됐다는 뜻일 뿐, 실제 커밋 여부는 바깥쪽(물리 경계) 호출의 `커밋 완료`/`롤백` 로그에서 확인해야 한다. |
| `롤백` | 예외가 발생했고, `@Transactional`의 롤백 규칙(`rollbackFor` 등 포함)상 실제로 롤백된다. |
| `커밋(롤백 대상이 아닌 예외)` | 예외가 발생했지만 Spring 기본 규칙(체크 예외는 커밋)상 롤백되지 않는다. |
| `참여 호출 중 예외 발생` | 참여 호출 중 예외가 났다 — `외부트랜잭션롤백유발여부`로 바깥 트랜잭션이 롤백 전용으로 표시될지 미리 알 수 있다. |

같은 대괄호 안의 `trace id`(`[tx-1]`, `[tx-2]` ...)를 grep해 보면, 그 흐름에 속한 모든 호출이 한 번에
모인다. 하나의 `trace id` 아래 여러 서비스 메소드가 `기존 트랜잭션에 참여`로 묶여 있다면 하나의
트랜잭션으로 잘 합쳐지고 있다는 뜻이고, 새로운 `trace id`가 따로 찍혔다면 트랜잭션이 의도치 않게
분리된 건 아닌지 확인해봐야 한다.

---

## 5. 로그 레벨

- `트랜잭션 시작` / `기존 트랜잭션에 참여` / `커밋 완료` / `롤백` / `참여 호출 중 예외 발생`은 `INFO`
  레벨이다 — 흐름 파악에 필요한 핵심 이벤트만 여기서 보인다.
- `참여 호출 정상 종료`처럼 정상적으로 끝난 참여 호출의 상세 내역은 `DEBUG` 레벨이라 자주 찍혀서
  거슬리지 않는다.
- 별도 설정 없이도 `application-local.yml`/`application-dev.yml`의 기존 `logging.level.com.onestar.aicc:
  debug` 설정을 그대로 상속받는다. `prod`(기본 `root: info`)에서는 `INFO` 레벨 이벤트만 보인다.

---

## 6. 알아둘 점 — Spring AOP 우회 케이스

Spring AOP 프록시는 **다른 빈으로의 호출**이나 **빈 바깥에서 들어오는 호출**만 가로챌 수 있다. 같은
클래스 안에서 `this.otherMethod()`처럼 자기 자신을 직접 호출하면 프록시를 거치지 않으므로 트랜잭션
경계도, 이 로깅도 전혀 적용되지 않는다. 서비스 메소드를 나눌 때 이 점을 염두에 둘 것.

---

## 7. 관련 파일

| 파일 | 역할 |
| --- | --- |
| `src/main/java/com/onestar/aicc/aop/TransactionLoggingAspect.java` | 로깅을 실제로 수행하는 애스펙트 |
| `src/main/java/com/onestar/aicc/aop/TransactionTrace.java` | 추적을 켜는 마커 애노테이션 |
| `src/main/java/com/onestar/aicc/sample/service/SampleBookService.java` | 사용 예시 (`update()`는 켜짐, `delete()`는 꺼짐) |
