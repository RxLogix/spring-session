import grails.plugins.Plugin
import groovy.util.logging.Slf4j
import org.grails.plugins.springsession.data.redis.config.MasterNamedNode
import org.grails.plugins.springsession.data.redis.config.NoOpConfigureRedisAction
import org.grails.plugins.springsession.web.http.HttpSessionSynchronizer
import org.grails.plugins.springsession.config.SpringSessionConfig
import org.springframework.data.redis.connection.RedisNode
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.session.web.http.CookieHttpSessionIdResolver
import org.springframework.session.web.http.HeaderHttpSessionIdResolver

import utils.SpringSessionUtils

@Slf4j
class SpringSessionGrailsPlugin extends Plugin {

    def grailsVersion = "7.0.0 > *"
    def title = "Spring Session Grails Plugin"
    def author = "Jitendra Singh"
    def authorEmail = "jeet.mp3@gmail.com"
    def description = 'Provides support for SpringSession project'
    def documentation = "https://github.com/jeetmp3/spring-session"
    def license = "APACHE"
    def issueManagement = [url: "https://github.com/jeetmp3/spring-session/issues"]
    def scm = [url: "https://github.com/jeetmp3/spring-session"]
    def loadAfter = ['springSecurityCore', 'cors']
    def profiles = ['web']

    Closure doWithSpring() {
        { ->
            println "\n++++++ Configuring Spring session"
            SpringSessionUtils.application = grailsApplication
            ConfigObject conf = SpringSessionUtils.sessionConfig

            springSessionConfig SpringSessionConfig

            // SSL, timeouts and pooling live in the jedisClientConfiguration bean (SpringSessionConfig).
            // This replaces the JedisShardInfo beans, which no longer exist in Jedis 4+ / Spring Data Redis 3.x.
            if (conf.redis.sentinel.master && conf.redis.sentinel.nodes) {
                List<Map> nodes = conf.redis.sentinel.nodes as List<Map>
                masterName(MasterNamedNode) {
                    name = conf.redis.sentinel.master
                }
                redisSentinelConfiguration(RedisSentinelConfiguration) {
                    master = ref("masterName")
                    sentinels = (nodes.collect { new RedisNode(it.host as String, it.port as Integer) }) as Set
                    database = (conf.redis.connectionFactory.dbIndex ?: 0) as int
                    if (conf.redis.sentinel.password) {
                        sentinelPassword = RedisPassword.of(conf.redis.sentinel.password as String)
                    }
                    if (conf.redis.connectionFactory.password) {
                        password = RedisPassword.of(conf.redis.connectionFactory.password as String)
                    }
                }
                redisConnectionFactory(JedisConnectionFactory, ref("redisSentinelConfiguration"), ref("jedisClientConfiguration")) {
                    convertPipelineAndTxResults = (conf.redis.connectionFactory.convertPipelineAndTxResults ? true : false) as Boolean
                }
            } else {
                redisStandaloneConfiguration(RedisStandaloneConfiguration,
                        (conf.redis.connectionFactory.hostName ?: "localhost") as String,
                        (conf.redis.connectionFactory.port ?: 6379) as int) {
                    database = (conf.redis.connectionFactory.dbIndex ?: 0) as int
                    if (conf.redis.connectionFactory.password) {
                        password = RedisPassword.of(conf.redis.connectionFactory.password as String)
                    }
                }
                redisConnectionFactory(JedisConnectionFactory, ref("redisStandaloneConfiguration"), ref("jedisClientConfiguration")) {
                    convertPipelineAndTxResults = (conf.redis.connectionFactory.convertPipelineAndTxResults ? true : false) as Boolean
                }
            }

            String defaultStrategy = conf.strategy.defaultStrategy
            if (defaultStrategy == "HEADER") {
                httpSessionIdResolver(HeaderHttpSessionIdResolver) {
                    headerName = conf.strategy.httpHeader.headerName
                }
            } else {
                httpSessionIdResolver(CookieHttpSessionIdResolver)
            }

            configureRedisAction(NoOpConfigureRedisAction)
            httpSessionSynchronizer(HttpSessionSynchronizer) {
                persistMutable = conf.allow.persist.mutable as Boolean
            }

            println "++++++ Finished Spring Session configuration"
        }
    }
}
