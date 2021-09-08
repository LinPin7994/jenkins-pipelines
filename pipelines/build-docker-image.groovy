def cleanUp() {
   def imagesList = sh(script: "docker images -qa", returnStdout: true).trim()
   while (imagesList.length() > 0) {
      try {
         println("[JENKINS][DEBUG] exists images " + imagesList)
         sh(script: "docker rmi -f \$(docker images -qa)", returnStdout: true)
         imagesList = sh(script: "docker images -qa", returnStdout: true).trim()
      } catch(Exception e) {
         imagesList = sh(script: "docker images -qa", returnStdout: true).trim()
         println("[JENKINS][DEBUG] " + e)
      }
   }
   println("[JENKINS][DEBUG] all docker images clean")
}

node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    def NAMESPACE = env.NAMESPACE
    def GERRIT_URL = env.GERRIT_URL
    def NEXUS_URL = env.NEXUS_URL
    def NEXUS_REPOSITORY = env.NEXUS_REPOSITORY
    def BUILD_IMAGE = env.BUILD_IMAGE

    stage("Pull repository") {
        withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh """
                rm -rf ${env.GERRIT_PROJECT_NAME}
                git config --global user.name ${GIT_USERNAME}
                git config --global user.email ${GIT_USERNAME}@example.com
                git clone https://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME}
               """
        }
    }
    stage("Clean docker images") {
        cleanUp()
    }
    stage("Build docker images") {
        sh(script: "cd ${env.GERRIT_PROJECT_NAME}/dockerfile/${env.BUILD_IMAGE} && docker build -t ${env.BUILD_IMAGE} .", returnStdout: true)
    }

    stage("Add tag") {
        sh(script: "docker tag \$(docker images|grep ${env.BUILD_IMAGE}|awk '{print \$1}') ${env.NEXUS_URL}/${env.NEXUS_REPOSITORY}/${env.BUILD_IMAGE}:1.${BUILD_NUMBER}")
    }

    stage("Push image to nexus") {
       sh(script: "docker push ${env.NEXUS_URL}'/'${env.NEXUS_REPOSITORY}/${env.BUILD_IMAGE}:1.${BUILD_NUMBER}")
   }

} 