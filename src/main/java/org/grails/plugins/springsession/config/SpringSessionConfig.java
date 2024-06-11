package org.grails.plugins.springsession.config;

import org.grails.plugins.springsession.converters.GrailsJdkSerializationRedisSerializer;
import org.grails.plugins.springsession.web.http.HttpSessionSynchronizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.Session;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import redis.clients.jedis.JedisPoolConfig;

@EnableRedisHttpSession
public class SpringSessionConfig {

    @Bean
    public RedisSerializer springSessionDefaultRedisSerializer() {
        return new GrailsJdkSerializationRedisSerializer();
    }

    @Bean
    public JedisPoolConfig poolConfig() {
        return new JedisPoolConfig();
    }

    @Bean
    public FilterRegistrationBean springSessionFilter(SessionRepositoryFilter<? extends Session> filter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean sessionSynchronizerFilter(HttpSessionSynchronizer filter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);
        return registrationBean;
    }
}
