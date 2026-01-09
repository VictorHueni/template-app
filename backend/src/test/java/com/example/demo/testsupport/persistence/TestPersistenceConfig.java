package com.example.demo.testsupport.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("integration")
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

}
