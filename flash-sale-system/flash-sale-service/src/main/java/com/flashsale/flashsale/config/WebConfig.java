package com.flashsale.flashsale.config;

import com.flashsale.flashsale.ai.QpsCollector;
import com.flashsale.flashsale.filter.QpsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<QpsFilter> qpsFilter(QpsCollector qpsCollector) {
        FilterRegistrationBean<QpsFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new QpsFilter(qpsCollector));
        bean.addUrlPatterns("/api/flash-sale/*", "/api/ai/*");
        bean.setOrder(1);
        return bean;
    }
}