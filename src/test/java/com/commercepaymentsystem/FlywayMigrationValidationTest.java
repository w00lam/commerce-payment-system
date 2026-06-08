package com.commercepaymentsystem;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Flyway 마이그레이션 스크립트(V1__init.sql 등)와 JPA 엔티티 간의 스키마 정합성을 검증하는 테스트입니다.
 * ddl-auto: validate 환경에서 수동으로 Flyway 마이그레이션을 실행한 후 Spring Context 로딩이 성공하는지 확인합니다.
 */
@ActiveProfiles("test")
@Import(FlywayMigrationValidationTest.TestConfig.class)
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.datasource.url=jdbc:h2:mem:validationdb;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class FlywayMigrationValidationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Flyway flyway(DataSource dataSource) {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
            flyway.migrate();
            return flyway;
        }

        @Bean
        public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlywayPostProcessor() {
            return beanFactory -> {
                try {
                    BeanDefinition bd = beanFactory.getBeanDefinition("entityManagerFactory");
                    String[] dependsOn = bd.getDependsOn();
                    String[] newDependsOn = dependsOn == null ? new String[]{"flyway"} :
                        Stream.concat(Arrays.stream(dependsOn), Stream.of("flyway")).toArray(String[]::new);
                    bd.setDependsOn(newDependsOn);
                } catch (NoSuchBeanDefinitionException e) {
                    // ignore
                }
            };
        }
    }

    @Test
    void validateFlywayAndJpaSchema() {
        // Spring Context가 정상 로드되고 DDL 검증(validate)을 통과하면 본 테스트가 성공합니다.
    }
}
