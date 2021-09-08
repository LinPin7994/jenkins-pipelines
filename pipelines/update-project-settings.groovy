import groovy.json.*

node("master") {
    def PROJECT_NAME = env.PROJECT_NAME

    def builder = new JsonBuilder()
    def addObject = builder.event {
        name "${env.PROJECT_NAME}"
    }

    def json = new JsonSlurper().parseText(new File("${JENKINS_HOME}/${settingFile}").text)
    json.event = addObject.event
    new JsonBuilder(json).toPrettyString()
}