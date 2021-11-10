import groovy.json.*
import Hudson.instance.*
import hudson.model.ListView
import jenkins.model.Jenkins

def pipelinePath = 'pipelines'
def repositoryUrl = 'https://gerrit.andtree.ru/edp'
def repositoryCreds = 'jenkinsHTTP'
def kubeconfig = '/srv/admin.conf'
def gerritUrl = 'gerrit.andtree.ru'
def nexusUrl = 'nexus.andtree.ru'
def sonarUrl = 'https://sonarqube.andtree.ru'

def createPipelines(pipelineName, applicationName, pipelineScript, pipelinePath, repositoryUrl, repositoryCreds, gerritUrl, nexusUrl, sonarUrl, kubeconfig) {
    folder("${applicationName}") {
        description("${applicationName} pipelines")
    }
    pipelineJob("${applicationName}/${pipelineName}") {
        logRotator {
            numToKeep(5)
            daysToKeep(3)
        }
        triggers {
            if (pipelineName.contains("precommit")) {
                gerrit {
                    events {
                        patchsetCreated()
                    }
                    project("plain:${applicationName}", ['ant:**'])
                }
            }
        }
        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(repositoryUrl)
                            credentials(repositoryCreds)
                        }
                        branches("master")
                        scriptPath("${pipelineScript}")
                    }
                }
                parameters {
                    wHideParameterDefinition {
                        name('PIPELINES_PATH')
                        defaultValue("${pipelinePath}")
                        description('Path to pipelines in DevOps repo')
                    }
                    wHideParameterDefinition {
                        name('GERRIT_PROJECT_NAME')
                        defaultValue("${applicationName}")
                        description('gerrit project name')
                    }
                    wHideParameterDefinition {
                        name('GERRIT_URL')
                        defaultValue(gerritUrl)
                        description('gerrit internal url')
                    }
                    wHideParameterDefinition {
                        name('KUBECONFIG')
                        defaultValue(kubeconfig)
                        description('path to kubeconfig file')
                    }
                    wHideParameterDefinition {
                        name('NEXUS_URL')
                        defaultValue(nexusUrl)
                        description('Nexus url')
                    }
                    stringParam("NAMESPACE", "default", "")
                    if (pipelineName =~ /.*_deploy*/) {
                        activeChoiceReactiveParam('IMAGE_VERSION') {
                        description('Exists image version')
                        choiceType('SINGLE_SELECT')
                        groovyScript {
                        script('''def list =[]
def project = "${GERRIT_PROJECT_NAME}"
def path = "/var/jenkins_home/workspace/update-devops-version/devops-version"
def cmd = new ProcessBuilder('sh','-c',"cat ${path}/${project}.txt").redirectErrorStream(false).start().text
list = cmd.readLines()
return list''')            
                        fallbackScript(''' return['error'] ''')
                        }
                        referencedParameter('GERRIT_PROJECT_NAME')
                        }
                    stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")   
                    }
                    if (pipelineName.contains("precommit")) 
                        wHideParameterDefinition {
                            name('SONAR_URL')
                            defaultValue(sonarUrl)
                            description('sonar url')
                        }
                }
            }
        }
    }
    Jenkins.instance.save()
    println("Created - ${pipelineName}")
}
['project.json'].each() { settingFile ->
    new JsonSlurperClassic().parseText(new File("${JENKINS_HOME}/${settingFile}").text).each() { item ->
        applicationName = item.name
        createPipelines("gerrit_precommit_${applicationName}", applicationName, "${pipelinePath}/gerrit-precommit.groovy", pipelinePath, repositoryUrl, repositoryCreds, gerritUrl, nexusUrl, sonarUrl, kubeconfig)
        createPipelines("${applicationName}_deploy_via_vm", applicationName, "${pipelinePath}/deploy_app.groovy", pipelinePath, repositoryUrl, repositoryCreds, gerritUrl, nexusUrl, sonarUrl, kubeconfig)
        createPipelines("${applicationName}_deploy_via_pod", applicationName, "${pipelinePath}/deploy_app_kub.groovy", pipelinePath, repositoryUrl, repositoryCreds, gerritUrl, nexusUrl, sonarUrl, kubeconfig)
        createPipelines("${applicationName}_remove", applicationName, "${pipelinePath}/remove_app.groovy", pipelinePath, repositoryUrl, repositoryCreds, gerritUrl, nexusUrl, sonarUrl, kubeconfig)
    }
}