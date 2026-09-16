# Changelog

All notable changes to this project will be documented in this file.

## 2.0.0-RC3-beta1
- [task-PVI-23315: committing changes of 2.0.0-RC3 plugin and changes for concurrent login.](https://github.com/RxLogix/spring-session/commit/966178e2ecb127d57c0752f1c7419331b977056f)

## 7.0-JDK11-M1
- [Merge grails 6.x upgrade to 7.0](https://github.com/RxLogix/spring-session/pull/3)

## 3.0-JDK11-M2
- Update Readme file with base branch 3.x

## 3.0-JDK11-M3
- PVCM-119734 : Add SSL support for ElastiCache Redis In-Transit and At-Rest Encryption

## 3.0-JDK11-M4
- PVCM-128648 : Fix ssl enable check type cast issue while creating bean. Also make poolConfig properties configurable.
- 
## 3.0-JDK11-M5
- PVCM-128648 : Correct dependencies scope to API to transmit libs files.

## 7.0.0-M1
- Upgrade plugin to Apache Grails 7.0.16 / Spring Boot 3.5.16 / Java 17 / Jakarta EE. Spring Session 3.5.7, Spring Data Redis 3.5.13, Jedis 6.0.0 (versions now managed by the Grails BOM). Redis connection wiring rewritten from `JedisShardInfo` to `RedisStandaloneConfiguration` / `RedisSentinelConfiguration` + `JedisClientConfiguration`. Session repository is now explicitly `@EnableRedisIndexedHttpSession` to keep Spring Session 2.x indexed behaviour. `springsession.maxInactiveInterval` is now actually applied to the session repository (it was previously ignored; effective default remains 1800s).
