def getComponents(pv, pvc, svc, deploy, cm) {
    objects = [:]
    objects['deploy'] = "${deploy}"
    objects['svc'] = "${svc}"
    objects['pvc'] = "${pvc}"
    objects['pv'] = "${pv}"
    objects['configmap'] = "${cm}"
    while (pv.length() > 0 || pvc.length() > 0 || svc.length() > 0 || deploy.length() > 0 || cm.length() > 0) {
        (objects).each {
            println "[JENKINS][DEBUG] object ${it.key} : ${it.value} mark deleted"
            if (it.key == "pv") {
                try {
                    sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} delete ${it.key} ${it.value}")
                } catch (Exception e) {
                    println("[JENKINS][DEBUG] " + e)
                }
            } else {
                try {
                    sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} delete ${it.key} ${it.value}")
                } catch (Exception e) {
                    println("[JENKINS][DEBUG] " + e)
                }
            }
        }
        pv = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} get pv |grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        pvc = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pvc|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        svc = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get svc|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        deploy = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get deploy|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        cm = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get cm|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        if (pv.length() == 0 && pvc.length() == 0 && svc.length() == 0 && deploy.length() == 0 && cm.length() == 0) {
            println("[JENKINS][DEBUG] all resources deleted" )
        }
    }
}
node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def NAMESPACE = env.NAMESPACE
    def GERRIT_URL = env.GERRIT_URL
    def KUBECONFIG = env.KUBECONFIG

    stage("Delete components") {
        def persistentVolume = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} get pv |grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        def persistentVolumeClaim = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get pvc|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        def service = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get svc|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        def deployment = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get deploy|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        def configmap = sh(script: "kubectl --kubeconfig ${env.KUBECONFIG} -n ${env.NAMESPACE} get cm|grep ${env.GERRIT_PROJECT_NAME}|awk '{print \$1}'", returnStdout: true).trim()
        getComponents("${persistentVolume}", "${persistentVolumeClaim}", "${service}", "${deployment}", "${configmap}")
    }
}    
