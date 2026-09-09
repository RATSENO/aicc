package com.onestar.aicc.commons.audit;

/**
 * 현재 요청을 처리한 "행위자(actor)"의 id를 스레드 단위로 보관한다. 등록자/수정자 컬럼을 채울 때
 * 서비스 코드가 이 클래스를 통해 값을 읽으면 되고, 실제 판정(콜봇/챗봇 구분 등)은
 * {@link AuditContextInterceptor}가 요청당 한 번만 수행해서 여기에 세팅해준다.
 */
public final class AuditContext {

    /** 인터셉터가 전혀 실행되지 않은 상황(단위 테스트, 향후 배치 등)에서의 최후 안전장치 기본값. */
    private static final String FALLBACK_ACTOR_ID = "AICC_BOT";

    private static final ThreadLocal<String> ACTOR_ID_HOLDER = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void setActorId(String actorId) {
        ACTOR_ID_HOLDER.set(actorId);
    }

    public static String getActorId() {
        String actorId = ACTOR_ID_HOLDER.get();
        return actorId != null ? actorId : FALLBACK_ACTOR_ID;
    }

    /**
     * Tomcat은 워커 스레드를 요청마다 재사용하므로, 요청이 끝나면 반드시 호출해야 한다.
     * {@code set(null)}은 ThreadLocalMap에 엔트리를 남겨두므로 누수 방지 효과가 없다 — 반드시 {@code remove()}.
     */
    public static void clear() {
        ACTOR_ID_HOLDER.remove();
    }
}
