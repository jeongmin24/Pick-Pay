package com.ssafy.pickpay.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String APPLICATION_JSON = "application/json";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .path("/login", loginPath())
                .info(new Info()
                        .title("PickPay API 명세서")
                        .description("SSAFY PickPay 프로젝트의 유저 및 결제 관련 API 명세서입니다.")
                        .version("v1.0.0"));
    }

    private PathItem loginPath() {
        return new PathItem()
                .post(new Operation()
                        .addTagsItem("Auth")
                        .summary("로그인")
                        .description("loginId와 password로 로그인하고 JWT 토큰을 발급받습니다.")
                        .security(Collections.emptyList())
                        .requestBody(new RequestBody()
                                .required(true)
                                .content(jsonContent(loginRequestSchema())))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("로그인 성공")
                                        .content(jsonContent(tokenResponseSchema())))
                                .addApiResponse("401", new ApiResponse()
                                        .description("로그인 실패"))));
    }

    private Content jsonContent(ObjectSchema schema) {
        return new Content()
                .addMediaType(APPLICATION_JSON, new MediaType().schema(schema));
    }

    private ObjectSchema loginRequestSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("loginId", new StringSchema().example("user1"));
        schema.addProperty("password", new PasswordSchema().example("1234"));
        schema.addRequiredItem("loginId");
        schema.addRequiredItem("password");
        return schema;
    }

    private ObjectSchema tokenResponseSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("accessToken", new StringSchema().example("eyJhbGciOi..."));
        schema.addProperty("refreshToken", new StringSchema().example("eyJhbGciOi..."));
        return schema;
    }
}
