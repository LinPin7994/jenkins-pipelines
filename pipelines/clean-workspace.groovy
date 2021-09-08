def clean(workDir) {
    sh(script: "rm -rf ${workDir}/${env.GERRIT_PROJECT_NAME}/*", returnStdout: true)
}
node("docker") {
    def GERRIT_PROJECT_NAME = env.GERRIT_PROJECT_NAME
    WORKSPACE = "/tmp/workspace"
    println("[JENKINS][DEBUG] clean up workspace dir.")
    clean("${WORKSPACE}")
}