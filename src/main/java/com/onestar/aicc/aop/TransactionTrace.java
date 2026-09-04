package com.onestar.aicc.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 트랜잭션 경계 로그(TransactionLoggingAspect)를 켜고 싶은 "최초 진입" 서비스 메소드에 붙이는 마커
 * 애노테이션이다. 이 애노테이션이 붙은 메소드가 호출되면, 그 안에서 연쇄적으로 호출되는 모든
 * `@Transactional` 메소드까지 포함해서 로그가 남는다. 이 애노테이션이 없는 서비스는 아무 로그도
 * 남기지 않는다 — 확인이 필요한 서비스에만 붙여서 켜는 용도다.
 *
 * <p>반드시 `@Transactional`과 함께 사용해야 한다 — 트랜잭션 자체가 없으면 추적할 경계도 없다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionTrace {
}
