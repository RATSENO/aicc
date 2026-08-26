package com.onestar.aicc.commons.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "공통 에러 응답 포맷")
public class ErrorResponse {

    @Schema(description = "에러 코드", example = "VALIDATION_FAILED")
    private String code;

    @Schema(description = "에러 메시지", example = "요청 값이 올바르지 않습니다.")
    private String message;

    @Schema(description = "필드별 상세 검증 오류 목록 (Bean Validation 실패 시에만 포함)")
    private List<FieldError> errors;

    @Getter
    @Builder
    @Schema(description = "필드 단위 검증 오류")
    public static class FieldError {

        @Schema(description = "오류가 발생한 필드명", example = "title")
        private String field;

        @Schema(description = "거부된 값", example = "")
        private Object rejectedValue;

        @Schema(description = "검증 실패 사유", example = "제목은 필수값입니다.")
        private String reason;
    }
}
