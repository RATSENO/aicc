package com.onestar.aicc.sample.service;

import com.onestar.aicc.commons.response.PageResponse;
import com.onestar.aicc.sample.domain.BookEntity;
import com.onestar.aicc.sample.dto.BookRequest;
import com.onestar.aicc.sample.dto.BookResponse;
import com.onestar.aicc.sample.dto.BookSearchCondition;
import com.onestar.aicc.sample.exception.SampleNotFoundException;
import com.onestar.aicc.sample.mapper.SampleBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * H2(MyBatis) 기반으로 실제 DB에 저장/조회되는 샘플 서비스.
 * PROJECT.md §17 코딩 규칙에 따라 Mapper 호출만 담당하고 업무 로직은 최소한으로 유지한다.
 */
@Service
@RequiredArgsConstructor
public class SampleBookService {

    private final SampleBookMapper sampleBookMapper;

    public PageResponse<BookResponse> search(BookSearchCondition condition) {
        int offset = condition.getPage() * condition.getSize();
        List<BookResponse> content = sampleBookMapper.selectBooks(condition, offset).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long totalElements = sampleBookMapper.countBooks(condition);
        return new PageResponse<>(content, condition.getPage(), condition.getSize(), totalElements);
    }

    public BookResponse getOne(Long bookId) {
        return toResponse(findEntityOrThrow(bookId));
    }

    public BookResponse register(BookRequest request) {
        BookEntity entity = BookEntity.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .price(request.getPrice())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .build();
        sampleBookMapper.insertBook(entity);
        return toResponse(entity);
    }

    @Transactional
    public BookResponse update(Long bookId, BookRequest request) {
        BookEntity existing = findEntityOrThrow(bookId);
        BookEntity updated = BookEntity.builder()
                .bookId(bookId)
                .title(request.getTitle())
                .author(request.getAuthor())
                .price(request.getPrice())
                .status(request.getStatus())
                .createdAt(existing.getCreatedAt())
                .build();
        sampleBookMapper.updateBook(updated);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long bookId) {
        findEntityOrThrow(bookId);
        sampleBookMapper.deleteBookById(bookId);
    }

    private BookEntity findEntityOrThrow(Long bookId) {
        BookEntity entity = sampleBookMapper.selectBookById(bookId);
        if (entity == null) {
            throw new SampleNotFoundException("도서를 찾을 수 없습니다. bookId=" + bookId);
        }
        return entity;
    }

    private BookResponse toResponse(BookEntity entity) {
        return BookResponse.builder()
                .bookId(entity.getBookId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .price(entity.getPrice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
