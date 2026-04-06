package com.opticoms.optinmscore.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterTokenFilterConfig {

    @Value("${app.master-token}")
    private String masterToken;

    @Bean
    public MasterTokenFilter masterTokenFilter() {
        return new MasterTokenFilter(masterToken);
    }

    @Bean
    public FilterRegistrationBean<MasterTokenFilter> masterTokenFilterRegistration(MasterTokenFilter filter) {
        FilterRegistrationBean<MasterTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
