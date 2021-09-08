def notify(type) {
    println("[JENKINS][DEBUG] project type - " + type)
}
def sonarScannerCheck(workDir) {
    def scannerHome = tool 'sonar-scanner';
    withSonarQubeEnv(installationName: 'sonarqube', credentialsId: 'sonar') {
      sh """
         ${scannerHome}/bin/sonar-scanner \
            -Dsonar.projectKey=${env.GERRIT_PROJECT_NAME} \
            -Dsonar.sources=. \
            -Dsonar.host.url=${env.SONAR_URL} \
            -Dsonar.scm.provider=git \
            -Dsonar.projectBaseDir=${workDir}/${env.GERRIT_PROJECT_NAME}
      """
    }
    def qualityGate = waitForQualityGate()
    if (qualityGate.status != 'OK') {
        error "[JENKINS][ERROR] Sonar quality gate check has been failed with status ${qualityGate.status}"
    }
}
node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def NAMESPACE = env.NAMESPACE
    def GERRIT_URL = env.GERRIT_URL
    def KUBECONFIG = env.KUBECONFIG
    def SONAR_URL = env.SONAR_URL
    stage("Pull HEAD branch") {
        withCredentials([usernamePassword(credentialsId: "jenkinsHTTP", passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh """
                git config --global user.email 'jenkins'
                git config --global user.name '${GIT_USERNAME}'
                rm -rf ${env.GERRIT_PROJECT_NAME}
                git clone https://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME}
                cd ${env.GERRIT_PROJECT_NAME}
                git fetch origin ${GERRIT_REFSPEC}
                git checkout FETCH_HEAD
            """
        }
    }
    stage("Processing") {
        def projectTypes = sh(script: "ls ${env.GERRIT_PROJECT_NAME}|grep -E '(helm|pipelines)'|tr -d '/'", returnStdout: true).trim()
        switch(projectTypes) {
            case "helm-charts":
                notify("helm-charts")
                def helmLintStatus = sh(script: "helm lint ${env.GERRIT_PROJECT_NAME}/helm-charts/", returnStatus: true)
                if (helmLintStatus.equals(0)) {
                    println("[JENKINS][DEBUG] yaml is valid.")
                } else {
                    println("[JENKINS][ERROR] yaml is not valid.")
                }
                break;
            case "pipelines":
                notify("groovy pipelines")
                //sonarScannerCheck("${WORKSPACE}")
                break;
            default:
                notify("other project")
                break;
        }
    }
}