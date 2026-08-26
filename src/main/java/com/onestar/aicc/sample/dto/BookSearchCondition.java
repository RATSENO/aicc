package com.onestar.aicc.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 목록 조회용 쿼리 파라미터 묶음.
 * {@link ParameterObject}를 붙이면 개별 @RequestParam을 나열하지 않고도
 * 필드 단위 @Schema 설명이 Swagger 쿼리 파라미터 목록에 그대로 반영된다.
 */
@Getter
@Setter
@ParameterObject
@Schema(description = "도서 목록 조회 조건")
public class BookSearchCondition {

    @Schema(description = "제목/저자 검색 키워드", example = "이펙티브")
    private String keyword;

    @Schema(description = "판매 상태 필터", example = "AVAILABLE")
    private BookStatus status;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    private int page = 0;

    @Schema(description = "페이지당 개수", example = "10", defaultValue = "10")
    private int size = 10;
}
