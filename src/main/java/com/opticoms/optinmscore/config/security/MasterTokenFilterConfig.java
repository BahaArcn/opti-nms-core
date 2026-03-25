package com.opticoms.optinmscore.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class MasterTokenFilterConfig {

    @Value("${app.master-token}")
    private String masterToken;

    @Bean
    public FilterRegistrationBean<MasterTokenFilter> masterTokenFilterRegistration() {
        FilterRegistrationBean<MasterTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MasterTokenFilter(masterToken));
        registration.addUrlPatterns("/api/v1/slave/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("masterTokenFilter");
        return registration;
    }
}
