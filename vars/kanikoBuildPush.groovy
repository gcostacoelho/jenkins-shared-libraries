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

            ENV=""

            if [ $(echo $GIT_BRANCH | grep -E ^developer$) ]; then
                TAG="dev-${GIT_COMMIT:0:10}"
                ENV="dev"
            elif [ $(echo $GIT_BRANCH | grep -E "^hotfix-.*") ]; then
                TAG="${GIT_BRANCH#*-}-${GIT_COMMIT:0:10}"
                ENV="stg"
            fi

            DESTINATION="${REGISTRY}/${REPOSITORY}:${TAG}"

            /kaniko/executor \
                --insecure \
                --destination ${DESTINATION} \
                --context $(pwd)

            echo "${TAG}" > /artifacts/${ENV}.artifact
        '''
    }
}