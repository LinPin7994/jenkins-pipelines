node("master") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def GERRIT_URL = env.GERRIT_URL
    def NEXUS_URL = env.NEXUS_URL
    stage("Pull devops-version repository") {
        withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh """
                rm -rf ${env.GERRIT_PROJECT_NAME}
                git config --global user.name ${GIT_USERNAME}
                git config --global user.email ${GIT_USERNAME}@example.com
                git clone http://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME}
               """
      }
    }
    stage("Update devops-version") {
        def components = sh(script: "curl -sSL -X GET  http://${env.NEXUS_URL}/service/rest/v1/search/assets |grep path|grep docker-external|cut -d/ -f3|uniq", returnStdout: true).trim()
        println("[JENKINS][DEBUG] Clean " + GERRIT_PROJECT_NAME + " repository")
        sh(script: "rm -f ${env.GERRIT_PROJECT_NAME}/*")
        for (item in components.split("\n")) {
            def version = sh(script: """curl -sSL -X GET  http://${env.NEXUS_URL}/service/rest/v1/search/assets | grep docker-external|grep ${item}|cut -d/ -f5|grep -v docker|sed -e \'s/",//g' """, returnStdout: true).trim()
            println("[JENKINS][DEBUG] Update " + item + " version.")
            if (version.length() > 0) {
                for (ver in version.split("\n")) {
                    sh(script: "echo ${ver} >> ${env.GERRIT_PROJECT_NAME}/${item}.txt")
                }
            } else {
                sh(script: "echo ${version} > ${env.GERRIT_PROJECT_NAME}/${item}.txt")
            }
        }
    }
    stage("Push to devops-version") {
        withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
         sh """
            cd ${env.GERRIT_PROJECT_NAME}
            git config --global user.name ${GIT_USERNAME}
            git config --global user.email ${GIT_USERNAME}@example.com
            git add *
            git commit --allow-empty -m 'update devops version files'
            git push http://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME} HEAD:master
            """
        }
    }
}