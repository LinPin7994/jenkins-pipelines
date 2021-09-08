import Hudson.instance.*
import hudson.model.ListView
import jenkins.model.Jenkins

def viewName = 'devops'

if (Jenkins.instance.getView(viewName)) {
    println("[JENKINS][DEBUG] ${viewName} view already exists")
} else {
    Jenkins.instance.addView(new ListView(viewName))
    println("[JENKINS][DEBUG] ${viewName} view created")
}

def jobNames = ["create-service-account", "create-registry-secret", "clean-up-workspace", "get-and-push-docker-images", "job_provision", "update-devops-version"]

jobNames.each() { jobName ->
    try {
        Jenkins.instance.getView(viewName).doAddJobToView(jobName)
        println("Job ${jobName} added to ${viewName} view")
    }
    catch (Exception e) {
        println("Exception - " + e)
    }
}