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
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import redis.clients.jedis.JedisPoolConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@EnableRedisIndexedHttpSession
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

    /**
     * Jedis client settings (SSL, timeouts, pooling). Replaces the JedisShardInfo based wiring
     * used with Jedis 3.x, which no longer exists in Jedis 4+ / Spring Data Redis 3.x.
     */
    @Bean
    public JedisClientConfiguration jedisClientConfiguration(GrailsApplication grailsApplication, JedisPoolConfig poolConfig) {
        grails.config.Config config = grailsApplication.getConfig();
        boolean useSsl = config.getProperty("springsession.redis.connectionFactory.ssl", Boolean.class, Boolean.FALSE);
        boolean usePool = config.getProperty("springsession.redis.connectionFactory.usePool", Boolean.class, Boolean.TRUE);
        boolean sentinel = config.getProperty("springsession.redis.sentinel.master", String.class, null) != null;
        Integer timeout = sentinel
                ? config.getProperty("springsession.redis.sentinel.timeout", Integer.class, 5000)
                : config.getProperty("springsession.redis.connectionFactory.timeout", Integer.class, 2000);

        JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout));
        if (useSsl) {
            builder.useSsl();
        }
        if (usePool) {
            builder.usePooling().poolConfig(poolConfig);
        }
        return builder.build();
    }

    /**
     * Applies springsession.maxInactiveInterval (seconds) to the session repository.
     * Falls back to Spring Session's default of 1800 seconds when the property is absent.
     */
    @Bean
    public SessionRepositoryCustomizer<RedisIndexedSessionRepository> springSessionMaxInactiveIntervalCustomizer(GrailsApplication grailsApplication) {
        Integer seconds = grailsApplication.getConfig().getProperty("springsession.maxInactiveInterval", Integer.class, 1800);
        if (seconds == null || seconds <= 0) {
            throw new IllegalStateException("springsession.maxInactiveInterval must be a positive number of seconds, got: " + seconds);
        }
        Duration maxInactiveInterval = Duration.ofSeconds(seconds);
        return repository -> repository.setDefaultMaxInactiveInterval(maxInactiveInterval);
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
