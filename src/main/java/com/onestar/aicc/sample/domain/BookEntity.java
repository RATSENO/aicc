package com.onestar.aicc.sample.domain;

import com.onestar.aicc.sample.dto.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * H2(MyBatis) 테이블 BOOK에 매핑되는 도메인 객체.
 * 실제 업무 도메인이 아닌 sample 패키지 전용 학습용 엔티티다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookEntity {

    private Long bookId;
    private String title;
    private String author;
    private Integer price;
    private BookStatus status;
    private LocalDateTime createdAt;
}
