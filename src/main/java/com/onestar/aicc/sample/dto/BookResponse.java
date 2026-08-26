package com.onestar.aicc.sample.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "도서 응답")
public class BookResponse {

    @Schema(description = "도서 ID", example = "1")
    private Long bookId;

    @Schema(description = "도서 제목", example = "이펙티브 자바")
    private String title;

    @Schema(description = "저자", example = "Joshua Bloch")
    private String author;

    @Schema(description = "정가(원)", example = "36000")
    private Integer price;

    @Schema(description = "판매 상태", example = "AVAILABLE")
    private BookStatus status;

    @Schema(
            description = "등록 일시 (yyyy-MM-dd'T'HH:mm:ss)",
            example = "2026-08-25T10:15:30",
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static BookResponse of(Long bookId, BookRequest request, LocalDateTime createdAt) {
        return BookResponse.builder()
                .bookId(bookId)
                .title(request.getTitle())
                .author(request.getAuthor())
                .price(request.getPrice())
                .status(request.getStatus())
                .createdAt(createdAt)
                .build();
    }
}
