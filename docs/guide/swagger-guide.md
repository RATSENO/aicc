# Swagger(OpenAPI) 어노테이션 가이드

## 1. 개요

이 문서는 AICC Interface Server에서 Swagger(OpenAPI 3) 문서를 작성할 때 사용하는 어노테이션과
사용법을 정리한 가이드다. 대상 독자는 Spring 기반 REST API 개발 경험이 있는 개발자이며,
Swagger 어노테이션 자체가 처음이더라도 이 문서와 실제 샘플 코드만으로 바로 적용할 수 있도록 작성했다.

전제 조건은 다음과 같다.

| 구분 | 값 |
| --- | --- |
| Spring Boot | 2.7.18 |
| Java | 11 |
| API 문서 라이브러리 | springdoc-openapi-ui 1.8.0 (Swagger 어노테이션은 `io.swagger.v3.oas.annotations.*`) |
| Bean Validation | spring-boot-starter-validation (`javax.validation.*`) |
| 샘플용 DB | H2 (`local` 프로파일 전용, 인메모리) + MyBatis |

> 주의: springdoc-openapi-ui 1.x는 Spring Boot 2.x(javax 네임스페이스) 전용이다.
> `springdoc-openapi-starter-webmvc-ui`(2.x, jakarta 네임스페이스)는 Boot 3.x 전용이므로
> 이 프로젝트에는 사용하지 않는다 (`docs/PROJECT.md` §18 참고).

실제 동작하는 전체 샘플 코드는 아래 경로에 있다.

```text
src/main/java/com/onestar/aicc/commons/response/ApiResponse.java
src/main/java/com/onestar/aicc/commons/response/PageResponse.java
src/main/java/com/onestar/aicc/commons/response/ErrorResponse.java
src/main/java/com/onestar/aicc/sample/dto/BookStatus.java
src/main/java/com/onestar/aicc/sample/dto/BookRequest.java
src/main/java/com/onestar/aicc/sample/dto/BookResponse.java
src/main/java/com/onestar/aicc/sample/dto/BookSearchCondition.java
src/main/java/com/onestar/aicc/sample/exception/SampleNotFoundException.java
src/main/java/com/onestar/aicc/sample/exception/SampleExceptionHandler.java
src/main/java/com/onestar/aicc/sample/domain/BookEntity.java
src/main/java/com/onestar/aicc/sample/mapper/SampleBookMapper.java
src/main/java/com/onestar/aicc/sample/service/SampleBookService.java
src/main/java/com/onestar/aicc/sample/controller/SampleBookController.java
src/main/java/com/onestar/aicc/config/OpenApiConfig.java
src/main/resources/mapper/sample/SampleBookMapper.xml
src/main/resources/schema-h2.sql
src/main/resources/data-h2.sql
```

`sample` 패키지는 `docs/PROJECT.md` §7에서 이미 예약된 패키지이며, 여기서 다루는 "도서(Book)" 도메인은
실제 업무와 무관한 학습용 예제다. 실제 업무 API를 작성할 때는 이 패키지의 구조와 어노테이션 사용법만
참고하고, `controller` / `service` / `dto` 등 실제 패키지에 만들면 된다.

---

## 2. Swagger UI 접속 경로

애플리케이션 실행 후 아래 경로로 접속한다 (`docs/PROJECT.md` §16과 동일).

```text
http://localhost:8080/swagger-ui.html   # Swagger UI 화면
http://localhost:8080/v3/api-docs       # OpenAPI JSON 원본
```

`server.servlet.context-path`가 설정되어 있다면 그 경로를 URL 앞에 붙여야 한다. 현재
`application.yml`에는 context-path가 설정되어 있지 않으므로 루트(`/`) 경로 그대로 사용한다.

샘플 API만 모아 보고 싶다면 Swagger UI 우측 상단의 그룹 선택 드롭다운에서 `sample`을 선택한다
(3.3절 참고).

---

## 3. OpenApiConfig 설정 설명

`config/OpenApiConfig.java`에는 세 가지 설정이 들어있다.

### 3.1 기본 문서 정보 (`Info`)

```java
@Bean
public OpenAPI openApi() {
    return new OpenAPI()
            .info(new Info()
                    .title("AICC API")
                    .description("AICC Interface Server API")
                    .version("0.0.1"))
            .components(new Components()
                    .addSecuritySchemes(BEARER_AUTH_SCHEME_NAME, bearerAuthScheme()));
}
```

Swagger UI 최상단에 표시되는 제목/설명/버전을 정의한다.

### 3.2 인증 스킴 (`SecurityScheme`)

```java
private SecurityScheme bearerAuthScheme() {
    return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");
}
```

Swagger UI의 `Authorize` 버튼에 Bearer 토큰 입력창을 노출하기 위한 정의다. 컨트롤러 메서드에서
`@SecurityRequirement(name = "bearerAuth")`로 이 스킴을 참조하면 해당 API에 자물쇠 아이콘이 표시된다.

> **중요**: 이 설정은 문서화(Swagger UI 표시) 목적일 뿐이며, 실제 토큰 검증 로직(Security Filter,
> Interceptor 등)은 별도로 구현해야 한다. 현재 프로젝트에는 인증 로직이 없다.

### 3.3 그룹 설정 (`GroupedOpenApi`)

```java
@Bean
public GroupedOpenApi allGroupedOpenApi() {
    return GroupedOpenApi.builder()
            .group("all")
            .pathsToMatch("/**")
            .build();
}

@Bean
public GroupedOpenApi sampleGroupedOpenApi() {
    return GroupedOpenApi.builder()
            .group("sample")
            .pathsToMatch("/api/v1/sample/**")
            .build();
}
```

API가 많아지면 도메인별로 그룹을 나눠서 Swagger UI 상단 드롭다운으로 전환할 수 있다.

> **주의(자주 하는 실수)**: `GroupedOpenApi` 빈을 하나라도 등록하면 springdoc은 Swagger UI 드롭다운에
> **그 빈들만** 노출하고, 전체 API를 보여주는 기본 그룹을 자동으로 추가해주지 않는다. `/v3/api-docs`
> URL을 직접 호출하면 그룹 설정과 무관하게 항상 전체 API가 내려오지만, Swagger UI 드롭다운에서는
> 보이지 않는다. 그래서 이 프로젝트는 `pathsToMatch("/**")`로 전체를 포함하는 `all` 그룹을 함께
> 등록해, 드롭다운에서 `all`(전체)과 `sample`(샘플만) 두 그룹을 모두 선택할 수 있게 했다.

---

## 4. 어노테이션 레퍼런스

| 어노테이션 | 위치 | 설명 |
| --- | --- | --- |
| `@Tag` | Class | Swagger UI에서 API를 묶어서 보여주는 그룹명/설명 |
| `@Operation` | Method | API 요약(summary), 상세 설명(description), deprecated 여부 |
| `@Parameter` | Method Parameter | `@PathVariable`/`@RequestParam` 하나에 대한 설명, 예시값, 필수 여부 |
| `@ParameterObject` | DTO Class (springdoc) | 여러 쿼리 파라미터를 하나의 DTO로 묶어서 문서화 |
| `@Schema` | Class / Field / Enum | DTO 필드 설명, 예시값, 필수 여부(`requiredMode`), 포맷 |
| `@ApiResponse` / `@ApiResponses` | Method | HTTP 상태코드별 응답 설명과 예시 |
| `@Content` / `@ExampleObject` | `@ApiResponse` 내부 | 응답 바디의 실제 JSON 예시 |
| `@SecurityRequirement` | Method / Class | 해당 API에 인증이 필요함을 표시 |
| `@Hidden` | Class / Method | 특정 API를 Swagger 문서에서 완전히 숨김 |
| `deprecated = true` (`@Operation`) + `@Deprecated` | Method | API를 사용 중단(취소선) 표시 |

각 항목의 실제 사용 예시는 아래 절에서 실제 소스 코드와 함께 설명한다.

---

## 5. Controller 레벨 예시 — `@Tag`

```java
@Tag(name = "Sample - Book", description = "Swagger 어노테이션 사용법을 보여주는 샘플 도서 API")
@RestController
@RequestMapping("/api/v1/sample/books")
public class SampleBookController {
    ...
}
```

`@Tag`는 클래스 위에 붙이며, Swagger UI에서 이 컨트롤러의 모든 API를 하나의 접이식 그룹으로
묶어서 보여준다. `name`은 그룹 제목, `description`은 그룹 설명이다.

---

## 6. 메서드 레벨 예시 — `@Operation`, `@Parameter`, `@ApiResponses`

단건 조회 API 전체 예시다 (`SampleBookController.getBook`).

```java
@Operation(summary = "도서 단건 조회", description = "bookId로 도서 한 건을 조회한다.")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
                responseCode = "404",
                description = "도서를 찾을 수 없음",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "NOT_FOUND",
                                value = "{\"code\":\"SAMPLE_NOT_FOUND\",\"message\":\"도서를 찾을 수 없습니다. bookId=999\"}"
                        )
                )
        )
})
@GetMapping("/{bookId}")
public ApiResponse<BookResponse> getBook(
        @Parameter(description = "도서 ID", example = "1", required = true)
        @PathVariable Long bookId
) {
    return ApiResponse.success(sampleBookService.getOne(bookId));
}
```

- `@Operation`: API 요약과 설명. Swagger UI 목록에서 이 텍스트가 바로 보인다.
- `@Parameter`: 경로/쿼리 파라미터 하나하나에 대한 설명과 예시값을 지정한다.
- `@ApiResponses` + `@ApiResponse`: 상태코드별로 다른 응답 스키마/예시를 지정할 수 있다.
  응답이 하나뿐이면 `@ApiResponses` 없이 `@ApiResponse` 하나만 메서드에 직접 붙여도 된다
  (목록 조회 API `getBooks`가 이 방식이다).
- `@ExampleObject`: 실제 JSON 예시 문자열을 지정해 문서에 바로 표시한다.

> **주의(자주 하는 실수)**: `io.swagger.v3.oas.annotations.responses.ApiResponse`(문서화 어노테이션)와
> 이 프로젝트의 공통 응답 래퍼 `com.onestar.aicc.commons.response.ApiResponse<T>`(6번 항목에서 설명)는
> 이름이 완전히 같다. 같은 파일에서 두 개를 동시에 짧은 이름으로 import할 수 없으므로, 실제
> `SampleBookController`에서는 어노테이션은 `import`하고 래퍼 클래스는 전체 경로(FQN)로 사용했다.
> 직접 컨트롤러를 작성할 때 `cannot find symbol` 또는 `import` 충돌 오류가 나면 이 케이스인지 먼저 확인한다.

**쿼리 파라미터를 DTO로 묶는 경우** — `@ParameterObject` (springdoc 전용 어노테이션):

```java
@Operation(summary = "도서 목록 조회", description = "키워드/상태로 필터링하고 페이지 단위로 조회한다.")
@ApiResponse(responseCode = "200", description = "조회 성공")
@GetMapping
public ApiResponse<PageResponse<BookResponse>> getBooks(@ParameterObject BookSearchCondition condition) {
    return ApiResponse.success(sampleBookService.search(condition));
}
```

`BookSearchCondition`은 `keyword`, `status`, `page`, `size` 네 개의 필드를 가진 평범한 DTO다.
`@RequestParam`을 파라미터마다 나열하는 대신 `@ParameterObject`를 DTO 위(정확히는 컨트롤러 파라미터에)
붙이면, DTO 필드에 붙은 `@Schema` 설명이 그대로 쿼리 파라미터 목록에 반영된다. 쿼리 파라미터 개수가
3~4개 이상이면 이 방식을 권장한다.

---

## 7. DTO/Schema 레벨 예시 — `@Schema`, enum 표현

```java
@Schema(description = "도서 등록/수정 요청")
public class BookRequest {

    @NotBlank(message = "제목은 필수값입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    @Schema(description = "도서 제목", example = "이펙티브 자바", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "판매 상태는 필수값입니다.")
    @Schema(description = "판매 상태", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private BookStatus status;
    ...
}
```

- 클래스 위 `@Schema(description = ...)`: Swagger UI의 Schema 섹션에서 DTO 자체에 대한 설명.
- 필드 위 `@Schema`: 필드별 설명(`description`), 예시값(`example`), 필수 여부(`requiredMode`)를 지정한다.
  `requiredMode = Schema.RequiredMode.REQUIRED`를 쓰면 필드명 옆에 `*` 표시가 붙는다.
- `BookStatus`처럼 enum 타입 필드는 Swagger UI에서 자동으로 드롭다운(select)으로 렌더링된다.
  enum 자체에도 `@Schema(description = "...")`를 붙여 각 상수의 의미를 설명할 수 있다
  (`sample/dto/BookStatus.java` 참고).

`LocalDateTime` 같은 날짜 타입 필드는 실제 JSON 직렬화 포맷과 Swagger 문서상의 `example`이 다르면
혼란을 준다. 이 프로젝트에서는 Jackson `@JsonFormat`으로 직렬화 포맷을 고정하고, `@Schema`의
`example`/`pattern`을 동일하게 맞췄다 (`sample/dto/BookResponse.java`의 `createdAt` 필드 참고).

```java
@Schema(description = "등록 일시", example = "2026-08-25T10:15:30", pattern = "yyyy-MM-dd'T'HH:mm:ss")
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime createdAt;
```

---

## 8. 요청 검증(Validation) 예시

`@Valid` + Bean Validation 어노테이션(`javax.validation.constraints.*`)을 조합하면, 검증 실패 시
Swagger 문서와 무관하게 스프링이 자동으로 `MethodArgumentNotValidException`을 던진다.

```java
@PostMapping
public ApiResponse<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
    return ApiResponse.success("도서가 등록되었습니다.", sampleBookService.register(request));
}
```

`BookRequest`에 붙은 `@NotBlank`, `@Size`, `@NotNull`, `@Positive` 등은 두 가지 역할을 동시에 한다.

1. **런타임 검증**: `@Valid`가 붙은 파라미터에서 실제로 값 검증을 수행한다 (미준수 시 400 오류).
2. **문서 표현**: springdoc이 일부 제약(예: `@Size(max=100)`)을 Swagger 스키마의 `maxLength` 등으로
   자동 변환해 문서에도 반영한다.

검증 실패 시 응답은 아래 9절의 `SampleExceptionHandler`가 공통 포맷(`ErrorResponse`)으로 변환한다.

---

## 9. 공통 응답 포맷 — `ApiResponse<T>`, `PageResponse<T>`

`commons/response` 패키지에 재사용 가능한 공통 응답 클래스를 정의했다.

```java
@Schema(description = "공통 API 응답 포맷")
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    ...
}
```

컨트롤러가 `ApiResponse<BookResponse>`, `ApiResponse<PageResponse<BookResponse>>`처럼 제네릭 타입을
그대로 반환하면, springdoc이 `data` 필드의 실제 타입을 추론해서 Swagger UI 스키마에 반영한다
(예: `ApiResponseBookResponse`, `ApiResponsePageResponseBookResponse`처럼 조합된 스키마 이름으로 표시됨).

`PageResponse<T>`는 목록 조회 API에서 페이지네이션 메타정보(`page`, `size`, `totalElements`,
`totalPages`)를 함께 내려줄 때 사용한다. 목록 API의 실제 반환 타입은
`ApiResponse<PageResponse<BookResponse>>`가 된다 (`getBooks` 예시 참고).

> 실무 팁: 제네릭 스키마 이름이 너무 길거나 겹쳐 보이면, springdoc 설정(`springdoc.use-fqn`) 또는
> `@Schema(name = "...")`로 스키마 이름을 직접 지정해 정리할 수 있다. 이 샘플에서는 기본 동작 그대로
> 두었다.

---

## 10. 에러 응답 문서화

`sample/exception/SampleExceptionHandler.java`는 `@RestControllerAdvice(basePackages =
"com.onestar.aicc.sample")`로 **sample 패키지에만 적용되도록 범위를 한정**했다. 이렇게 스코프를
좁힌 이유는, 이후 `docs/PROJECT.md` §7에서 예약된 전역 `exception` 패키지(`GlobalExceptionHandler`)가
실제 업무 요건으로 만들어질 때 이 샘플 핸들러와 충돌하지 않도록 하기 위해서다. 실제 업무 API를
개발할 때는 전역 핸들러 하나로 통합하는 것을 권장한다.

```java
@RestControllerAdvice(basePackages = "com.onestar.aicc.sample")
public class SampleExceptionHandler {

    @ExceptionHandler(SampleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SampleNotFoundException e) {
        ErrorResponse body = ErrorResponse.builder()
                .code("SAMPLE_NOT_FOUND")
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        // 필드별 오류를 ErrorResponse.FieldError 목록으로 변환
        ...
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 요청 본문이 JSON 형식이 아니거나 깨진 경우(HttpMessageNotReadableException)도
    // 아래에서 처리하지 않으면 하위 Exception.class 핸들러로 흘러가 500으로 응답하게 되므로,
    // 반드시 별도로 처리해 400으로 내려준다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        ErrorResponse body = ErrorResponse.builder()
                .code("INVALID_REQUEST_BODY")
                .message("요청 본문을 읽을 수 없습니다. JSON 형식과 인코딩(UTF-8)을 확인하세요.")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
```

이렇게 처리한 예외의 응답 스키마는 컨트롤러의 `@ApiResponse(responseCode = "404", content =
@Content(schema = @Schema(implementation = ErrorResponse.class)))`처럼 명시적으로 문서에 연결해줘야
Swagger UI에도 노출된다. 즉, 실제 예외 처리 로직(`@RestControllerAdvice`)과 문서화(`@ApiResponse`)는
서로 별개이며 항상 둘 다 챙겨야 한다.

---

## 11. 인증(Security) 스킴 문서화

```java
@Operation(
        summary = "인증이 필요한 API 문서화 예시",
        description = "실제 인증 로직은 적용되어 있지 않다. @SecurityRequirement로 Swagger UI에 "
                + "'Authorize' 잠금 아이콘과 Bearer 토큰 입력창을 노출하는 방법만 보여주는 예시이다."
)
@SecurityRequirement(name = "bearerAuth")
@GetMapping("/secure-example")
public ApiResponse<String> secureExample() {
    return ApiResponse.success("이 응답은 실제 인증 검증 없이 반환됩니다 (문서화 예시 전용).");
}
```

`name = "bearerAuth"`는 `OpenApiConfig`에서 정의한 `SecurityScheme` 이름과 반드시 일치해야 한다.
이 API를 Swagger UI에서 보면 자물쇠 아이콘이 표시되고, `Authorize` 버튼으로 입력한 토큰이
`Authorization: Bearer <토큰>` 헤더로 "Try it out" 요청에 자동으로 실려 나간다. 다시 한번 강조하면,
**이 프로젝트에는 실제 토큰 검증 로직이 없으므로** 아무 값이나 넣어도 API는 정상 응답한다. 실제
인증을 적용하려면 Security Filter/Interceptor를 별도로 구현해야 한다.

---

## 12. Deprecated API 표시

```java
@Deprecated
@Operation(
        summary = "[Deprecated] 구버전 도서 개수 조회",
        description = "더 이상 사용되지 않는 API 예시.",
        deprecated = true
)
@GetMapping("/legacy-count")
public ApiResponse<Integer> legacyCount() { ... }
```

자바의 `@Deprecated`와 Swagger의 `@Operation(deprecated = true)`를 함께 붙이면, IDE에서는 취소선
경고를, Swagger UI에서는 API 이름에 취소선과 "Deprecated" 배지를 동시에 표시할 수 있다. 두 어노테이션
중 하나만 붙이면 한쪽에서만(IDE 또는 Swagger UI 중 하나) 경고가 표시되므로 함께 사용하는 것을 권장한다.

---

## 13. Swagger UI에서 그룹 활용

`OpenApiConfig`의 `allGroupedOpenApi()` / `sampleGroupedOpenApi()` 빈으로 인해 Swagger UI 상단에
그룹 선택 드롭다운이 생긴다. `all`(전체)과 `sample`(샘플 API만) 두 그룹 중 선택해서 볼 수 있다.
실제 업무 API가 늘어나면 도메인별로 `GroupedOpenApi` 빈을 추가해 문서를 분리하는 것을 권장한다.
새 그룹을 추가하더라도 `all` 그룹(`pathsToMatch("/**")`)은 그대로 유지해야 전체 API를 한 번에
확인할 수 있는 드롭다운 옵션이 계속 남는다 (3.3절 참고).

```java
@Bean
public GroupedOpenApi customerGroupedOpenApi() {
    return GroupedOpenApi.builder()
            .group("customer")
            .pathsToMatch("/api/v1/customers/**")
            .build();
}
```

---

## 14. 실행 및 확인 방법

### 14.1 로컬 DB (H2) 구성

`local` 프로파일(`application-local.yml`)은 실제 MariaDB 대신 **H2 인메모리 DB**를 사용하도록
구성되어 있다. 별도 DB 설치 없이 `mvn spring-boot:run`만으로 샘플 API를 MyBatis 기반 실제 DB
연동까지 포함해 바로 테스트할 수 있다.

```yaml
spring:
  datasource:
    driver-class-name: net.sf.log4jdbc.sql.jdbcapi.DriverSpy
    url: jdbc:log4jdbc:h2:mem:aicc;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  sql:
    init:
      mode: always
      platform: h2
      encoding: UTF-8
```

- `MODE=MySQL`: H2가 MySQL/MariaDB 호환 SQL 문법(`LIMIT ... OFFSET ...` 등)을 사용하도록 한다.
  덕분에 `SampleBookMapper.xml`의 SQL을 나중에 실제 MariaDB로 옮길 때 거의 그대로 재사용할 수 있다.
- `DB_CLOSE_DELAY=-1`: 커넥션이 모두 끊겨도 애플리케이션이 떠 있는 동안 인메모리 DB를 유지한다.
- `spring.sql.init.mode: always` + `platform: h2`: 기동 시 `schema-h2.sql`(테이블 생성)과
  `data-h2.sql`(샘플 데이터 3건 INSERT)을 자동 실행한다. **`encoding: UTF-8`을 반드시 지정해야
  한다** — 지정하지 않으면 OS의 플랫폼 기본 인코딩으로 SQL 파일을 읽어, Windows 환경(플랫폼 기본
  인코딩이 MS949 등)에서는 한글 시드 데이터가 깨진 채로 들어간다.
- `spring.h2.console`: 브라우저에서 `http://localhost:8080/h2-console`로 접속해 JDBC URL
  `jdbc:h2:mem:aicc`, 사용자명 `sa`, 빈 비밀번호로 로그인하면 실제 저장된 데이터를 SQL로 직접
  조회할 수 있다.

`dev`/`prod` 프로파일은 그대로 실제 MariaDB를 사용하며 이번 변경의 영향을 받지 않는다. H2는
`local` 프로파일 전용이다.

샘플 도메인(`BookEntity`) → Mapper(`SampleBookMapper`) → Service(`SampleBookService`) → Controller
흐름은 PROJECT.md §9의 표준 API 구조를 그대로 따른다. 애플리케이션을 재시작하면 인메모리 DB가
초기화되고 `data-h2.sql`이 다시 실행되어 시드 데이터 3건으로 리셋된다.

### 14.2 실행 및 테스트 시나리오

```bash
mvn spring-boot:run
```

애플리케이션이 기동되면 브라우저에서 `http://localhost:8080/swagger-ui.html`에 접속한다.

1. `Sample - Book` 태그를 펼쳐 API 목록을 확인한다.
2. `POST /api/v1/sample/books`를 펼치고 `Try it out` → 예시 값 그대로 `Execute`를 눌러 도서를 등록한다.
3. `GET /api/v1/sample/books`로 방금 등록한 도서가 목록에 나오는지 확인한다.
4. 존재하지 않는 `bookId`로 `GET /api/v1/sample/books/{bookId}`를 호출해 404 + `ErrorResponse` 형식을 확인한다.
5. `title`을 빈 값으로 등록 요청을 보내 400 + 필드별 검증 오류 응답을 확인한다.
6. 잘못된 형식의 JSON(예: 닫히지 않은 중괄호)으로 요청을 보내 400 + `INVALID_REQUEST_BODY` 응답을 확인한다.
7. `http://localhost:8080/h2-console`에 접속해 `SELECT * FROM book;`으로 실제 저장된 데이터를 조회해본다.

---

## 15. 자주 하는 실수 / FAQ

**Q1. `@RequestBody`를 안 붙였더니 Swagger UI에 요청 body 입력창이 안 보인다.**
`@RequestBody`가 없으면 스프링은 해당 파라미터를 요청 바디로 바인딩하지 않고, springdoc도 이를
쿼리 파라미터가 아닌 요청 바디로 인식하지 못한다. `@Valid @RequestBody BookRequest request`처럼
반드시 `@RequestBody`를 함께 붙인다.

**Q2. `@ApiModelProperty`를 썼는데 아무 효과가 없다.**
`@ApiModelProperty`는 Swagger 2.x(`io.swagger.annotations`) 시절 어노테이션이다. OpenAPI 3 기반인
springdoc에서는 `io.swagger.v3.oas.annotations.media.Schema`를 사용해야 한다. import 문에
`io.swagger.annotations.*`가 보인다면 구버전 어노테이션이 섞인 것이니 `io.swagger.v3.oas.annotations.*`로
교체한다.

**Q3. enum 필드가 Swagger 문서에 숫자(ordinal)나 이상한 값으로 보인다.**
기본적으로 Jackson은 enum을 이름(`AVAILABLE` 등)으로 직렬화하므로 보통은 문제가 없다. 커스텀
`@JsonValue`/`@JsonCreator`를 enum에 정의했다면 실제 직렬화 값과 Swagger `example`이 다를 수 있으니
반드시 실제 응답 JSON과 Swagger 예시가 일치하는지 확인한다.

**Q4. 공통 응답 래퍼(`ApiResponse<T>`)를 상속/재사용하는 DTO마다 `@Schema` 설명이 중복된다.**
필드가 여러 DTO에서 반복된다면 공통 상위 클래스나 공통 필드 묶음 DTO로 추출하고, 그 안에서만
`@Schema`를 선언한다. 이 샘플의 `ErrorResponse`, `PageResponse<T>`처럼 재사용 가능한 응답 형태는
`commons/response` 패키지에 모아두고 여러 컨트롤러에서 공유한다.

**Q5. `@ApiResponse`(Swagger)와 우리 프로젝트의 `ApiResponse<T>`(공통 응답 래퍼) 이름이 겹쳐서
import 오류가 난다.**
6절 마지막에 설명한 것과 동일한 문제다. 한쪽은 짧은 이름으로 import하고, 다른 한쪽은 전체 경로
(FQN)로 사용한다. 이 프로젝트 컨벤션은 "Swagger 어노테이션은 짧은 이름으로 import, 공통 응답
래퍼 클래스는 FQN"이다 (`SampleBookController` 참고).

**Q6. 쿼리 파라미터가 5개, 10개로 늘어나서 메서드 시그니처가 지저분하다.**
`@RequestParam`을 하나씩 나열하지 말고, `BookSearchCondition`처럼 DTO로 묶은 뒤
`@ParameterObject`를 붙인다 (7절 참고).

**Q7. H2에 넣은 한글 시드 데이터(`data-h2.sql`)가 물음표나 깨진 글자로 조회된다.**
Windows처럼 플랫폼 기본 인코딩이 UTF-8이 아닌 환경(MS949 등)에서는 `spring.sql.init.encoding`을
지정하지 않으면 Spring Boot가 `schema-h2.sql`/`data-h2.sql`을 플랫폼 기본 인코딩으로 읽어 한글이
깨진다. `application-local.yml`의 `spring.sql.init.encoding: UTF-8` 설정이 이를 방지한다 (14.1절
참고). 같은 증상이 다른 초기화 스크립트에서도 보이면 이 설정부터 확인한다.

**Q8. 잘못된 형식의 JSON을 보냈는데 400이 아니라 500이 반환된다.**
`@Valid`가 잡아주는 것은 "형식은 올바르지만 값이 조건에 안 맞는" 경우(`MethodArgumentNotValidException`)
뿐이다. JSON 자체가 깨졌거나(중괄호 누락 등) 파싱이 불가능한 경우는 `HttpMessageNotReadableException`이
발생하며, 이를 별도로 처리하지 않으면 `Exception.class`로 흘러가 500으로 응답하게 된다. sample 패키지의
`SampleExceptionHandler`는 이 예외를 별도로 잡아 400으로 응답하도록 처리했다 (10절 참고).

---

## 16. 참고 링크

- springdoc-openapi 공식 문서: https://springdoc.org
