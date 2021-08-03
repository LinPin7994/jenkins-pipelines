def numberOfBuildsToKeep = 5
def daysToKeepBuilds = 3
def pipelinePath = 'pipelines'
def repositoryUrl = 'http://192.168.0.56:32003/edp'
def repositoryCreds = 'jenkinsHTTP'

folder('grafana') {
    description('Grafana pipelines')
}
pipelineJob('grafana/grafana-deploy') {
    description("Унифицированный деплой helm charts")
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
            defaultValue('192.168.0.56:32003')
            description('gerrit internal url')
        }
        wHideParameterDefinition {
            name('KUBECONFIG')
            defaultValue('/srv/admin.conf')
            description('path to kubeconfig file')
        }
        stringParam("NAMESPACE", "default", "")
        activeChoiceReactiveParam('IMAGE_VERSION') {
            description('Exists image version')
            choiceType('SINGLE_SELECT')
            groovyScript {
                script('''def list =[]
def project = "${GERRIT_PROJECT_NAME}"
def path = "/var/jenkins_home/workspace/update-devops-version/devops-version/${GERRIT_PROJECT_NAME}.txt"
def cmd = new ProcessBuilder('sh','-c',"cat ${path}").redirectErrorStream(false).start().text
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
            defaultValue('192.168.0.56:32003')
            description('gerrit internal url')
        }
        wHideParameterDefinition {
            name('KUBECONFIG')
            defaultValue('/srv/admin.conf')
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
            defaultValue('192.168.0.56:32003')
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
            defaultValue('192.168.0.56:32003')
            description('gerrit internal url')
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
