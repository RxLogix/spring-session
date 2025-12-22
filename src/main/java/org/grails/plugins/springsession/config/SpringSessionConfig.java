package org.grails.plugins.springsession.config;

import grails.core.GrailsApplication;
import org.grails.plugins.springsession.converters.GrailsJdkSerializationRedisSerializer;
import org.grails.plugins.springsession.web.http.HttpSessionSynchronizer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.Session;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import redis.clients.jedis.JedisPoolConfig;
import java.util.Collections;
import java.util.Map;

@EnableRedisHttpSession
public class SpringSessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GrailsJdkSerializationRedisSerializer();
    }

    @Bean
    public JedisPoolConfig poolConfig(GrailsApplication grailsApplication) {
        JedisPoolConfig config = new JedisPoolConfig();
        Map<String, Object> props =
                grailsApplication.getConfig().getProperty(
                        "springsession.redis.poolConfig",
                        Map.class,
                        Collections.emptyMap()
                );
        BeanWrapper wrapper = new BeanWrapperImpl(config);
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (wrapper.isWritableProperty(entry.getKey())) {
                try {
                    wrapper.setPropertyValue(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Invalid Jedis pool config property: " + entry.getKey(),
                            e
                    );
                }
            }
        }
        return config;
    }

    @Bean
    public FilterRegistrationBean<SessionRepositoryFilter<? extends Session>> springSessionFilter(SessionRepositoryFilter<? extends Session> filter) {
        FilterRegistrationBean<SessionRepositoryFilter<? extends Session>> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<HttpSessionSynchronizer> sessionSynchronizerFilter(HttpSessionSynchronizer filter) {
        FilterRegistrationBean<HttpSessionSynchronizer> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);
        return registrationBean;
    }
}
