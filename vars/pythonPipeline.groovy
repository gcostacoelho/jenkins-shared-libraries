def call(body){
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
	                branch pattern:  "feature*"
                    branch pattern:  "developer"
                    branch pattern:  "release*"
                    branch pattern:  "hotfix*"
                    branch pattern:  "fix*"
                    branch pattern:  'v*'
                }
            }
        }

        stage('Quality Gate') {
            environment {
                SONAR_HOST_URL = "http://sonarqube.localhost.com/"
                SONAR_TOKEN    = credentials('sonar-scanner-cli')
            }
            steps {
                sonarQualityGate { }
            }
            when {
                anyOf {
                    branch pattern:  "developer"
                    branch pattern:  "release*"
	                branch pattern:  "feature*"
                    branch pattern:  "hotfix*"
                    branch pattern:  "fix*"
                    branch pattern:  'v*'
                }
            }
        }

            stage('Build and push') {
                steps {
                    kanikoBuildPush { }
                }
                when {
                    anyOf {
                        branch pattern:  'hotfix*'
                        branch pattern:  'developer'
                    }
                }
            }

            stage('Scan Security') {
                environment {
                    HARBOR_API_TOKEN = credentials('harbor-api-token')
                }

                steps {
                    scanSecurity { }
                }
                when {
                    anyOf {
                        branch pattern:  'hotfix*'
                        branch pattern:  'developer'
                    }
                }
            }
        }
    }
}