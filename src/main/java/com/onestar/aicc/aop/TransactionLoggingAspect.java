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
 * {@code com.onestar.aicc} 하위의 {@code @Transactional} 메소드 호출마다 트랜잭션 경계 상태(신규 시작인지
 * 기존 트랜잭션에 참여하는지, 커밋/롤백이 언제 일어나는지)를 로그로 남긴다.
 *
 * <p>{@code @Order}를 명시하지 않으면 Spring의 트랜잭션 어드바이저({@code BeanFactoryTransactionAttributeSourceAdvisor},
 * 기본값 {@code Ordered.LOWEST_PRECEDENCE})와 이 애스펙트의 상대 순서가 정의되지 않는다. 유한한 order 값을 주면
 * 이 애스펙트가 항상 트랜잭션 인터셉터 바깥쪽을 감싸는 것이 보장되어, {@link #proceed} 시점에는 이미 내부에서
 * 트랜잭션 시작/커밋/롤백이 끝난 뒤이므로 {@code TransactionSynchronizationManager} 상태만으로 판정할 수 있다.
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
            traceId = "tx-" + TRACE_SEQ.incrementAndGet();
            stack.push(traceId);
            pushed = true;
            log.info("[{}] BEGIN({}) {} propagation={} readOnly={} thread={}",
                    traceId, kind, label, propagationName(txAttr.getPropagationBehavior()),
                    txAttr.isReadOnly(), Thread.currentThread().getName());
        } else if (kind == BoundaryKind.JOINED) {
            traceId = stack.peek();
            log.info("[{}] JOIN(existing tx) {} propagation={} thread={}",
                    traceId, label, propagationName(txAttr.getPropagationBehavior()), Thread.currentThread().getName());
        } else {
            traceId = stack.peek();
            log.debug("(no active tx, kind={}) {} propagation={} — running non-transactionally",
                    kind, label, propagationName(txAttr.getPropagationBehavior()));
        }

        long startNanos = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (pushed) {
                log.info("[{}] COMMIT {} elapsedMs={}", traceId, label, elapsedMs);
            } else if (kind == BoundaryKind.JOINED) {
                log.debug("[{}] PARTICIPATE-OK {} elapsedMs={}", traceId, label, elapsedMs);
            }
            return result;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (pushed) {
                boolean willRollback = txAttr.rollbackOn(ex);
                log.info("[{}] {} {} elapsedMs={} exception={}: {}",
                        traceId, willRollback ? "ROLLBACK" : "COMMIT(non-rollback exception)",
                        label, elapsedMs, ex.getClass().getName(), ex.getMessage());
            } else if (kind == BoundaryKind.JOINED) {
                boolean willRollback = txAttr.rollbackOn(ex);
                log.info("[{}] PARTICIPATE-EXCEPTION {} elapsedMs={} exception={} willMarkOuterRollbackOnly={}",
                        traceId, label, elapsedMs, ex.getClass().getSimpleName(), willRollback);
            } else {
                log.warn("{} threw while entering propagation={}: {}",
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
