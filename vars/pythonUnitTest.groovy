def call(body){
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("python") {
        sh '''
            pip install -r requirements.txt
            pytest -v --disable-warnings
            bandit -r . -x '/.venv/','/tests/'
            black .
            flake8 . --exclude .venv
        '''
    }
}