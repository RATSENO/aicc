package com.onestar.aicc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 실제 DB 연결 없이 애플리케이션 컨텍스트 로딩만 검증한다.
 * DataSource/MyBatis 관련 AutoConfiguration은 초기 셋업 단계에서
 * 로컬 MariaDB가 없어도 컨텍스트가 뜨는지 확인하기 위해 제외한다.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
                "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class AiccApplicationTests {

    @Test
    void contextLoads() {
    }

}
