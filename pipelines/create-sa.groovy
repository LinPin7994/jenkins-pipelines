node("docker") {
    def KUBECONFIG = env.KUBECONFIG
    def SA_NAME = env.SA_NAME
    def NAMESPACE = env.NAMESPACE

    stage("Create service account") {
        sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} create sa ${env.SA_NAME}||true")
        sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} create clusterrolebinding ${env.SA_NAME} --clusterrole cluster-admin --serviceaccount=${env.NAMESPACE}:${env.SA_NAME}||true")
    }
}