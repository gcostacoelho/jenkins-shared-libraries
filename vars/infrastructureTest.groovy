def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("infra-test") {        
        // Install stage dependencies
        sh "apk add openssh curl"

        sh '''
            GITEA_HOST="gitea.localhost.com"
            GITEA_ORGANIZATION_HOST="git@gitea.localhost.com:gustavocosta.me"

            mkdir $HOME/.ssh

            cp $JENKINS_GITEA_PRIVATE_KEY $HOME/.ssh/id_rsa
            chmod 400 $HOME/.ssh/id_rsa

            ssh-keyscan ${GITEA_HOST} > $HOME/.ssh/known_hosts
        
            git clone $GITEA_ORGANIZATION_HOST/helm-applications.git
        
            ls -l  helm-applications/
        '''
    }
}