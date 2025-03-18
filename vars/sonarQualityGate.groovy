def call(body){
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container('sonar-scanner-cli') {
        sh '''
            echo "${JOB_NAME%/*}"

            sonar-scanner -X \
                -D sonar.login=${SONAR_LOGIN}
                -D sonar.projectKey=${JOB_NAME%/*}
        '''
    }
}