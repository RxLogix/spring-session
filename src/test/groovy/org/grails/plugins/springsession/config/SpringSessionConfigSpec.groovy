package org.grails.plugins.springsession.config

import grails.core.GrailsApplication
import grails.config.Config
import redis.clients.jedis.JedisPoolConfig
import spock.lang.Specification

class SpringSessionConfigSpec extends Specification {

    SpringSessionConfig configClass = new SpringSessionConfig()

    GrailsApplication grailsApplication = Mock()
    Config grailsConfig = Mock()

    def setup() {
        grailsApplication.getConfig() >> grailsConfig
    }

    void "should bind jedis pool properties from config when present"() {
        given:
        grailsConfig.getProperty(
                "springsession.redis.poolConfig",
                Map,
                _
        ) >> [
                maxTotal    : 50,
                maxIdle     : 10,
                minIdle     : 2,
                testOnBorrow: true
        ]

        when:
        JedisPoolConfig poolConfig =
                configClass.poolConfig(grailsApplication)

        then:
        poolConfig.maxTotal == 50
        poolConfig.maxIdle == 10
        poolConfig.minIdle == 2
        poolConfig.testOnBorrow
    }

    void "should keep defaults when pool config is not provided"() {
        given:
        grailsConfig.getProperty(
                "springsession.redis.poolConfig",
                Map,
                _
        ) >> [:]

        when:
        JedisPoolConfig poolConfig =
                configClass.poolConfig(grailsApplication)

        then:
        poolConfig.maxTotal == JedisPoolConfig.DEFAULT_MAX_TOTAL
        poolConfig.maxIdle == JedisPoolConfig.DEFAULT_MAX_IDLE
        poolConfig.minIdle == JedisPoolConfig.DEFAULT_MIN_IDLE
    }

    void "should ignore unknown properties gracefully"() {
        given:
        grailsConfig.getProperty(
                "springsession.redis.poolConfig",
                Map,
                _
        ) >> [
                maxTotal: 20,
                fooBar  : "shouldBeIgnored"
        ]

        when:
        JedisPoolConfig poolConfig =
                configClass.poolConfig(grailsApplication)

        then:
        poolConfig.maxTotal == 20
        noExceptionThrown()
    }

    void "should convert string values to target types"() {
        given:
        grailsConfig.getProperty(
                "springsession.redis.poolConfig",
                Map,
                _
        ) >> [
                maxTotal    : "30",
                testOnBorrow: "true"
        ]

        when:
        JedisPoolConfig poolConfig =
                configClass.poolConfig(grailsApplication)

        then:
        poolConfig.maxTotal == 30
        poolConfig.testOnBorrow
    }

    void "should fail fast on invalid property value"() {
        given:
        grailsConfig.getProperty(
                "springsession.redis.poolConfig",
                Map,
                _
        ) >> [
                maxTotal: "notANumber"
        ]

        when:
        configClass.poolConfig(grailsApplication)

        then:
        thrown(IllegalStateException)
    }

}
