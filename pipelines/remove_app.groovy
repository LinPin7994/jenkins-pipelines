def deleteComponents(config, key, value, namespace) {
    for (item in value.split("\n")) {
        if (key == 'pv') {
            sh(script: "kubectl --kubeconfig ${config} delete ${key} ${item}")
        } else {
            sh(script: "kubectl --kubeconfig ${config} -n ${namespace} delete ${key} ${item}")
        }
    }
}
def initVars() {
    pv = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} get pv |grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
    pvc = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pvc|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
    svc = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get svc|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
    deploy = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get deploy|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
    cm = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get cm|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
}
node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def NAMESPACE = env.NAMESPACE
    def GERRIT_URL = env.GERRIT_URL
    def KUBECONFIG = env.KUBECONFIG
    stage("Delete components") {
        initVars()
        objects = [:]
        objects['deploy'] = "${deploy}"
        objects['svc'] = "${svc}"
        objects['pvc'] = "${pvc}"
        objects['pv'] = "${pv}"
        objects['configmap'] = "${cm}"
        while (pv.length() > 0 || pvc.length() > 0 || svc.length() > 0 || deploy.length() > 0 || cm.length() > 0) {
            (objects).each { key, value ->
                println "[JENKINS][DEBUG] object ${key} : ${value} mark deleted"
                try {
                    deleteComponents(env.KUBECONFIG, key, value, env.NAMESPACE)
                } catch (Exception e) {
                    println("[JENKINS][DEBUG] " + e)
                }
            }
            initVars()
            if (pv.length() == 0 && pvc.length() == 0 && svc.length() == 0 && deploy.length() == 0 && cm.length() == 0) {
                println("[JENKINS][DEBUG] all resources deleted" )
            }
        }
    }
}    