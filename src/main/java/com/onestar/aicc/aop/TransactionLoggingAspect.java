package com.onestar.aicc.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * `@Transactional` 메소드가 호출될 때마다 "이번 호출이 새 트랜잭션을 시작하는 건지, 이미 진행 중인
 * 트랜잭션에 그냥 참여하는 건지"와 "결과적으로 커밋됐는지 롤백됐는지"를 로그로 남겨주는 애스펙트다.
 *
 * 쉽게 말하면: 서비스 메소드끼리 서로 호출하는 구조에서 트랜잭션이 하나로 묶이는지 따로 도는지,
 * 언제 롤백이 발생하는지를 로그만 보고 바로 파악할 수 있게 해준다.
 *
 * <p>단, 아무 서비스에서나 다 로그를 남기지는 않는다. {@link TransactionTrace}가 붙은 "최초 진입"
 * 메소드가 호출된 경우에만 로그가 켜지고, 그 호출 안에서 연쇄적으로 실행되는 다른 `@Transactional`
 * 메소드들까지 같은 흐름으로 묶여서 로그에 남는다. 확인이 필요한 서비스에만 애노테이션을 붙여서 켜면 된다.
 *
 * (구현 참고) `@Order(100)`을 명시한 이유는 Spring의 트랜잭션 처리 로직보다 이 애스펙트가 항상
 * "바깥쪽"에서 감싸도록 순서를 고정하기 위함이다 — 그래야 트랜잭션이 이미 시작/종료된 뒤의 상태를
 * 보고 정확하게 판정할 수 있다.
 */
@Aspect
@Component
@Order(100)
@Slf4j
public class TransactionLoggingAspect {

    private final AnnotationTransactionAttributeSource transactionAttributeSource =
            new AnnotationTransactionAttributeSource();

    private static final AtomicLong TRACE_SEQ = new AtomicLong();
    private static final ThreadLocal<Deque<String>> TRACE_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private enum BoundaryKind {
        NEW, JOINED, SUSPENDED_NEW, NON_TRANSACTIONAL, UNCLASSIFIED
    }

    /**
     * `@Transactional` 메소드가 호출될 때마다 실행되는 로깅 본체.
     * 새 트랜잭션 시작인지 기존 트랜잭션 참여인지 판정해서 진입 로그를 남기고,
     * 메소드 실행이 끝나면 커밋됐는지 롤백됐는지를 로그로 남긴다.
     */
    @Around("execution(* com.onestar.aicc..*(..)) && "
            + "(@annotation(org.springframework.transaction.annotation.Transactional) || "
            + " @within(org.springframework.transaction.annotation.Transactional))")
    public Object logTransactionBoundary(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // 이 메소드에 @TransactionTrace가 직접 붙어있거나(=최초 진입 지점), 이미 추적 중인 호출
        // 체인 안쪽(스택이 비어있지 않음)이 아니면 아무것도 하지 않고 그냥 통과시킨다.
        boolean armed = method.isAnnotationPresent(TransactionTrace.class) || !TRACE_STACK.get().isEmpty();
        if (!armed) {
            return pjp.proceed();
        }

        Class<?> targetClass = pjp.getTarget().getClass();
        TransactionAttribute txAttr = transactionAttributeSource.getTransactionAttribute(method, targetClass);
        if (txAttr == null) {
            return pjp.proceed();
        }

        String label = signature.toShortString();
        boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
        BoundaryKind kind = classify(txAttr.getPropagationBehavior(), wasActive);

        Deque<String> stack = TRACE_STACK.get();
        boolean pushed = false;
        String traceId;
        if (kind == BoundaryKind.NEW || kind == BoundaryKind.SUSPENDED_NEW) {
            // 활성화된 트랜잭션이 없던 상태에서 호출됐으므로, 이 호출이 새 물리 트랜잭션을 시작한다.
            traceId = "tx-" + TRACE_SEQ.incrementAndGet();
            stack.push(traceId);
            pushed = true;
            log.info("[{}] 트랜잭션 시작({}) {} 전파방식={} 읽기전용={} 스레드={}",
                    traceId, kind, label, propagationName(txAttr.getPropagationBehavior()),
                    txAttr.isReadOnly(), Thread.currentThread().getName());
        } else if (kind == BoundaryKind.JOINED) {
            // 이미 진행 중인 트랜잭션이 있어서, 새로 시작하지 않고 거기에 그냥 묶여 들어간다.
            traceId = stack.peek();
            log.info("[{}] 기존 트랜잭션에 참여 {} 전파방식={} 스레드={}",
                    traceId, label, propagationName(txAttr.getPropagationBehavior()), Thread.currentThread().getName());
        } else {
            // 이 호출은 실제 트랜잭션 없이(또는 판정 불가 상태로) 실행된다.
            traceId = stack.peek();
            log.debug("(활성 트랜잭션 없음, 종류={}) {} 전파방식={} — 트랜잭션 없이 실행됨",
                    kind, label, propagationName(txAttr.getPropagationBehavior()));
        }

        long startNanos = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (pushed) {
                // 이 호출이 물리 트랜잭션의 경계였으므로, 여기서 실제 커밋이 일어난 것이다.
                log.info("[{}] 커밋 완료 {} 소요시간={}ms", traceId, label, elapsedMs);
            } else if (kind == BoundaryKind.JOINED) {
                // 참여 호출은 정상 종료됐을 뿐, 실제 커밋 여부는 바깥쪽(물리 경계) 호출의 로그에서 확인해야 한다.
                log.debug("[{}] 참여 호출 정상 종료 {} 소요시간={}ms", traceId, label, elapsedMs);
            }
            return result;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (pushed) {
                // 물리 트랜잭션 경계에서 예외가 터졌으므로, Spring의 롤백 규칙(rollbackFor 등)을 그대로
                // 반영해서 실제로 롤백될지 커밋될지를 판정해 로그로 남긴다.
                boolean willRollback = txAttr.rollbackOn(ex);
                log.info("[{}] {} {} 소요시간={}ms 예외={}: {}",
                        traceId, willRollback ? "롤백" : "커밋(롤백 대상이 아닌 예외)",
                        label, elapsedMs, ex.getClass().getName(), ex.getMessage());
            } else if (kind == BoundaryKind.JOINED) {
                // 참여 호출 중 예외가 발생했다 — 최종 롤백 여부는 바깥쪽 물리 트랜잭션이 결정하지만,
                // 이 예외가 그 트랜잭션을 롤백 전용으로 만들 가능성이 있는지는 여기서 미리 판정해 둔다.
                boolean willRollback = txAttr.rollbackOn(ex);
                log.info("[{}] 참여 호출 중 예외 발생 {} 소요시간={}ms 예외={} 외부트랜잭션롤백유발여부={}",
                        traceId, label, elapsedMs, ex.getClass().getSimpleName(), willRollback);
            } else {
                log.warn("{} 진입 중 예외 발생(전파방식={}): {}",
                        label, propagationName(txAttr.getPropagationBehavior()), ex.toString());
            }
            throw ex;
        } finally {
            if (pushed) {
                stack.pop();
                if (stack.isEmpty()) {
                    TRACE_STACK.remove();
                }
            }
        }
    }

    /**
     * propagation 설정과 "호출 전에 이미 활성화된 트랜잭션이 있었는지"를 조합해서
     * 이번 호출이 신규/참여/일시중단후신규/비트랜잭션 중 어디에 해당하는지 분류한다.
     */
    private BoundaryKind classify(int propagation, boolean wasActive) {
        switch (propagation) {
            case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
                return wasActive ? BoundaryKind.SUSPENDED_NEW : BoundaryKind.NEW;
            case TransactionDefinition.PROPAGATION_SUPPORTS:
            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED:
            case TransactionDefinition.PROPAGATION_NEVER:
                return wasActive ? BoundaryKind.JOINED : BoundaryKind.NON_TRANSACTIONAL;
            case TransactionDefinition.PROPAGATION_MANDATORY:
                return wasActive ? BoundaryKind.JOINED : BoundaryKind.UNCLASSIFIED;
            case TransactionDefinition.PROPAGATION_REQUIRED:
            case TransactionDefinition.PROPAGATION_NESTED:
            default:
                return wasActive ? BoundaryKind.JOINED : BoundaryKind.NEW;
        }
    }

    /** propagation 정수 상수를 로그에 보기 좋은 이름(REQUIRED, REQUIRES_NEW 등)으로 바꿔준다. */
    private String propagationName(int propagation) {
        switch (propagation) {
            case TransactionDefinition.PROPAGATION_REQUIRED:
                return "REQUIRED";
            case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
                return "REQUIRES_NEW";
            case TransactionDefinition.PROPAGATION_SUPPORTS:
                return "SUPPORTS";
            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED:
                return "NOT_SUPPORTED";
            case TransactionDefinition.PROPAGATION_NEVER:
                return "NEVER";
            case TransactionDefinition.PROPAGATION_MANDATORY:
                return "MANDATORY";
            case TransactionDefinition.PROPAGATION_NESTED:
                return "NESTED";
            default:
                return "UNKNOWN(" + propagation + ")";
        }
    }
}
