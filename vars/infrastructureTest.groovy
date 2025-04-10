def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("infra-test") {        
        // Install stage dependencies
        sh "apk add openssh"

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
                --wait \
                flask-ci .
        '''

        // Send a request using curl
        sh '''
            STATUS_CODE="$(curl --silent \
                --output /dev/null \
                --write-out %'{http_code}\n' \
                http://sample-app.citest.svs.cluster.local:5000/users
            )"

            if [ "$STATUS_CODE" == "200" ]; then
                echo "All good, response HTTP 200"
            else
                echo "Error: $STATUS_CODE"
                exit 1
            fi;
        '''
    }
}