def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("alpine") {
        // Install stage dependencies
        sh "apk add openssh git"

        // Config Git user
        sh '''
            git config --global user.name "Jenkins"
            git config --global user.email "jenkins.local@email.com"
        '''

        // Prepare the ssh environment
        sh '''
            GITEA_HOST="gitea.localhost.com"

            mkdir $HOME/.ssh

            cp $ARGOCD_GITEA_PRIVATE_KEY $HOME/.ssh/id_rsa
            chmod 400 $HOME/.ssh/id_rsa

            ssh-keyscan ${GITEA_HOST} > $HOME/.ssh/known_hosts
        '''

        // Clone helm-applications repository
        sh '''
            GITEA_ORGANIZATION_HOST="git@gitea.localhost.com:gustavocosta.me"
            
            if [ ! -d helm-applications ]; then
                git clone $GITEA_ORGANIZATION_HOST/helm-applications.git
                ls -l  helm-applications/
            fi
        '''

        sh '''
            cd helm-applications/${JOB_NAME%/*}

            IMAGE_TAG="$(cat /artifacts/stg.artifact)"

            sed -i -E "s/v[0-9]{1,2}\\.[0-9]{1,3}\\.[0-9]{1,3}-[0-9a-z]{10}/${IMAGE_TAG}/g" values-stg.yaml

            git add values-stg.yaml
            git commit -m "[${JOB_NAME%/*}|stg] - Deploy image tag ${IMAGE_TAG}" --allow-empty
            git push
        '''
    }
}