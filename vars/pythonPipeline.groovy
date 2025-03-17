def call(body){
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    pipeline {
    agent {
        kubernetes {
            yamlFile "JenkinsAgent.yaml"
        }
    }

    stages {
        stage('Unit tests') {
            steps {
                pythonUnitTest {} // Chama a outra parte do shared library 
            } 
            when {
                anyOf {
                    branch pattern:  "feature*"
                    branch pattern:  "developer*"
                    branch pattern:  "hotfix*"
                    branch pattern:  "fix*"
                }
            }
        }

        stage('Quality Gate'){
            steps {
                sonarQualityGate {}
            }
            when {
                anyOf {
                    branch pattern:  "feature*"
                    branch pattern:  "developer*"
                    branch pattern:  "hotfix*"
                    branch pattern:  "fix*"
                }
            }
        }
    }
}
}