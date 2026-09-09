package com.onestar.aicc.commons.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 요청마다 한 번, 호출한 채널(콜봇/챗봇 등)을 헤더로 판정해서 {@link AuditContext}에 세팅해준다.
 * 판정 실패가 요청 자체를 막아서는 안 되므로, 실패 시 항상 기본 actor id로 대체하고 요청은 그대로 진행시킨다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditContextInterceptor implements HandlerInterceptor {

    private final AuditProperties auditProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String actorId;
        try {
            actorId = resolveActorId(request.getHeader(auditProperties.getChannelHeader()));
        } catch (Exception ex) {
            log.warn("감사 컨텍스트 판정 중 예외 발생, 기본 actor id로 대체합니다: {}", ex.toString());
            actorId = auditProperties.getDefaultActorId();
        }
        AuditContext.setActorId(actorId);
        log.debug("감사 컨텍스트 설정: uri={}, actorId={}", request.getRequestURI(), actorId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        AuditContext.clear();
    }

    private String resolveActorId(String channelHeaderValue) {
        if (channelHeaderValue == null) {
            return auditProperties.getDefaultActorId();
        }
        return auditProperties.getChannelActorMap()
                .getOrDefault(channelHeaderValue.trim().toUpperCase(), auditProperties.getDefaultActorId());
    }
}
