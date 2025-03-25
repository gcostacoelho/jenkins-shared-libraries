def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    pipeline {
        agent {
            kubernetes {
                yamlFile 'JenkinsAgent.yaml'
            }
        }

        stages {
            stage('Unit tests') {
                steps {
                    pythonUnitTest { } // Chama a outra parte do shared library
                }
                when {
                    anyOf {
                        branch pattern:  'developer'
                        branch pattern:  'release/'
                        branch pattern:  'feature/'
                        branch pattern:  'hotfix/'
                        branch pattern:  'fix/'
                    }
                }
            }

            stage('Quality Gate') {
                environment {
                    SONAR_HOST_URL = 'http://sonarqube.localhost.com/'
                    SONAR_TOKEN    = credentials('sonar-scanner-cli')
                }
                steps {
                    sonarQualityGate { }
                }
                when {
                    anyOf {
                        branch pattern:  'developer'
                        branch pattern:  'release/'
                        branch pattern:  'feature/'
                        branch pattern:  'hotfix/'
                        branch pattern:  'fix/'
                    }
                }
            }

            stage('Build and push') {
                steps {
                    kanikoBuildPush { }
                }
                when {
                    anyOf {
                        branch pattern:  'developer'
                        branch pattern:  'release/'
                        branch pattern:  'hotfix/'
                        branch pattern:  'main'
                    }
                }
            }
        }
    }
}
