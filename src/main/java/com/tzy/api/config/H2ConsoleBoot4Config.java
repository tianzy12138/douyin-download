package com.tzy.api.config;

import lombok.extern.slf4j.Slf4j;
import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class H2ConsoleBoot4Config {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServletRegistration() {
        // 1. 直接实例化 H2 自带的 Servlet
        JakartaWebServlet h2Servlet = new JakartaWebServlet();

        // 2. 强行将其注册到 Spring Boot 4 的内嵌容器中
        ServletRegistrationBean<JakartaWebServlet> registrationBean = new ServletRegistrationBean<>(h2Servlet);

        // 3. 映射访问路径
        registrationBean.addUrlMappings("/h2-console/*");
        log.info("H2Console is active!");
        return registrationBean;
    }
}