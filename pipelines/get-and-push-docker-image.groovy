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

def checkVersionInRepository() {
   def existVersion = sh(script: "cat ${env.GERRIT_PROJECT_NAME}/${env.IMAGE_NAME}.txt|grep ${env.VERSION}|| true", returnStdout: true).trim()
   if (existVersion == "${env.VERSION}") {
      println("[JENKINS][DEBUG] " + IMAGE_NAME + ":" + VERSION + " already exist")
      currentBuild.result = "SUCCESS"
      currentBuild.getRawBuild().getExecutor().interrupt(Result.SUCCESS)
   } else if (fileExists("${env.GERRIT_PROJECT_NAME}/${env.IMAGE_NAME}.txt")) {
      println("[JENKINS][DEBUG] add new version in file " + GERRIT_PROJECT_NAME + "/" + IMAGE_NAME + ".txt")
      sh(script: "echo ${env.VERSION} >> ${env.GERRIT_PROJECT_NAME}/${env.IMAGE_NAME}.txt")
   } else {
      println("[JENKINS][DEBUG] create version file and add new version to " + GERRIT_PROJECT_NAME + "/" + IMAGE_NAME + ".txt")
      sh(script: "echo ${env.VERSION} > ${env.GERRIT_PROJECT_NAME}/${env.IMAGE_NAME}.txt")
   }
}
node("docker") {
   def IMAGE_NAME = env.IMAGE_NAME
   def VERSION = env.VERSION
   def NEXUS_URL = env.NEXUS_URL
   def NEXUS_REPOSITORY = env.NEXUS_REPOSITORY
   def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
   def GERRIT_URL = env.GERRIT_URL
   def DOCKER_HUB_REPO = env.DOCKER_HUB_REPO

   stage("Clean docker images") {
      cleanUp()
   }

   stage("Pull devops-version repo") {
      withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh """
                rm -rf ${env.GERRIT_PROJECT_NAME}
                git config --global user.name ${GIT_USERNAME}
                git config --global user.email ${GIT_USERNAME}@example.com
                git clone http://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME}
               """
      }
   }

   stage("Get docker image from docker hub") {
      checkVersionInRepository()
      if (DOCKER_HUB_REPO.length() > 0) {
         sh(script: "docker pull ${env.DOCKER_HUB_REPO}/${env.IMAGE_NAME}:${env.VERSION}")
      } else {
         sh(script: "docker pull ${env.IMAGE_NAME}:${env.VERSION}")
      }  
   }

   stage("Add tag for docker image") {
       sh(script: "docker tag \$(docker images|grep ${env.IMAGE_NAME}|awk '{print \$1}'):${env.VERSION} ${env.NEXUS_URL}/${env.NEXUS_REPOSITORY}/${env.IMAGE_NAME}:${env.VERSION}")
   }

   stage("Push image to nexus") {
       sh(script: "docker push ${env.NEXUS_URL}'/'${env.NEXUS_REPOSITORY}/${env.IMAGE_NAME}")
   }
   stage("Add version to devops-version") {
      checkVersionInRepository()
   }
   stage("Push to gerrit") {
      withCredentials([usernamePassword(credentialsId: 'jenkinsHTTP', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
         sh """
            cd ${env.GERRIT_PROJECT_NAME}
            git config --global user.name ${GIT_USERNAME}
            git config --global user.email ${GIT_USERNAME}@example.com
            git add ${env.IMAGE_NAME}.txt
            git commit --allow-empty -m 'update devops version files'
            git push http://${GIT_USERNAME}:${GIT_PASSWORD}@${env.GERRIT_URL}/${env.GERRIT_PROJECT_NAME} HEAD:master
            """
      } 
   }
}