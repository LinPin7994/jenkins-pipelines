def numberOfBuildsToKeep = 5
def daysToKeepBuilds = 3
def pipelinePath = 'pipelines'
def repositoryUrl = 'http://192.168.0.56:32003/edp'
def repositoryCreds = 'jenkinsHTTP'
def kubeconfig = '/srv/admin.conf'
def gerritUrl = '192.168.0.56:32003'
def nexusUrl = '192.168.0.56:32000'

folder('grafana') {
    description('Grafana pipelines')
}
pipelineJob('grafana/grafana-deploy') {
    description("grafana deploy")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("grafana")
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
        stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")
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
                    scriptPath("${pipelinePath}/deploy_app.groovy")
                }
            }
        }
    }
}
pipelineJob('grafana/grafana-remove') {
    description("Удаление компонента")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("grafana")
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
        stringParam("NAMESPACE", "default", "")
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
                    scriptPath("${pipelinePath}/remove_app.groovy")
                }
            }
        }
    }
}
folder('prometheus') {
    description('Prometheus pipelines')
}
pipelineJob('prometheus/prometheus-deploy') {
    description("prometheus deploy")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("prometheus")
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
        stringParam("NAMESPACE", "default", "")
        stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")
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
                    scriptPath("${pipelinePath}/deploy_app.groovy")
                }
            }
        }
    }
}
pipelineJob('prometheus/prometheus-remove') {
    description("Удаление компонента")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("prometheus")
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
        stringParam("NAMESPACE", "default", "")
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
                    scriptPath("${pipelinePath}/remove_app.groovy")
                }
            }
        }
    }
}
pipelineJob('prometheus/node-exporter-deploy') {
    description("node-exporter deploy")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("node-exporter")
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
        stringParam("NAMESPACE", "default", "")
        stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")
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
                    scriptPath("${pipelinePath}/deploy_app.groovy")
                }
            }
        }
    }
}
folder('keycloak') {
    description('keycloak pipelines')
}
pipelineJob('keycloak/keycloak-deploy') {
    description("deploy keycloak")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("keycloak")
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
        stringParam("NAMESPACE", "default", "")
        stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")
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
                    scriptPath("${pipelinePath}/deploy_app.groovy")
                }
            }
        }
    }
}
pipelineJob('keycloak/keycloak-remove') {
    description("Удаление компонента")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("keycloak")
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
        stringParam("NAMESPACE", "default", "")
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
                    scriptPath("${pipelinePath}/remove_app.groovy")
                }
            }
        }
    }
}
folder('gerrit') {
    description('gerrit pipelines')
}
pipelineJob('gerrit/gerrit-deploy') {
    description("deploy gerrit")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("gerrit")
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
        stringParam("NAMESPACE", "default", "")
        stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")
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
                    scriptPath("${pipelinePath}/deploy_app.groovy")
                }
            }
        }
    }
}
pipelineJob('gerrit/keycloak-remove') {
    description("Удаление компонента")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("gerrit")
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
        stringParam("NAMESPACE", "default", "")
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
                    scriptPath("${pipelinePath}/remove_app.groovy")
                }
            }
        }
    }
}
folder('nexus') {
    description('nexus pipelines')
}
pipelineJob('nexus/nexus-deploy') {
    description("deploy nexus")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("nexus")
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
        stringParam("NAMESPACE", "default", "")
        stringParam("CUSTOM_PARAMETERS", "", "custom parameters for replace helm template value. Example - key=value,key=value")
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
                    scriptPath("${pipelinePath}/deploy_app.groovy")
                }
            }
        }
    }
}
pipelineJob('nexus/nexus-remove') {
    description("Удаление компонента")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("nexus")
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
        stringParam("NAMESPACE", "default", "")
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
                    scriptPath("${pipelinePath}/remove_app.groovy")
                }
            }
        }
    }
}
pipelineJob("get-and-push-docker-images") {
    description("Get and push external docker images")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('NEXUS_URL')
            defaultValue("192.168.0.56:32100")
            description('nexus url for docker-external artefacts')
        }
        wHideParameterDefinition {
            name('NEXUS_REPOSITORY')
            defaultValue('docker-external')
            description('s3 bucker for docker artefacts')
        }
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue('devops-version')
            description('repo for component version')
        }
        wHideParameterDefinition {
            name('GERRIT_URL')
            defaultValue(gerritUrl)
            description('gerrit internal url')
        }
        stringParam("IMAGE_NAME", "", "")
        stringParam("DOCKER_HUB_REPO", "", "")
        stringParam("VERSION", "", "")
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
                    scriptPath("${pipelinePath}/get-and-push-docker-image.groovy")
                }
            }
        }
    }
}
pipelineJob("update-devops-version") {
    description("Update devops version on master node")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue('devops-version')
            description('repo for component version')
        }
        wHideParameterDefinition {
            name('GERRIT_URL')
            defaultValue(gerritUrl)
            description('gerrit internal url')
        }
        wHideParameterDefinition {
            name('NEXUS_URL')
            defaultValue(nexusUrl)
            description('nexus internal url')
        }
    }
    triggers {
        upstream('get-and-push-docker-images')
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
                    scriptPath("${pipelinePath}/update-devops-version.groovy")
                }
            }
        }
    }
}
pipelineJob("create-registry-secret") {
    description("Create docker registry secret")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('KUBECONFIG')
            defaultValue(kubeconfig)
            description('path to kubeconfig value')
        }
        stringParam("NAMESPACE", "", "namespace for create secret")
        stringParam("SECRET_NAME", "docker-registry", "")
        stringParam("DOCKER_SERVER", "192.168.0.56", "")
        stringParam("DOCKER_SERVER_PORT", "32100", "")
        stringParam("DOCKER_USER", "jenkins", "")
        stringParam("DOCKER_PASSWORD", "", "")
        stringParam("DOCKER_EMAIL", "jenkins@example.com", "")
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
                    scriptPath("${pipelinePath}/create-registry-secret.groovy")
                }
            }
        }
    }
}