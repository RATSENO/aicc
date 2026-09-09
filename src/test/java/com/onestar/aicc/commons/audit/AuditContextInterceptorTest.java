package com.onestar.aicc.commons.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditContextInterceptorTest {

    private static final String CHANNEL_HEADER = "X-AICC-Channel";
    private static final String DEFAULT_ACTOR_ID = "AICC_BOT";

    private AuditContextInterceptor interceptor;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        AuditProperties properties = new AuditProperties();
        properties.setChannelHeader(CHANNEL_HEADER);
        Map<String, String> channelActorMap = new LinkedHashMap<>();
        channelActorMap.put("CALLBOT", "AICC_CALLBOT");
        channelActorMap.put("CHATBOT", "AICC_CHATBOT");
        properties.setChannelActorMap(channelActorMap);
        properties.setDefaultActorId(DEFAULT_ACTOR_ID);

        interceptor = new AuditContextInterceptor(properties);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    @Test
    void preHandle_setsMappedActorId_forKnownChannelHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CHANNEL_HEADER, "CALLBOT");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(AuditContext.getActorId()).isEqualTo("AICC_CALLBOT");
    }

    @Test
    void preHandle_fallsBackToDefault_whenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        interceptor.preHandle(request, response, new Object());

        assertThat(AuditContext.getActorId()).isEqualTo(DEFAULT_ACTOR_ID);
    }

    @Test
    void preHandle_fallsBackToDefault_whenHeaderValueUnrecognized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CHANNEL_HEADER, "UNKNOWN_CHANNEL");

        interceptor.preHandle(request, response, new Object());

        assertThat(AuditContext.getActorId()).isEqualTo(DEFAULT_ACTOR_ID);
    }

    @Test
    void afterCompletion_clearsContext_evenWhenExceptionPassed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CHANNEL_HEADER, "CHATBOT");
        interceptor.preHandle(request, response, new Object());
        assertThat(AuditContext.getActorId()).isEqualTo("AICC_CHATBOT");

        interceptor.afterCompletion(request, response, new Object(), new RuntimeException("boom"));

        assertThat(AuditContext.getActorId()).isEqualTo(DEFAULT_ACTOR_ID);
    }
}
