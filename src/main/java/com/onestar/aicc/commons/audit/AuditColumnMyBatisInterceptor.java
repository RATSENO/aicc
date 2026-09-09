package com.onestar.aicc.commons.audit;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

/**
 * INSERT/UPDATE 매퍼 호출을 가로채, 파라미터가 {@link BaseAuditEntity}를 상속받으면
 * regId(등록자)/modId(수정자)를 {@link AuditContext#getActorId()}로 자동 세팅한다.
 *
 * - INSERT: regId, modId 둘 다 세팅.
 * - UPDATE: modId만 세팅. regId(등록자)는 최초 등록 시점 값을 그대로 보존해야 하므로 절대 건드리지 않는다.
 * - DELETE 및 그 외 커맨드: 통과만 시키고 아무것도 하지 않는다.
 *
 * mybatis-spring-boot-starter는 스프링 컨텍스트의 모든 Interceptor 빈을 자동으로 SqlSessionFactory에
 * 등록해주므로, 이 클래스는 @Component만 붙이면 별도 설정 없이 동작한다.
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class AuditColumnMyBatisInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];

        SqlCommandType commandType = mappedStatement.getSqlCommandType();
        if (commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE) {
            BaseAuditEntity entity = resolveAuditEntity(parameter, mappedStatement.getId());
            if (entity != null) {
                String actorId = AuditContext.getActorId();
                if (commandType == SqlCommandType.INSERT) {
                    entity.setRegId(actorId);
                }
                entity.setModId(actorId);
            }
        }
        return invocation.proceed();
    }

    /**
     * @Param 없는 단일 파라미터 mapper 메소드는 파라미터 자체가 BaseAuditEntity 인스턴스다.
     * @Param을 쓰는 메소드는 MyBatis가 파라미터를 Map(ParamMap)으로 감싸므로, 그 안에서
     * BaseAuditEntity 타입 값을 찾는다 — 정확히 하나만 매칭돼야 하고, 0개/2개 이상이면 애매하므로
     * 추측하지 않고 건너뛴다.
     */
    private BaseAuditEntity resolveAuditEntity(Object parameter, String statementId) {
        if (parameter instanceof BaseAuditEntity) {
            return (BaseAuditEntity) parameter;
        }
        if (parameter instanceof Map) {
            BaseAuditEntity found = null;
            for (Object value : ((Map<?, ?>) parameter).values()) {
                if (value instanceof BaseAuditEntity) {
                    if (found != null) {
                        log.warn("[{}] 파라미터에서 BaseAuditEntity 타입 값이 2개 이상 발견되어 "
                                + "감사 컬럼 자동 세팅을 건너뜁니다.", statementId);
                        return null;
                    }
                    found = (BaseAuditEntity) value;
                }
            }
            return found;
        }
        return null;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
