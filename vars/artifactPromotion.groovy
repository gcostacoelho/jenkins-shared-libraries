def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("crane") {
        sh '''
            REGISTRY="harbor.localhost.com/gustavome"
            REPOSITORY=${JOB_NAME%/*}

            OLD_TAG=""
            NEW_TAG=""

            ENV=""

            if [ $(echo $GIT_BRANCH | grep -E "^release-.*") ]; then
                ENV="stg"

                OLD_TAG="$(cat /artifacts/dev.artifact)"
                NEW_TAG="${GIT_BRANCH#*-}-$(echo ${OLD_TAG} | cut -d - -f 2)"

            elif [ $(echo $GIT_BRANCH | grep -E "v[0-9]\\.[0-9]{1,2}\\.[0-9]{1,3}$") ]; then
                ENV="prd"

                OLD_TAG="$(cat /artifacts/stg.artifact)"
                NEW_TAG="$(echo ${OLD_TAG} | cut -d - -f 1)"
            fi

            crane tag ${REGISTRY}/${REPOSITORY}:${OLD_TAG} ${NEW_TAG} --insecure

            echo "${NEW_TAG}" > /artifacts/${ENV}.artifact
        '''
    }
}