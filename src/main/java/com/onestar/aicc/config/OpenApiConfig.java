package com.onestar.aicc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AICC API")
                        .description("AICC Interface Server API")
                        .version("0.0.1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME_NAME, bearerAuthScheme()));
    }

    /**
     * Swagger UI의 Authorize 버튼에 Bearer 토큰 입력창을 노출하기 위한 SecurityScheme 정의.
     * 컨트롤러 메서드에서 @SecurityRequirement(name = "bearerAuth")로 참조한다.
     * 실제 인증 필터/인터셉터는 별도로 구현해야 하며, 이 설정은 문서화 목적으로만 존재한다.
     */
    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    /**
     * springdoc은 GroupedOpenApi 빈이 하나라도 정의되면 Swagger UI 드롭다운에 해당 그룹들만 노출하고,
     * 전체 API를 보여주는 기본 그룹은 자동으로 추가해주지 않는다. 따라서 그룹을 나누더라도 전체 API를
     * 한눈에 보기 위한 "all" 그룹을 함께 등록해둔다.
     */
    @Bean
    public GroupedOpenApi allGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }

    /**
     * Swagger UI 상단 그룹 드롭다운에서 sample 패키지 API만 따로 모아 보기 위한 그룹 설정 예시.
     */
    @Bean
    public GroupedOpenApi sampleGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("sample")
                .pathsToMatch("/api/v1/sample/**")
                .build();
    }
}
