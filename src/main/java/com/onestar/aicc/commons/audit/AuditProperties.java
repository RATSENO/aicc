package com.onestar.aicc.commons.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * `aicc.audit.*` 설정값을 바인딩한다. AICC와 채널 구분 헤더 이름/값이 아직 확정 전이므로, 기본값을
 * 코드에 하드코딩하지 않고 application.yml만 고치면 되도록 한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aicc.audit")
public class AuditProperties {

    /** AICC가 콜봇/챗봇 구분을 실어 보내는 HTTP 헤더 이름. */
    private String channelHeader;

    /** 헤더 값(대문자 기준) → 등록자/수정자 컬럼에 쓸 actor id 매핑. */
    private Map<String, String> channelActorMap = new LinkedHashMap<>();

    /** 헤더가 없거나 매핑되지 않은 값일 때 사용할 기본 actor id. */
    private String defaultActorId;
}
