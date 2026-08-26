package com.onestar.aicc.sample.mapper;

import com.onestar.aicc.sample.dto.BookSearchCondition;
import com.onestar.aicc.sample.domain.BookEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PROJECT.md §8의 mapper 컨벤션(MyBatis Mapper Interface, DB SQL 호출만 담당)을 따르는
 * sample 패키지 전용 Mapper. 실제 SQL은 resources/mapper/sample/SampleBookMapper.xml에 있다.
 */
@Mapper
public interface SampleBookMapper {

    List<BookEntity> selectBooks(@Param("condition") BookSearchCondition condition,
                                  @Param("offset") int offset);

    long countBooks(@Param("condition") BookSearchCondition condition);

    BookEntity selectBookById(@Param("bookId") Long bookId);

    int insertBook(BookEntity book);

    int updateBook(BookEntity book);

    int deleteBookById(@Param("bookId") Long bookId);
}
