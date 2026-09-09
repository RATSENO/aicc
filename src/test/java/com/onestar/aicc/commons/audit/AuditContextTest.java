package com.onestar.aicc.commons.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditContextTest {

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    @Test
    void getActorId_returnsFallback_whenNothingSet() {
        assertThat(AuditContext.getActorId()).isEqualTo("AICC_BOT");
    }

    @Test
    void getActorId_returnsSetValue_afterSetActorId() {
        AuditContext.setActorId("AICC_CALLBOT");

        assertThat(AuditContext.getActorId()).isEqualTo("AICC_CALLBOT");
    }

    @Test
    void clear_resetsToFallback() {
        AuditContext.setActorId("AICC_CHATBOT");

        AuditContext.clear();

        assertThat(AuditContext.getActorId()).isEqualTo("AICC_BOT");
    }
}
