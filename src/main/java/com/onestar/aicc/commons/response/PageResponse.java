package com.onestar.aicc.commons.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "공통 페이지네이션 응답 포맷")
public class PageResponse<T> {

    @Schema(description = "조회된 데이터 목록")
    private final List<T> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private final int page;

    @Schema(description = "페이지당 데이터 개수", example = "10")
    private final int size;

    @Schema(description = "전체 데이터 개수", example = "42")
    private final long totalElements;

    @Schema(description = "전체 페이지 개수", example = "5")
    private final int totalPages;

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
