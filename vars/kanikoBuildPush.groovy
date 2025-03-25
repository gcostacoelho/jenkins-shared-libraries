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
            TAG="0.0.1"

            DESTINATION="${REGISTRY}/${REPOSITORY}:${TAG}"

            /kaniko/executor \
                --insecure \
                --destination ${DESTINATION} \
                --context $(pwd)
        '''
    }
}