# spring-session-grails-plugin

This plugin provides [Spring Session](https://spring.io/projects/spring-session) support in a Grails application, using Redis to persist HTTP sessions. Spring Session gives you:

* HttpSession
  * Clustered sessions
  * Multiple browser sessions
  * RESTful APIs (header based session id)
* WebSocket (not yet supported by this plugin)

Currently this plugin provides support for HttpSession only.

Official Spring Session documentation: https://docs.spring.io/spring-session/reference/

## Plugin details

| | |
|---|---|
| Maven coordinates | `org.grails.plugins:spring-session` |
| Plugin name (Grails) | `springSession` |
| Load order | after `springSecurityCore` and `cors` |
| Session store | Redis via Jedis, `RedisIndexedSessionRepository` |
| Session id strategy | Cookie (`SESSION`) or HTTP header (`x-auth-token`) |
| Serialization | JDK serialization (`GrailsJdkSerializationRedisSerializer`) |
| Filter order | `SessionRepositoryFilter` at `HIGHEST_PRECEDENCE + 10`, `HttpSessionSynchronizer` at `+ 11` |
| Config prefix | `springsession.*` |

The plugin registers its own `RedisConnectionFactory`, disables Spring Boot's `SessionAutoConfiguration`, and sets `spring.data.redis.client-type=jedis`.

## Version support

| Plugin version | Grails | Java | Spring Boot | Spring Session | Redis client | Servlet API | Status |
|---|---|---|---|---|---|---|---|
| **7.0.0-M1** (branch `task/7.x-upgrade`) | 7.0.x (built against 7.0.16) | 17+ | 3.5.x | 3.5.x | Jedis 6.0 | `jakarta.servlet` | Current |
| 3.0-JDK11-M3 … M5 (branch `6.x-upgrade`) | 6.2.0 | 11+ | 2.7.x | 2.7.4 | Jedis 3.8 | `javax.servlet` | Legacy, maintenance only |
| 3.0-JDK11-M1 / M2 | 6.2.0 | 11 | 2.7.x | 2.7.4 | Jedis 3.8 | `javax.servlet` | Legacy, superseded by M3+ (no SSL / poolConfig support) |
| 2.0.x (branch `2.x`) | 3.1.x | 7+ | 1.3.x | 1.0.2 | Jedis 2.5 | `javax.servlet` | Legacy, unsupported |
| 1.0 … 1.2 (branches `1.0` … `1.3`) | 2.4+ | 6+ | n/a | 1.0.1 | Jedis 2.x | `javax.servlet` | Legacy, unsupported |

Redis server 2.8+ is required for all versions. The Redis keyspace-notification `CONFIG SET` step is deliberately disabled (`NoOpConfigureRedisAction`), so managed Redis such as AWS ElastiCache works without extra permissions.

Feature availability by version:

| Feature | 1.x | 2.0.x | 3.0-JDK11-M1/M2 | 3.0-JDK11-M3+ | 7.0.0-M1 |
|---|---|---|---|---|---|
| Cookie / header session id strategy | yes | yes | yes | yes | yes |
| Redis Sentinel | yes | yes | yes | yes | yes |
| `allow.persist.mutable` | yes (1.1+) | yes | yes | yes | yes |
| TLS to Redis (`connectionFactory.ssl`) | no | no | no | yes | yes |
| Configurable Jedis pool (`redis.poolConfig.*`) | no | no | no | yes | yes |
| `maxInactiveInterval` actually applied | no | no | no | no | **yes** |

## Using

Add the plugin dependency to `build.gradle`.

Grails 7.x
```groovy
dependencies {
    implementation "org.grails.plugins:spring-session:7.0.0-M1"
}
```

Grails 6.x (legacy)
```groovy
dependencies {
    implementation "org.grails.plugins:spring-session:3.0-JDK11-M5"
}
```

Grails 3.x (legacy)
```groovy
dependencies {
    runtime "org.grails.plugins:spring-session:2.0-RC1"
}
```

The plugin declares `spring-session-data-redis` and `jedis` as `api` dependencies. On Grails 7 their versions come from the Grails BOM that the plugin POM imports, so do not pin them in the consuming application unless you need to override.

## Configuration

All properties live under the `springsession` prefix and can be set in `application.yml` or `application.groovy`. Values shown are the defaults from the plugin's `DefaultSessionConfig.groovy`.

#### 1. Redis connection
```groovy
springsession.redis.connectionFactory.hostName = "localhost"
springsession.redis.connectionFactory.port = 6379
springsession.redis.connectionFactory.password = "<redis auth password>"   // optional
springsession.redis.connectionFactory.timeout = 2000                        // ms, connect and read timeout
springsession.redis.connectionFactory.usePool = true
springsession.redis.connectionFactory.dbIndex = 0
springsession.redis.connectionFactory.ssl = true                            // default TRUE; set false for a plain Redis
springsession.redis.connectionFactory.convertPipelineAndTxResults = true
```

#### 2. Jedis pool (3.0-JDK11-M3 and later)
Any writable property of `redis.clients.jedis.JedisPoolConfig` can be set:
```groovy
springsession.redis.poolConfig.maxTotal = 8
springsession.redis.poolConfig.maxIdle = 8
springsession.redis.poolConfig.minIdle = 0
springsession.redis.poolConfig.testOnBorrow = true
```

#### 3. Session timeout (applied from 7.0.0-M1)
```groovy
springsession.maxInactiveInterval = 1800   // seconds
```

#### 4. Session id strategy
Default is cookie based with cookie name `SESSION`. To switch to an HTTP header:
```groovy
springsession.strategy.defaultStrategy = "HEADER"
springsession.strategy.httpHeader.headerName = "x-auth-token"   // default
```

Note: the property is `strategy.httpHeader.headerName`. Older README versions documented `strategy.token.headerName`, which the plugin never read. The cookie name is currently fixed at `SESSION`; `springsession.strategy.cookie.name` is present in the default config but not applied by the plugin.

#### 5. Redis Sentinel
```groovy
springsession.redis.sentinel.master = "<sentinel master name>"
springsession.redis.sentinel.nodes = [[host: "host1", port: 26379], [host: "host2", port: 26379]]
springsession.redis.sentinel.password = "<sentinel password>"     // auth for the sentinel nodes
springsession.redis.sentinel.timeout = 2000                        // ms
springsession.redis.connectionFactory.password = "<data password>" // auth for the Redis data nodes
```
Sentinel mode is enabled when both `sentinel.master` and `sentinel.nodes` are set. `connectionFactory.hostName` and `port` are ignored in Sentinel mode; `dbIndex`, `ssl`, `usePool` and `poolConfig` still apply.

#### 6. Persisting mutable session objects
Spring Session does not detect in-place changes to mutable objects stored in the session. Enable re-saving of every attribute after each request:
```groovy
springsession.allow.persist.mutable = true   // default false
```

## Migrating a consuming application to 7.0.0-M1

The `springsession.*` property names are unchanged from the 3.0-JDK11 line. The following items do need attention when moving an application from the Grails 6 plugin to 7.0.0-M1.

#### Prerequisites
* The application itself must already be on Apache Grails 7.0.x, Java 17+, Spring Boot 3.5 and Jakarta EE. This plugin cannot be mixed into a Grails 6 / Spring Boot 2 application.
* Any of the application's own code that touches the session filter chain, `HttpSession`, or servlet filters must use `jakarta.servlet.*` imports.

#### Configuration structure changes

| Area | Grails 6 plugin (3.0-JDK11-Mx) | 7.0.0-M1 | Action |
|---|---|---|---|
| Spring Boot Redis properties | `spring.redis.*` | `spring.data.redis.*` | If the application set any `spring.redis.*` keys (for example `spring.redis.client-type`), rename them. The plugin sets `spring.data.redis.client-type: jedis` itself. |
| `springsession.maxInactiveInterval` | Present in defaults but **ignored**; sessions always expired after 1800 s | Applied to the session repository | Review the value. If your application set it to something other than 1800 expecting it to work, that timeout now takes effect. Remove any workaround such as `server.servlet.session.timeout` or a custom `SessionRepositoryCustomizer`. |
| Sentinel authentication | `sentinel.password` was passed as the connection password to the Redis data nodes; a separate sentinel-node password was not supported | `sentinel.password` authenticates against the **sentinel nodes**; `connectionFactory.password` authenticates against the **data nodes** | If your Redis data nodes require AUTH in Sentinel mode, add `springsession.redis.connectionFactory.password`. If your sentinels do not require AUTH, remove `sentinel.password` or leave it empty. |
| Sentinel timeout | `sentinel.timeout` applied to the shard, defaulting to 5000 ms if unset | `sentinel.timeout` is the connect/read timeout for the Jedis client in Sentinel mode; default 2000 ms from `DefaultSessionConfig` | Set it explicitly if you relied on the 5000 ms fallback. |
| TLS | `connectionFactory.ssl` (default `true`) | Same property, same default, now applied through `JedisClientConfiguration.useSsl()` | No change. Keep `ssl = false` for a plain local Redis. |
| Jedis pool | `redis.poolConfig.*` | Same; pool is attached only when `usePool = true` | No change. |
| Session repository type | `@EnableRedisHttpSession` (indexed repository in Spring Session 2.x) | `@EnableRedisIndexedHttpSession` | No change for the application. Session keys in Redis keep the `spring:session:` namespace, and principal-name indexing still works. |
| Spring Boot session auto-configuration | Excluded by the plugin | Still excluded | Do not add `spring-boot-starter-data-redis` session auto-configuration expecting it to take over. |

#### Runtime and dependency changes

* Redis client is Jedis 6.0. Applications that referenced `redis.clients.jedis.JedisShardInfo` or `JedisConnectionFactory.setShardInfo(...)` directly must remove that code; those APIs no longer exist.
* Spring Session 3.x serializes session attributes with the same JDK serializer as before, but the attribute classes must be loadable under Java 17 and `jakarta.*`. Sessions written by a Grails 6 application containing `javax.*` types will not deserialize in the Grails 7 application. Plan for a session flush (or a different `dbIndex`) during cut-over rather than a rolling upgrade sharing one Redis database.
* Redis 2.8 remains the documented minimum, but the Redis client stack moved from Jedis 3.8 to Jedis 6.0. Validate against your actual Redis server version (in particular managed services such as ElastiCache) before production cut-over.

#### Suggested cut-over checklist
1. Upgrade the application to Grails 7 first and get it green without this plugin.
2. Add `org.grails.plugins:spring-session:7.0.0-M1`, remove any pinned `spring-session-data-redis` / `jedis` versions.
3. Rename `spring.redis.*` keys to `spring.data.redis.*`.
4. Review `springsession.maxInactiveInterval` and the Sentinel password split described above.
5. Point the application at an empty Redis database (or clear the `spring:session:*` keys) for the first deployment.
6. Verify login, session persistence across two application nodes, and session expiry.

## Changelog
See [changelog.md](changelog.md).
