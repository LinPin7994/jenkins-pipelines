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

def checkPodStatus(podName) {
    def podStatus = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pod ${podName} --no-headers|awk '{print \$3}'", returnStdout: true).trim()
    timeout(time: 5, unit: 'MINUTES') {
        for (i = 0; i < 30; i++) {
            if (podStatus in ["ContainerCreating", "Pending"]) {
                println("[JENKINS][DEBUG] pod \"${podName}\" current status: ${podStatus}")
                sleep(10)
                podStatus = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pod ${podName} --no-headers|awk '{print \$3}'", returnStdout: true).trim()
            } else {
                i = 30
            }
        }
    }
    println("[JENKINS][DEBUG] Pod \"${podName}\" current status: ${podStatus}")
    logs = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} logs ${podName}", returnStdout: true)
    println("[JENKINS][DEBUG] Pod \"${podName}\" logs:\n${logs}")
    if (!(podStatus in ["Completed", "Running"])) {
        currentBuild.result = "FAILURE"
    }
}

node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def NAMESPACE = env.NAMESPACE
    def GERRIT_URL = env.GERRIT_URL
    def KUBECONFIG = env.KUBECONFIG
    def IMAGE_VERSION = env.IMAGE_VERSION

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
    stage("Deploy application") {
        sh(script: "helm template ${env.GERRIT_PROJECT_NAME}/helm-charts/ --set APP_VERSION=${env.IMAGE_VERSION} > helm_manifest.yaml")
        archiveArtifacts artifacts: 'helm_manifest.yaml', fingerprint: true
        applyManifest("helm_manifest.yaml")
        sleep(10)
    }
    stage("Labels namespace") {
        sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} label --overwrite ns ${env.NAMESPACE} ${env.GERRIT_PROJECT_NAME}=${env.IMAGE_VERSION}")
    }
    stage("Print logs") {
        deploingPod = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pod |grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        checkPodStatus("${deploingPod}")
    }
}