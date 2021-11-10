node("docker") {
    HOST = env.HOST
    SCRIPT = env.SCRIPT
    stage("Exec command") {
        sh """
            echo -e "${env.SCRIPT}\n" >> script.sql
        """
        archiveArtifacts artifacts: 'script.sql', fingerprint: true
    }
}
