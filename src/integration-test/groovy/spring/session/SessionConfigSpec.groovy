package spring.session

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.session.data.redis.RedisIndexedSessionRepository
import org.springframework.session.web.http.SessionRepositoryFilter
import spock.lang.Specification

import java.time.Duration

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
        given: "the configured max inactive interval"
        int expectedSeconds = grailsApplication.config.getProperty('springsession.maxInactiveInterval', Integer, 1800)

        when: "a new session is created by the repository"
        def session = sessionRepository.createSession()

        then: "its max inactive interval matches the configured value (5s in this test app, not the 1800s default)"
        expectedSeconds != 1800
        session.maxInactiveInterval == Duration.ofSeconds(expectedSeconds)
    }
}