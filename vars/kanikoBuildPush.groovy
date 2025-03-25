def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("kaniko") {
        sh '''
            echo "Building docker image and push to registry"

            REGISTRY="harbor.localhost.com/gustavome"
            REPOSITORY=${JOB_NAME%/*}
            TAG=""

            if [ $(echo $GIT_BRANCH | grep -E ^developer$) ]; then
                TAG="dev-${GIT_COMMIT:0:10}"
            elif [ $(echo $GIT_BRANCH | grep -E "^(release-.*)|(hotfix-.*)") ]; then
                TAG="${GIT_BRANCH#*-}-${GIT_COMMIT:0:10}"
            fi

            DESTINATION="${REGISTRY}/${REPOSITORY}:${TAG}"

            /kaniko/executor \
                --insecure \
                --destination ${DESTINATION} \
                --context $(pwd)
        '''
    }
}