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
    def GITHUB_URL = env.GITHUB_URL
    def GITHUB_REPO = env.GITHUB_REPO
    def NAMESPACE = env.NAMESPACE
    def KUBECONFIG = env.KUBECONFIG

    stage("Pull helm-charst repository from github") {
        sh(script: "git clone ${env.GITHUB_URL}", returnStdout: true)
    }
    stage("Create kuberentes manifest") {
        componentsList = ["gerrit", "nexus", "keycloak"]
        def userInput = input(
                            id: 'userInput', message: 'Parameters for deploy',
                            parameters: [

                                    string(defaultValue: 'openfrontier',
                                            description: 'Gerrit repository',
                                            name: 'GERRIT_REPO_URL'),
                                    string(defaultValue: 'OAUTH',
                                            description: 'Gerrit auth type',
                                            name: 'GERRIT_AUTH_TYPE'),
                                    string(defaultValue: 'worker-node-4',
                                            description: 'Node selecter for gerrit. You need check taint.',
                                            name: 'GERRIT_NODE_SELECTOR'),
                                    string(defaultValue: '3.3.2',
                                            description: 'Version for gerrit',
                                            name: 'GERRIT_VERSION'),
                            ])
        gerritRepo = userInput.GERRIT_REPO_URL?:''
        gerritAuthType = userInput.GERRIT_AUTH_TYPE?:''
        gerritNodeSelecter = userInput.GERRIT_NODE_SELECTER?:''
        gerritVersion = userInput.GERRIT_VERSION?:''
        for (item in componentsList) {
            if (item in ["gerrit"]) {
                sh(script: "helm template ${env.GITHUB_REPO}/helm-charts/${item}/helm-charts --set APP.REPOSITORY=${gerritRepo},APP.APP_VERSION=${gerritVersion},APP.AUTH_TYPE=${gerritAuthType},APP.NODE_SELECTER=${gerritNodeSelecter} > helm_manifest_${item}.yaml")
                archiveArtifacts artifacts: 'helm_manifest_${item}.yaml', fingerprint: true
            } else if (item in ["nexus", "keycloak"]) {
                sh(script: "helm template ${env.GITHUB_REPO}/helm-charts/${item}/helm-charts  > helm_manifest_${item}.yaml")
                archiveArtifacts artifacts: 'helm_manifest_${item}.yaml', fingerprint: true
            }
            
        }  
    }
    stage ("Deploy gerrit") {
        
    }
}