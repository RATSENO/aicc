package com.onestar.aicc.commons.audit;

import lombok.Getter;
import lombok.Setter;

/**
 * MyBatis insert/update 대상 domain 엔티티가 상속받는 공통 베이스 클래스.
 * regId(등록자)/modId(수정자) 두 필드만 제공하며, 실제 값은 서비스 코드가 아니라
 * {@link AuditColumnMyBatisInterceptor}가 insert/update 실행 시점에 자동으로 채운다.
 *
 * 일부러 Lombok @Builder/@AllArgsConstructor/@NoArgsConstructor를 붙이지 않는다 — regId/modId는
 * 호출자가 빌더 체인으로 세팅할 값이 아니라 인터셉터가 세팅할 값이므로, 빌더에 노출되지 않는 것이
 * 오히려 의도된 동작이다. 하위 클래스의 기존 plain @Builder는 상속 필드를 보지 않으므로 그대로 동작한다.
 */
@Getter
@Setter
public abstract class BaseAuditEntity {

    /** 등록자(actor id). insert 시점에만 세팅되고, 이후 update에서는 절대 덮어쓰지 않는다. */
    private String regId;

    /** 수정자(actor id). insert/update 시점마다 항상 최신 actor id로 세팅된다. */
    private String modId;
}
