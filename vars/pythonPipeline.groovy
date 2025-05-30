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
        environment {
            DISCORD_WEBHOOK = credentials('discord-webhook')
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

            stage('Harbor Security Scan') {
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

            stage('Crane Artifact promotion') {
                steps {
                    artifactPromotion { }
                }
                when {
                    anyOf {
                        branch pattern:  "release*"
                        branch pattern:  'v*'
                    }
                }
            }

            stage('Integration/infrastructure test') {
                environment {
                    JENKINS_GITEA_PRIVATE_KEY = credentials('jenkins-gitea')
                }

                steps {
                    infrastructureTest { }
                }
                when {
                    anyOf {
                        branch pattern:  'hotfix*'
                        branch pattern:  'developer'
                    }
                }
                post {
                    always {
                        container('helm') {
                            sh '''
                                REPOSITORY=${JOB_NAME%/*}

                                helm delete -n citest ${REPOSITORY}-ci
                            '''
                        }
                    }
                }
            }

            stage('Deploy to Development') {
                environment {
                    ARGOCD_GITEA_PRIVATE_KEY = credentials('argocd-gitea')
                }

                steps {
                    deployDev { }
                }
                when {
                    anyOf {
                        branch pattern:  'developer'
                    }
                }
            }
            
            stage('Deploy to Staging') {
                environment {
                    ARGOCD_GITEA_PRIVATE_KEY = credentials('argocd-gitea')
                }

                steps {
                    deployStg { }
                }
                when {
                    anyOf {
                        branch pattern:  "release*"
                        branch pattern:  'hotfix*'
                    }
                }
            }

            stage('Creating tag') {
                environment {
                    ARGOCD_GITEA_PRIVATE_KEY = credentials('argocd-gitea')
                }

                steps {
                    input message: "Would you like to promote to production?"
                    createTag { }
                }
                when {
                    anyOf {
                        branch pattern:  "release*"
                        branch pattern:  'hotfix*'
                    }
                }
            }
            stage('Deploy to Production') {
                environment {
                    ARGOCD_GITEA_PRIVATE_KEY = credentials('argocd-gitea')
                }

                steps {
                    input message: "Would you like to promote to production?"
                    deployPrd { }
                }
                when {
                    anyOf {
                        branch pattern:  "v*"
                    }
                }
            }
        }
        post {
            always {
                discordSend description: "Jenkins Pipeline Build",
                    footer: "${JOB_BASE_NAME} (build #${BUILD_NUMBER})",
                    link: "${BUILD_URL}",
                    result: currentBuild.currentResult,
                    title: "${JOB_NAME}",
                    webhookURL: "${DISCORD_WEBHOOK}",
                    thumbnail: "https://www.errietta.me/blog/wp-content/uploads/2019/08/256.png"
            }
        }
    }
}