package spring.session

import grails.testing.spring.AutowiredTest
import spock.lang.*
import utils.SpringSessionUtils

class SpringSessionUtilsSpec extends Specification implements AutowiredTest {

    void "Canary test"() {
        expect:
        true
    }

    void "test getSessionConfig"() {
        setup:
        SpringSessionUtils.config = null
        SpringSessionUtils.application = getGrailsApplication()

        when:
        ConfigObject configObject = SpringSessionUtils.getSessionConfig()

        then:
        configObject
        configObject.maxInactiveInterval == 5
    }

    void "test getSessionConfig with overridden values"() {
        setup:
        SpringSessionUtils.config = null
        SpringSessionUtils.application = getGrailsApplication()
        SpringSessionUtils.application.config.springsession = configObject

        when:
        ConfigObject resultConfig = SpringSessionUtils.getSessionConfig()

        then:
        resultConfig.redis.connectionFactory.hostName == hostname
        SpringSessionUtils.application.config.springsession.redis.connectionFactory.hostName == hostname
        resultConfig.redis.connectionFactory.port == SpringSessionUtils.application.config.springsession.redis.connectionFactory.port

        where:
        description                  | configObject                                                          | hostname
        "Null Config object"         | null                                                                  | "localhost"
        "Changed host Config object" | [redis: [connectionFactory: [hostName: "127.0.0.1"]]] as ConfigObject | "127.0.0.1"
    }
}
