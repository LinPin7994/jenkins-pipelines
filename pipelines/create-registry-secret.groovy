node("docker") {
    def KUBECONFIG = env.KUBECONFIG
    def NAMESPACE = env.NAMESPACE
    def SECRET_NAME = env.SECRET_NAME
    def DOCKER_SERVER = env.DOCKER_SERVER
    def DOCKER_USER = env.DOCKER_USER
    def DOCKER_PASSWORD = env.DOCKER_PASSWORD
    def DOCKER_EMAIL = env.DOCKER_EMAIL
    def DOCKER_SERVER_PORT = env.DOCKER_SERVER_PORT
    stage("Create docker registry secret") {
        sh(script: "kubectl --kubeconfig ${env.KUBECONFIG}  -n ${env.NAMESPACE} create secret docker-registry ${env.SECRET_NAME} --docker-server=${env.DOCKER_SERVER}:${env.DOCKER_SERVER_PORT} --docker-username=${env.DOCKER_USER} --docker-password=${env.DOCKER_PASSWORD} --docker-email=${env.DOCKER_EMAIL}", returnStdout: true)
    }
}