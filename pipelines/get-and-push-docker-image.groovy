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

   stage("Get docker image from docker hub") {
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
}