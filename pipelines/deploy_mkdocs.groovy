node("docker") {
    GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    GERRIT_URL = env.GERRIT_URL
    HOST = env.HOST
    WEB_ROOT_DIR = env.WEB_ROOT_DIR
    stage("Pull mkdocs repository") {
        withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh """
                rm -rf ${env.GERRIT_PROJECT_NAME}
                git config --global user.name ${GIT_USERNAME}
                git config --global user.email ${GIT_USERNAME}@example.com
                git clone https://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME}
               """
        }
    }
    stage("Upload to web-server") {
        withCredentials([sshUserPrivateKey(credentialsId: 'web-server', keyFileVariable: 'KEY', usernameVariable: 'SSH_USER')]) { 
            sh(script: "scp -i ${KEY} -r ${env.GERRIT_PROJECT_NAME}/site/ ${SSH_USER}@${env.HOST}:${env.WEB_ROOT_DIR}", returnStdout: true)
        }
    }
}