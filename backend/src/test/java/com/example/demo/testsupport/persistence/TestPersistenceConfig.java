package com.example.demo.testsupport.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Test configuration for schema-per-test isolation in integration tests.
 *
 * <p>This configuration provides:</p>
 * <ul>
 *   <li>SmartRoutingDataSource wrapper for schema-based connection routing</li>
 *   <li>TestSchemaFilter to propagate X-Test-Schema header to SchemaContext</li>
 *   <li>SchemaContextTaskDecorator to propagate schema context to async threads</li>
 *   <li>Custom applicationTaskExecutor with schema-aware TaskDecorator</li>
 * </ul>
 *
 * <p><strong>Spring Modulith Integration:</strong></p>
 * <p>{@code @ApplicationModuleListener} is equivalent to {@code @Async @TransactionalEventListener}.
 * Spring Modulith 2.x uses the standard {@code applicationTaskExecutor} bean for async event
 * processing. The {@code @Primary} annotation ensures our decorated executor takes precedence
 * over any auto-configured executors.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("integration")
@EnableAsync
public class TestPersistenceConfig {

    @Bean
    static BeanPostProcessor dataSourceSchemaRoutingPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof DataSource dataSource)) {
                    return bean;
                }

                if (!"dataSource".equals(beanName)) {
                    return bean;
                }

                if (dataSource instanceof SmartRoutingDataSource) {
                    return bean;
                }

                return new SmartRoutingDataSource(dataSource);
            }
        };
    }

    @Bean
    FilterRegistrationBean<TestSchemaFilter> testSchemaFilter() {
        FilterRegistrationBean<TestSchemaFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TestSchemaFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean
    TaskDecorator schemaContextTaskDecorator() {
        return new SchemaContextTaskDecorator();
    }

    /**
     * Custom task executor with schema context propagation for async operations.
     *
     * <p>Spring Modulith's {@code @ApplicationModuleListener} uses {@code @Async} internally,
     * which delegates to this executor. The {@code @Primary} annotation ensures this bean
     * takes precedence over Spring Boot's auto-configured {@code TaskExecutionAutoConfiguration}.</p>
     *
     * <p>The TaskDecorator captures the schema context from the submitting thread and
     * applies it to the async execution thread, ensuring database writes go to the
     * correct test schema.</p>
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    ThreadPoolTaskExecutor applicationTaskExecutor(TaskDecorator taskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator);
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("it-exec-");
        executor.initialize();
        return executor;
    }

}
