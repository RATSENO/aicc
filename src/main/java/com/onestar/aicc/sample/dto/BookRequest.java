package com.onestar.aicc.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "도서 등록/수정 요청")
public class BookRequest {

    @NotBlank(message = "제목은 필수값입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    @Schema(description = "도서 제목", example = "이펙티브 자바", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "저자는 필수값입니다.")
    @Schema(description = "저자", example = "Joshua Bloch", requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    @NotNull(message = "가격은 필수값입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    @Schema(description = "정가(원)", example = "36000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer price;

    @NotNull(message = "판매 상태는 필수값입니다.")
    @Schema(description = "판매 상태", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private BookStatus status;
}
