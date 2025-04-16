def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("alpine") {
        // Install stage dependencies
        sh "apk add openssh git"

        sh '''
            RELEASE_VERSION="$(cat /artifacts/stg.artifact | cut -d - -f 1)"

            git config --global --add safe.directory $WORKSPACE

            git fetch --all

            git tag -a $RELEASE_VERSION -m "Production release $RELEASE_VERSION"
            git push --tags
        '''
    }
}