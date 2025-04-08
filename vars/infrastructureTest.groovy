def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("infra-test") {        
        // Install stage dependencies
        sh "apk add openssh curl"

        // Prepare the ssh environment
        sh '''
            GITEA_HOST="gitea.localhost.com"

            mkdir $HOME/.ssh

            cp $JENKINS_GITEA_PRIVATE_KEY $HOME/.ssh/id_rsa
            chmod 400 $HOME/.ssh/id_rsa

            ssh-keyscan ${GITEA_HOST} > $HOME/.ssh/known_hosts
        '''

 	    // Clone helm-applications repository
        sh '''
            GITEA_ORGANIZATION_HOST="git@gitea.localhost.com:gustavocosta.me"

            git clone $GITEA_ORGANIZATION_HOST/helm-applications.git
            ls -l  helm-applications/
        '''

        // Installing the chart
        sh '''
            ENV=""

            if [ $(echo $GIT_BRANCH | grep -E ^developer$) ]; then
                ENV="dev"
            elif [ $(echo $GIT_BRANCH | grep -E "^hotfix-.*") ]; then
                ENV="stg"
            fi

            REPOSITORY=${JOB_NAME%/*}

            cd helm-applications/$REPOSITORY

            helm dependency build
            helm upgrade -i -f values-ci.yaml  \
                -n citest --create-namespace \
                --set image.tag="$(cat /artifacts/${ENV}.artifact)" \
                --set fullnameOverride=${REPOSITORY} \
                --insecure-skip-tls-verify \
                --wait \
                flask-ci .
        '''
    }
}