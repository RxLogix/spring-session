# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Grails **plugin** (not an application) that wires [Spring Session](https://spring.io/projects/spring-session) with a Redis backend into a Grails app. It is RxLogix's fork of `jeetmp3/spring-session`, published as `org.grails.plugins:spring-session` to an internal Nexus repo. Consumers add it as an `implementation` dependency and configure it through `springsession.*` properties (see `README.md` for the full property list).

Current stack (from `gradle.properties` / `build.gradle`): Apache Grails 7.0.16 via `org.apache.grails:grails-bom`, Spring Boot 3.5.16, Gradle 8.14.4 wrapper, Java 17 (`compileJava.options.release = 17`), `jakarta.servlet`. Spring Session 3.5.7, Spring Data Redis 3.5.13 and Jedis 6.0.0 are BOM-managed, so `spring-session-data-redis` and `jedis` are declared without versions. The Grails Gradle plugin is resolved through a `buildscript {}` block from `https://repo.grails.org/grails/restricted` (there is no `buildSrc`). Version string lives in `gradle.properties` (`version=7.0.0-M1`). Branch naming: `3.x` is the Grails 3 line, `6.x-upgrade` is the Grails 6 mainline (default PR target, plugin versions `3.0-JDK11-Mx`), `task/7.x-upgrade` carries the Grails 7 work. See `MIGRATION_PLAN.md` for the 6 → 7 change log.

## Commands

```bash
./gradlew build                       # compile + unit tests + jar (bootJar is disabled)
./gradlew test                        # unit tests only (src/test/groovy, Spock on JUnit Platform)
./gradlew integrationTest             # src/integration-test/groovy; needs a running Redis on localhost:6379
./gradlew test --tests 'org.grails.plugins.springsession.config.SpringSessionConfigSpec'
./gradlew integrationTest --tests 'spring.session.SessionConfigSpec'
./gradlew bootRun                     # runs the embedded demo app (grails-app/init Application.groovy)
./gradlew publishToMavenLocal         # install jar + sources/javadoc/groovydoc jars locally
./gradlew publish                     # push to Nexus; needs nexusUrl/nexusUsername/nexusPassword
                                      # gradle properties or NEXUS_URL/NEXUS_USERNAME/NEXUS_PASSWORD env vars
```

Integration tests require Redis. `SessionPluginSpec` is a Geb/Selenium spec that drives `SpringSessionDemoController` in a browser; it also needs a WebDriver available. `SessionConfigSpec` only needs the Spring context, so it is the cheaper one to run when checking bean wiring.

When bumping the version, also add an entry to `changelog.md` (entries are keyed by version and reference the PVCM/PVI Jira ticket).

## Architecture

The plugin has two configuration layers that must agree with each other:

1. **Config resolution** — `src/main/groovy/utils/SpringSessionUtils.groovy` loads `grails-app/conf/DefaultSessionConfig.groovy` via `ConfigSlurper`, merges the consuming app's `springsession` block over it, and then pushes the merged result back into both `grailsApplication.config.springsession` and a high-priority Spring `PropertySource` named `SessionConfig`. This is why Java `@Bean` methods can read `springsession.redis.poolConfig` from `GrailsApplication.config` and see defaults the app never set. `SpringSessionUtils.application` is a static set by the plugin descriptor, so nothing else should touch config before `doWithSpring` runs.

2. **Bean wiring** — `src/main/groovy/SpringSessionGrailsPlugin.groovy` (`doWithSpring`) registers, using the Grails bean DSL:
   - `springSessionConfig` → `org.grails.plugins.springsession.config.SpringSessionConfig` (Java, `@EnableRedisIndexedHttpSession`; the *indexed* variant is deliberate, since in Spring Session 3.x the plain `@EnableRedisHttpSession` creates a non-indexed repository and ignores `ConfigureRedisAction`). It supplies the `JedisPoolConfig` bean (`poolConfig`, bound reflectively from `springsession.redis.poolConfig.*` via `BeanWrapper`), the `JedisClientConfiguration` bean (`jedisClientConfiguration`: SSL, connect/read timeout, pooling, read from `springsession.redis.*`), the JDK `RedisSerializer`, and two `FilterRegistrationBean`s ordered `HIGHEST_PRECEDENCE + 10` (Spring Session's `SessionRepositoryFilter`) and `+ 11` (`HttpSessionSynchronizer`).
   - `redisConnectionFactory` → `JedisConnectionFactory(<RedisConfiguration>, jedisClientConfiguration)`, in one of two shapes: **sentinel** (when `redis.sentinel.master` and `redis.sentinel.nodes` are set; builds `MasterNamedNode` + `RedisSentinelConfiguration` with `sentinelPassword` and data-node `password`) or **standalone** (`RedisStandaloneConfiguration` with host, port, database, password). `JedisShardInfo` no longer exists in Jedis 4+, so do not reintroduce it.
   - `httpSessionIdResolver` → `HeaderHttpSessionIdResolver` when `strategy.defaultStrategy == "HEADER"`, else `CookieHttpSessionIdResolver`.
   - `configureRedisAction` → `NoOpConfigureRedisAction`, so the plugin never tries to `CONFIG SET notify-keyspace-events` (required for managed Redis like ElastiCache).
   - `httpSessionSynchronizer` → re-`setAttribute`s every session attribute after each request when `allow.persist.mutable` is true, forcing Spring Session to persist in-place mutations.

`grails-app/conf/application.yml` excludes Spring Boot's `SessionAutoConfiguration` and sets `spring.redis.client-type: jedis`; the plugin owns session setup entirely, so don't reintroduce Boot's auto-config. The `plugin.loadAfter` list (`springSecurityCore`, `cors`) matters for filter ordering in consuming apps.

`grails-app/controllers/spring/session/SpringSessionDemoController.groovy` and `grails-app/init/.../Application.groovy` exist only to exercise the plugin in tests and `bootRun`; they are not part of the published API.

## Gotchas

- `DefaultSessionConfig.groovy` defaults `redis.connectionFactory.ssl = true`. A local plain Redis needs `springsession.redis.connectionFactory.ssl = false` in the consuming app (or in `application.yml` for integration tests).
- Config is a Groovy `ConfigObject`, so unset keys evaluate as empty `ConfigObject`s, not `null`. Use Groovy truth / `?:` and explicit `as Boolean` / `as int` casts when passing values to Java constructors, as the descriptor already does.
- `springsession.maxInactiveInterval` (seconds, default 1800) is applied through the `springSessionMaxInactiveIntervalCustomizer` bean in `SpringSessionConfig.java`. Before 7.0.0-M1 the property was defined but silently ignored.
- `grails-geb` in Grails 7 declares all its dependencies as optional, so `geb-spock` and the Selenium artifacts must be listed explicitly in `build.gradle` (versions come from the BOM).
- Gradle 8 requires `sourceJar` to depend on `generateGitProperties` explicitly, otherwise `assemble` fails validation.
- `spring-session-data-redis` and `jedis` are `api` scope on purpose (PVCM-128648): consuming apps rely on them transitively.
