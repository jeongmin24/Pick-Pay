package com.miniproject.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
/**
 * @EnableJpaAuditing: 자동 감시 기능 켜기
 * @CreatedDate, @LastModifiedDate: 어떤 필드에 넣을지 지정
 * */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
