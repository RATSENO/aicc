package com.onestar.aicc.config;

import com.onestar.aicc.commons.audit.AuditContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditContextInterceptor auditContextInterceptor;

    /**
     * AuditContextInterceptor를 API 경로에만 등록한다. Swagger UI/OpenAPI 문서/H2 콘솔 경로는
     * 감사(audit) 컨텍스트와 무관하므로 명시적으로 제외한다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditContextInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs", "/v3/api-docs/**",
                        "/h2-console/**"
                );
    }
}
