package spring.session

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.session.data.redis.RedisIndexedSessionRepository
import org.springframework.session.web.http.SessionRepositoryFilter
import spock.lang.Specification

@Integration
class SessionConfigSpec extends Specification {

    SessionRepositoryFilter springSessionRepositoryFilter
    RedisIndexedSessionRepository sessionRepository
    GrailsApplication grailsApplication

    def setup() {
    }

    def cleanup() {
    }

    void "Session Repository Filter bean injected"() {
        expect: "SessionRepositoryFilter and RedisIndexedSessionRepository should be injected"
        springSessionRepositoryFilter != null
        sessionRepository != null
    }

    void "Check http session timeout"() {
        expect: "The default max inactive interval should be as configured"
        sessionRepository.defaultMaxInactiveInterval == (grailsApplication.config.getProperty('springsession.maxInactiveInterval') as Integer) ?: 1800
    }
}