package com.gdg.unimatebackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    /**
     * local / prod 중 현재 프로파일
     */
    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * 운영 서버 URL (프로파일별로 application.yml / application-prod.yml에서 주입)
     * - local: http://localhost:8080 (기본)
     * - prod : http://seok-hwan1.duckdns.org (기본)
     */
    @Value("${app.swagger.prod-url:http://localhost:8080}")
    private String prodUrl;

    @Bean
    public OpenAPI openAPI() {
        // ✅ Security Scheme (JWT Bearer Token)
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        // ✅ Servers (환경별)
        List<Server> servers = new ArrayList<>();

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 서버");

        Server prodServer = new Server()
                .url(prodUrl)
                .description("운영 서버");

        if ("local".equalsIgnoreCase(activeProfile)) {
            // 로컬에서는 로컬 + 운영 둘 다 보여주기
            servers.add(localServer);
            servers.add(prodServer);
        } else {
            // prod에서는 운영만
            servers.add(prodServer);
        }

        // ✅ API Info
        Info info = new Info()
                .title("Unimate API Documentation")
                .version("1.0.0")
                .description("Unimate 백엔드 API 문서")
                .contact(new Contact()
                        .name("tripleS")
                        .email("seokhawnkim@gmail.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
