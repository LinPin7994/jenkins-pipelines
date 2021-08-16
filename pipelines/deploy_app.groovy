def createNamespace(namespace) {
    try {
        sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} create ns ${namespace}")
    } catch(Exception e) {
        println("[JENKINS][DEBUG] namespace " + namespace + " exist")
    }
}
def applyManifest(helmManifest) {
    sh(script: "kubectl -n ${env.NAMESPACE} --kubeconfig ${env.KUBECONFIG} apply -f ${helmManifest}")
}
def createHelmTemplate() {
    sh(script: "helm template ${env.GERRIT_PROJECT_NAME}/helm-charts/ --set APP_VERSION=${env.IMAGE_VERSION} > helm_manifest.yaml")
}
def createHelmTemplateWithParameters() {
    sh(script: "helm template ${env.GERRIT_PROJECT_NAME}/helm-charts/ --set APP_VERSION=${env.IMAGE_VERSION},${env.CUSTOM_PARAMETERS} > helm_manifest.yaml")
}
def checkPodStatus(podName) {
    podStatus = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pod ${podName} --no-headers|awk '{print \$3}'", returnStdout: true).trim()
    println("[JENKINS][DEBUG] pod \"${podName}\" current status: ${podStatus}")
    logs = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} logs ${podName}", returnStdout: true)
    println("[JENKINS][DEBUG] Pod \"${podName}\" logs:\n${logs}")
}
def checkRolloutStatus(deployment) {
    rolloutStatus = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} rollout status deployment/${deployment} -n ${env.NAMESPACE}", returnStatus: true)
    return rolloutStatus
}
def printLogs() {
    deploingPod = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pod |grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
    checkPodStatus("${deploingPod}")
}
def prometheusPreDeploy() {
    def reloaderUrl = "https://raw.githubusercontent.com/stakater/Reloader/master/deployments/kubernetes/reloader.yaml"
    def kubeStateMetricsRepo = "https://github.com/kubernetes/kube-state-metrics.git"
    println("[JENKINS][DEBUG] project = " + GERRIT_PROJECT_NAME + ". Will be use extend stage for prometheus")
    sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} apply -f ${reloaderUrl}", returnStdout: true)
    println("[JENLINS][DEBUG] apply kube-state-metrics")
    sh """
       rm -rf kube-state-metrics
       git clone ${kubeStateMetricsRepo}
    """
    sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} apply -f kube-state-metrics/examples/standard/", returnStdout: true)
}
node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def NAMESPACE = env.NAMESPACE
    def GERRIT_URL = env.GERRIT_URL
    def KUBECONFIG = env.KUBECONFIG
    def IMAGE_VERSION = env.IMAGE_VERSION
    def CUSTOM_PARAMETERS = env.CUSTOM_PARAMETERS

    stage("Pull repository") {
        withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh """
                rm -rf ${env.GERRIT_PROJECT_NAME}
                git config --global user.name ${GIT_USERNAME}
                git config --global user.email ${GIT_USERNAME}@example.com
                git clone http://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME}
               """
        }

    }
    stage("Create namespace") {
        createNamespace("${env.NAMESPACE}")
    }
    stage("Create docker-registry secret") {
        build job: 'create-registry-secret', parameters: [string(name: 'NAMESPACE', value: env.NAMESPACE)]
    }
    stage("Deploy application") {
        if (CUSTOM_PARAMETERS.length() > 0 && GERRIT_PROJECT_NAME != 'prometheus') {
            createHelmTemplateWithParameters()
        } else if (CUSTOM_PARAMETERS.length() > 0 && GERRIT_PROJECT_NAME == 'prometheus') {
            prometheusPreDeploy()
            createHelmTemplateWithParameters()
        } else if (GERRIT_PROJECT_NAME == 'prometheus') {
            prometheusPreDeploy()
            createHelmTemplate()
        } else {
            createHelmTemplate()
        }
        archiveArtifacts artifacts: 'helm_manifest.yaml', fingerprint: true
        applyManifest("helm_manifest.yaml")
        sleep(10)
        currentDeployment = "${env.GERRIT_PROJECT_NAME}"
        deploymentRolloutStatus = checkRolloutStatus("${currentDeployment}")
        if (deploymentRolloutStatus.equals(0)) {
            printLogs()
        } else {
            printLogs()
            currentBuild.result = "FAILURE"
        }
    }
    stage("Label namespace") {
        sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} label --overwrite ns ${env.NAMESPACE} ${env.GERRIT_PROJECT_NAME}=${env.IMAGE_VERSION}")
    }
}