# Migration Plan — spring-session plugin — Grails 6.2.0 → Apache Grails 7.0.16

Status: **APPLIED (uncommitted) — all mechanical changes and L1–L3 applied; see "Execution log" at the end.**
Baseline: branch `task/7.x-upgrade`, HEAD `c85537d`, tagged `3.0-JDK11-M5`.
Nothing is to be committed until explicitly requested.

## Triage

| Item | Finding |
|---|---|
| Target Grails | 7.0.16 (Hibernate 5 line; no Hibernate 6 work) |
| Current Java | Source level 11; machine default JDK is Zulu 17.0.6, so Java 17 move is a build-flag change |
| Plugin type / risk | **High** — servlet filter plugin with Spring bean wiring, `javax.servlet`, Redis client stack moves three majors |
| GORM domain classes | None |
| Spring Security | Not used |
| Views / assets | None (`grails-app/views` and `grails-app/assets` do not exist) — §5, §6, §8 of the skill do not apply |

## Verified target versions (from downloaded BOMs and jars)

| Component | Now | Grails 7.0.16 BOM provides |
|---|---|---|
| Spring Boot | 2.7.x | 3.5.16 |
| Spring Session | 2.7.4 | 3.5.7 |
| Spring Data Redis | 2.7.x | 3.5.13 |
| Jedis | 3.8.0 | 6.0.0 |
| Gradle wrapper | 7.6.4 | 8.14.4 |
| Selenium | 4.19.1 (pinned) | 4.38.0 (BOM-managed) |
| Spock | — | 2.3-groovy-4.0 |

API facts confirmed by inspecting the jars:
- `redis.clients.jedis.JedisShardInfo` does not exist in Jedis 6.0.0.
- `JedisConnectionFactory` (SDR 3.5.11) has no `setShardInfo`. It still has `setHostName`, `setPort`, `setPassword`, `setTimeout`, `setUsePool`, `setPoolConfig`, `setDatabase`, `setUseSsl`, `setConvertPipelineAndTxResults`, and constructors `(RedisStandaloneConfiguration, JedisClientConfiguration)` and `(RedisSentinelConfiguration, JedisPoolConfig | JedisClientConfiguration)`.
- Spring Session 3.5.7: `@EnableRedisHttpSession` now backs `RedisSessionRepository` (non-indexed). `@EnableRedisIndexedHttpSession` backs `RedisIndexedSessionRepository` and is the only one that consumes a `ConfigureRedisAction` bean.
- `RedisIndexedSessionRepository` has `setDefaultMaxInactiveInterval(Duration)` / `(int)` but no getter.
- The `springSessionDefaultRedisSerializer` qualifier and `springSessionRepositoryFilter` bean name still exist.

## Mechanical changes (apply on `go`)

| # | File | Change |
|---|------|--------|
| 1 | `build.gradle` | Replace `plugins{}` with `buildscript{}` block; `apply plugin: "org.apache.grails.gradle.grails-plugin"`; add `implementation platform("org.apache.grails:grails-bom:$grailsVersion")`; rename `org.grails:*` → `org.apache.grails:*` (`grails-core`, `grails-logging`, `grails-databinding`, `grails-i18n` [group `org.apache.grails.i18n`], `grails-interceptors`, `grails-rest-transforms`, `grails-services`, `grails-url-mappings`, `grails-web-boot`, `grails-gsp`, `grails-console`); drop `micronaut-inject-groovy`, `micronaut-http-client`, `hibernate5`, `scaffolding`, `h2`, `tomcat-jdbc` (no domain classes); drop Selenium `resolutionStrategy` and pins; Geb → `org.apache.grails:grails-geb`; testing support → `org.apache.grails.testing:grails-testing-support-core` + `org.apache.grails:grails-testing-support-web`; add `net.bytebuddy:byte-buddy` (+ agent); make `spring-session-data-redis` and `jedis` versionless `api` deps (BOM-managed); `compileJava.options.release = 17`; `groovyOptions.optimizationOptions.indy = false`; `classifier` → `archiveClassifier`; `tasks.withType(Test).configureEach` |
| 2 | `gradle.properties` | `grailsVersion=7.0.16`, `springBootVersion=3.5.16`, `version=7.0.0-M1`; remove `grailsGradlePluginVersion` |
| 3 | `settings.gradle` | Remove `pluginManagement`; keep `rootProject.name="spring-session"` |
| 4 | `buildSrc/` | Delete directory (replaced by `buildscript{}`) |
| 5 | `gradle/wrapper/gradle-wrapper.{jar,properties}`, `gradlew`, `gradlew.bat` | Regenerate with `./gradlew wrapper --gradle-version 8.14.4 --distribution-type bin` (run twice) |
| 6 | `src/main/groovy/SpringSessionGrailsPlugin.groovy` | `grailsVersion = "7.0.0 > *"`; fix corrupted `scm` URL string |
| 7 | `src/main/java/org/grails/plugins/springsession/web/http/HttpSessionSynchronizer.java` | `javax.servlet.*` → `jakarta.servlet.*` (5 imports, lines 8–12) |
| 8 | `grails-app/conf/application.yml` | `spring.redis.client-type` → `spring.data.redis.client-type`; add `springsession.redis.connectionFactory.ssl: false` for the bundled test app |
| 9 | `application.properties` | `app.grails.version=7.0.16` |
| 10 | `changelog.md`, `README.md`, `CLAUDE.md` | Add `7.0.0-M1` entry; update version/stack notes |

## Logic changes (require explicit approval before code is written)

| # | File | Change | Why | Risk |
|---|------|--------|-----|------|
| L1 | `SpringSessionGrailsPlugin.groovy` `doWithSpring()` | Remove `jedisShardInfo`/`shardInfo` beans. Standalone: `RedisStandaloneConfiguration(host, port)` + `database` + `password`, wrapped with a `JedisClientConfiguration` carrying `useSsl`, `connectTimeout`/`readTimeout`, and `usePooling(poolConfig)`. Sentinel: `RedisSentinelConfiguration` (master + nodes + `sentinelPassword`) + same `JedisClientConfiguration`. | `JedisShardInfo` and `setShardInfo` no longer exist. This is the SSL and password path. | **High** |
| L2 | `SpringSessionConfig.java` | `@EnableRedisHttpSession` → `@EnableRedisIndexedHttpSession` | Preserves Spring Session 2.7 behaviour (indexed repository, keyspace events, `configureRedisAction` honoured, `RedisIndexedSessionRepository` injectable). | Medium |
| L3 | `src/integration-test/groovy/spring/session/SessionConfigSpec.groovy` | Rewrite the max-inactive-interval assertion (e.g. create a session via the repository and assert `session.maxInactiveInterval`) | No `getDefaultMaxInactiveInterval()` on the repository in 3.5. | Low |

## Files with no changes needed

`SpringSessionUtils.groovy`, `GrailsJdkSerializationRedisSerializer.java`, `JdkDeserializer.java`, `MasterNamedNode.java`, `NoOpConfigureRedisAction.java`, `SpringSessionDemoController.groovy`, `Application.groovy`, `DefaultSessionConfig.groovy`, `SpringSessionConfigSpec.groovy`, `SessionPluginSpec.groovy`, `logback.xml`.

## Human-review flags (RxLogix policy)

- L1 rewires Redis **authentication (passwords) and TLS** configuration. Requires a human reviewer before merge regardless of approval in-session.
- The JDK serialization deserializer (`JdkDeserializer`, `GrailsJdkSerializationRedisSerializer`) is unchanged, but session payload deserialization from untrusted Redis content is a standing security consideration worth noting in the review.

## Verification steps (after Phase 2)

```bash
./gradlew --version                              # expect Gradle 8.14.4
./gradlew clean compileGroovy compileJava
./gradlew test
./gradlew integrationTest                        # needs local Redis on 6379, plain (non-TLS)
./gradlew assemble && unzip -l build/libs/*.jar | head
./gradlew publishToMavenLocal
```

## Totals

10 mechanical changes across 12 files; 3 logic changes requiring approval.

## Git policy for this task

No `git commit`, `git tag`, or `git push` until the user explicitly asks.

## Execution log (2026-09-16)

Applied on `go all`. Deviations from the plan above:

- **L1 implementation detail:** `JedisClientConfiguration` is built as a `@Bean jedisClientConfiguration` in `SpringSessionConfig.java` (SSL, connect/read timeout, pooling with `poolConfig`) and referenced from the descriptor DSL, because a `ref("poolConfig")` cannot be consumed inside a static helper at DSL-evaluation time. Sentinel path uses `sentinelPassword` for `springsession.redis.sentinel.password` and `password` for the data-node password; standalone uses `RedisStandaloneConfiguration`.
- **L3 changed direction:** the rewritten assertion revealed that `springsession.maxInactiveInterval` is *not* applied to the repository (effective timeout is Spring Session's 1800s default; the Grails 6 assertion `a == b ?: 1800` was always truthy). The spec now asserts the actual behaviour and documents the gap. See L4 below.
- **Extra test deps:** `grails-geb` 7.0.16 marks all its dependencies optional, so `org.apache.groovy.geb:geb-spock` and the Selenium api/remote-driver/support/driver artifacts were added explicitly (BOM-managed).
- **Gradle 8 validation:** `sourceJar` now `dependsOn generateGitProperties` and excludes `git.properties`.
- **Wrapper bootstrap:** the 7.6.4 wrapper could not evaluate the Grails 7 build, so `distributionUrl` was hand-bumped first, then the `wrapper` task was run twice; all four wrapper files are regenerated at 8.14.4.

### Verification results

| Command | Result |
|---|---|
| `./gradlew --version` | Gradle 8.14.4, JVM 17.0.6 |
| `./gradlew clean compileGroovy compileJava compileTestGroovy compileIntegrationTestGroovy` | OK |
| `./gradlew test` | 9 tests, 0 failures |
| `./gradlew integrationTest` (local plain Redis on 6379) | 6 tests, 0 failures, 1 skipped (pre-existing `@Ignore`) |
| `./gradlew assemble publishToMavenLocal` | OK — main, sources, javadoc, groovydoc jars; POM imports grails-bom 7.0.16 + spring-boot-dependencies 3.5.16 |

### L4 — APPLIED (approved 2026-09-16)

**File:** `SpringSessionConfig.java` — `springSessionMaxInactiveIntervalCustomizer` bean (`SessionRepositoryCustomizer<RedisIndexedSessionRepository>`) applies `springsession.maxInactiveInterval` seconds via `setDefaultMaxInactiveInterval(Duration)`; falls back to 1800 and rejects non-positive values.
**Why:** the property is documented in `DefaultSessionConfig.groovy` but previously had no effect.
**Risk:** Medium — consumers that set the property now get the timeout they configured (default 1800s unchanged otherwise).
**Verified:** `SessionConfigSpec` asserts the test app's 5s value is applied (and that it differs from the default). `test` + `integrationTest`: 15 tests, 0 failures, 1 pre-existing skip.

### Human review required before merge

- L1 (Redis authentication and TLS wiring) — RxLogix policy.
- Test in a real Grails 7 host application via `publishToMavenLocal` (`org.grails.plugins:spring-session:7.0.0-M1`).
