package com.onestar.aicc.sample.controller;

import com.onestar.aicc.commons.response.ErrorResponse;
import com.onestar.aicc.commons.response.PageResponse;
import com.onestar.aicc.sample.dto.BookRequest;
import com.onestar.aicc.sample.dto.BookResponse;
import com.onestar.aicc.sample.dto.BookSearchCondition;
import com.onestar.aicc.sample.service.SampleBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Swagger(OpenAPI 3) 어노테이션 사용법을 보여주기 위한 샘플 Controller.
 * 도서(Book) 도메인은 실제 업무와 무관한 학습용 예제이다.
 *
 * 주의: io.swagger.v3.oas.annotations.responses.ApiResponse(문서화 어노테이션)와
 * com.onestar.aicc.commons.response.ApiResponse(공통 응답 래퍼 클래스)는 이름이 같아 동시에
 * import할 수 없다. 이 클래스는 어노테이션 쪽을 import하고, 래퍼 클래스는 전체 경로(FQN)로 사용한다.
 */
@Tag(name = "Sample - Book", description = "Swagger 어노테이션 사용법을 보여주는 샘플 도서 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sample/books")
public class SampleBookController {

    private final SampleBookService sampleBookService;

    @Operation(
            summary = "도서 목록 조회",
            description = "키워드/상태로 필터링하고 페이지 단위로 도서 목록을 조회한다. "
                    + "쿼리 파라미터는 @ParameterObject로 묶인 BookSearchCondition을 사용한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public com.onestar.aicc.commons.response.ApiResponse<PageResponse<BookResponse>> getBooks(
            @ParameterObject BookSearchCondition condition
    ) {
        return com.onestar.aicc.commons.response.ApiResponse.success(sampleBookService.search(condition));
    }

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
    public com.onestar.aicc.commons.response.ApiResponse<BookResponse> getBook(
            @Parameter(description = "도서 ID", example = "1", required = true)
            @PathVariable Long bookId
    ) {
        return com.onestar.aicc.commons.response.ApiResponse.success(sampleBookService.getOne(bookId));
    }

    @Operation(summary = "도서 등록", description = "새 도서를 등록한다. 요청 본문은 @Valid로 검증된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public com.onestar.aicc.commons.response.ApiResponse<BookResponse> createBook(
            @Valid @RequestBody BookRequest request
    ) {
        return com.onestar.aicc.commons.response.ApiResponse.success("도서가 등록되었습니다.", sampleBookService.register(request));
    }

    @Operation(summary = "도서 수정", description = "bookId에 해당하는 도서 정보를 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음")
    })
    @PutMapping("/{bookId}")
    public com.onestar.aicc.commons.response.ApiResponse<BookResponse> updateBook(
            @Parameter(description = "도서 ID", example = "1", required = true) @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request
    ) {
        return com.onestar.aicc.commons.response.ApiResponse.success("도서가 수정되었습니다.", sampleBookService.update(bookId, request));
    }

    @Operation(summary = "도서 삭제", description = "bookId에 해당하는 도서를 삭제한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{bookId}")
    public void deleteBook(
            @Parameter(description = "도서 ID", example = "1", required = true) @PathVariable Long bookId
    ) {
        sampleBookService.delete(bookId);
    }

    @Operation(
            summary = "인증이 필요한 API 문서화 예시",
            description = "실제 인증 로직은 적용되어 있지 않다. @SecurityRequirement로 Swagger UI에 "
                    + "'Authorize' 잠금 아이콘과 Bearer 토큰 입력창을 노출하는 방법만 보여주는 예시이다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/secure-example")
    public com.onestar.aicc.commons.response.ApiResponse<String> secureExample() {
        return com.onestar.aicc.commons.response.ApiResponse.success("이 응답은 실제 인증 검증 없이 반환됩니다 (문서화 예시 전용).");
    }

    @Deprecated
    @Operation(
            summary = "[Deprecated] 구버전 도서 개수 조회",
            description = "더 이상 사용되지 않는 API 예시. Swagger UI에 취소선과 경고 배지로 표시된다.",
            deprecated = true
    )
    @GetMapping("/legacy-count")
    public com.onestar.aicc.commons.response.ApiResponse<Integer> legacyCount() {
        return com.onestar.aicc.commons.response.ApiResponse.success(
                sampleBookService.search(new BookSearchCondition()).getContent().size()
        );
    }
}
