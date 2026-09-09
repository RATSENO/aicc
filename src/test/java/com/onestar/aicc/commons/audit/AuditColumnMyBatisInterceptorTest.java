package com.onestar.aicc.commons.audit;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditColumnMyBatisInterceptorTest {

    private static final String ACTOR_ID = "AICC_CALLBOT";

    private final AuditColumnMyBatisInterceptor interceptor = new AuditColumnMyBatisInterceptor();
    private Executor executor;
    private Method updateMethod;

    @BeforeEach
    void setUp() throws Exception {
        AuditContext.setActorId(ACTOR_ID);
        executor = mock(Executor.class);
        when(executor.update(any(), any())).thenReturn(1);
        updateMethod = Executor.class.getMethod("update", MappedStatement.class, Object.class);
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    @Test
    void insert_setsBothRegIdAndModId() throws Throwable {
        TestEntity entity = new TestEntity();
        Invocation invocation = invocation(SqlCommandType.INSERT, "test.insert", entity);

        interceptor.intercept(invocation);

        assertThat(entity.getRegId()).isEqualTo(ACTOR_ID);
        assertThat(entity.getModId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void update_setsOnlyModId_preservesRegId() throws Throwable {
        TestEntity entity = new TestEntity();
        entity.setRegId("AICC_ORIGINAL");
        Invocation invocation = invocation(SqlCommandType.UPDATE, "test.update", entity);

        interceptor.intercept(invocation);

        assertThat(entity.getRegId()).isEqualTo("AICC_ORIGINAL");
        assertThat(entity.getModId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void delete_doesNotTouchEntity() throws Throwable {
        TestEntity entity = new TestEntity();
        Invocation invocation = invocation(SqlCommandType.DELETE, "test.delete", entity);

        interceptor.intercept(invocation);

        assertThat(entity.getRegId()).isNull();
        assertThat(entity.getModId()).isNull();
    }

    @Test
    void mapParameter_withExactlyOneAuditEntity_getsSet() throws Throwable {
        TestEntity entity = new TestEntity();
        Map<String, Object> paramMap = new LinkedHashMap<>();
        paramMap.put("entity", entity);
        paramMap.put("otherFlag", "not-an-entity");
        Invocation invocation = invocation(SqlCommandType.INSERT, "test.insertWithParam", paramMap);

        interceptor.intercept(invocation);

        assertThat(entity.getRegId()).isEqualTo(ACTOR_ID);
        assertThat(entity.getModId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void mapParameter_withNoAuditEntity_isSkipped() throws Throwable {
        Map<String, Object> paramMap = new LinkedHashMap<>();
        paramMap.put("id", 1L);
        Invocation invocation = invocation(SqlCommandType.INSERT, "test.noEntity", paramMap);

        // 예외 없이 그냥 proceed()만 호출되면 성공
        interceptor.intercept(invocation);
    }

    @Test
    void mapParameter_withTwoAuditEntities_isSkipped() throws Throwable {
        TestEntity first = new TestEntity();
        TestEntity second = new TestEntity();
        Map<String, Object> paramMap = new LinkedHashMap<>();
        paramMap.put("first", first);
        paramMap.put("second", second);
        Invocation invocation = invocation(SqlCommandType.INSERT, "test.twoEntities", paramMap);

        interceptor.intercept(invocation);

        assertThat(first.getRegId()).isNull();
        assertThat(second.getRegId()).isNull();
    }

    private Invocation invocation(SqlCommandType commandType, String statementId, Object parameter) {
        MappedStatement mappedStatement = mappedStatement(commandType, statementId);
        return new Invocation(executor, updateMethod, new Object[]{mappedStatement, parameter});
    }

    /**
     * MappedStatement는 final 클래스라 Mockito로 mock할 수 없으므로, 실제 MyBatis 빌더 API로
     * 최소한의 유효한 인스턴스를 직접 만든다.
     */
    private MappedStatement mappedStatement(SqlCommandType commandType, String statementId) {
        Configuration configuration = new Configuration();
        SqlSource sqlSource = param -> new BoundSql(configuration, "SELECT 1", Collections.emptyList(), param);
        return new MappedStatement.Builder(configuration, statementId, sqlSource, commandType).build();
    }

    private static class TestEntity extends BaseAuditEntity {
    }
}
