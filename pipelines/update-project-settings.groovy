import groovy.json.*

def PROJECT_NAME = getBinding().getVariables()['PROJECT_NAME']
def settingFile = "project.json"

def builder = new JsonBuilder()
def addObject = builder.event {
       name "${PROJECT_NAME}"
}

def json = new JsonSlurper().parseText(new File("${JENKINS_HOME}/${settingFile}").text)
json.event = addObject.event
new JsonBuilder(json).toPrettyString()