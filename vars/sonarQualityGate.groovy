def call(body){
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container('sonar-scanner-cli') {
        sh '''
            echo "${JOB_NAME%/*}"

            sonar-scanner -X \
                -Dsonar.SONAR_TOKEN=${SONAR_TOKEN} \
                -Dsonar.projectKey=${JOB_NAME%/*}-${GIT_BRANCH} \
                -Dsonar.qualitygate.wait=true
        '''
    }
}