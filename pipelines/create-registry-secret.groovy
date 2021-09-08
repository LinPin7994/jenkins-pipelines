node("docker") {
    def KUBECONFIG = env.KUBECONFIG
    def NAMESPACE = env.NAMESPACE
    def SECRET_NAME = env.SECRET_NAME
    def DOCKER_SERVER = env.DOCKER_SERVER
    def DOCKER_EMAIL = env.DOCKER_EMAIL
    def DOCKER_SERVER_PORT = env.DOCKER_SERVER_PORT
    stage("Create docker registry secret") {
        withCredentials([usernamePassword(credentialsId: 'nexusHTTP', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USERNAME')]) {
            def checkSecretExist = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get secret ${env.SECRET_NAME} --no-headers |awk '{print \$1}'", returnStdout: true).trim()
            if (checkSecretExist.length() > 0) {
                println("[JENKINS][DEBUG] Secret " + checkSecretExist + " exist.")
            } else {
                println("[JENKINS][DEBUG] Secret " + checkSecretExist + " doesn't exist and will be created.")
                sh(script: "kubectl --kubeconfig ${env.KUBECONFIG}  -n ${env.NAMESPACE} create secret docker-registry ${env.SECRET_NAME} --docker-server=${env.DOCKER_SERVER} --docker-username=${NEXUS_USERNAME} --docker-password=${NEXUS_PASSWORD} --docker-email=${env.DOCKER_EMAIL}")
            }
        }
    }
}