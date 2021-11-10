def numberOfBuildsToKeep = 5
def daysToKeepBuilds = 3
def pipelinePath = 'pipelines'
def repositoryUrl = 'https://gerrit.andtree.ru/edp'
def repositoryCreds = 'jenkinsHTTP'
def kubeconfig = '/srv/admin.conf'
def gerritUrl = 'gerrit.andtree.ru'
def nexusUrl = 'nexus.andtree.ru'
def sonarUrl = 'https://sonarqube.andtree.ru'

pipelineJob("get-and-push-docker-images") {
    description("Get and push external docker images")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('NEXUS_URL')
            defaultValue(nexusUrl)
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
        stringParam("DOCKER_SERVER", nexusUrl, "")
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
pipelineJob('gerrit-precommit-edp') {
    description("Перкомит проверка кода")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("edp")
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
            name('SONAR_URL')
            defaultValue(sonarUrl)
            description('sonar url')
        }
        triggers {
            gerrit {
                events {
                    patchsetCreated()
                }
                project('plain:edp', ['ant:**'])
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
                    scriptPath("${pipelinePath}/gerrit-precommit.groovy")
                }
            }
        }
    }
}
pipelineJob('clean-up-workspace') {
    description("Очистка рабочей директории")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    } 
    parameters {
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue("")
            description('gerrit project name')
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
                    scriptPath("${pipelinePath}/clean-workspace.groovy")
                }
            }
        }
    }
}
pipelineJob("create-service-account") {
    description("Create kubernetes service account")
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
        stringParam("SA_NAME", "jenkins", "sa name")
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
                    scriptPath("${pipelinePath}/create-sa.groovy")
                }
            }
        }
    }
}
pipelineJob("Deploy-mkdocs") {
    description("Deploy mkdocs site")
    logRotator {
        numToKeep(numberOfBuildsToKeep)
        daysToKeep(daysToKeepBuilds)
    }
    parameters {
        wHideParameterDefinition {
            name('HOST')
            defaultValue('192.168.0.150')
            description('host with web server')
        }
        wHideParameterDefinition {
            name('GERRIT_PROJECT_NAME')
            defaultValue('docs-andtree-ru')
            description('repository')
        }
        wHideParameterDefinition {
            name('GERRIT_URL')
            defaultValue(gerritUrl)
            description('gerrit internal url')
        }
        wHideParameterDefinition {
            name('WEB_ROOT_DIR')
            defaultValue('/var/www/html/')
            description('web server root dir')
        }
        triggers {
            scm("* * * * *")
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
                    scriptPath("${pipelinePath}/deploy_mkdocs.groovy")
                }
            }
        }
    }
}