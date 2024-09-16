package spring.session

import groovy.util.logging.Slf4j

@Slf4j
class SpringSessionDemoController {

    def set(String key, String value) {
        session[key] = value
        log.debug session.id
        render "success"
    }

    def get(String key) {
        log.debug session.id
        render(session[key] ?: "NO-KEY")
    }

    def setMutable(String name) {
        session.demoMap = [name: name]
        render "done"
    }

    def updateMutable(String name) {
        Map demoMap = session.demoMap
        demoMap.name = name
        render "ok"
    }

    def getFinalValue(String name) {
        Map sessionMap = session.demoMap
        render(sessionMap[name])
    }
}
