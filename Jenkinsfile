pipeline {
    agent {
        kubernetes {
            label 'molgenis-jdk11'
        }
    }
    environment {
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
    }
    stages {
        stage('Retrieve build secrets') {
            steps {
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=value secret/ops/token/github', returnStdout: true)
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                    }
                }
                dir("${JENKINS_AGENT_WORKDIR}/.m2") {
                    stash includes: 'settings.xml', name: 'maven-settings'
                }
                dir("${JENKINS_AGENT_WORKDIR}/.rancher") {
                    stash includes: 'cli2.json', name: 'rancher-config'
                }
            }
        }
        stage('Steps [ master ]') {
            when {
                branch 'master'
            }
            stages {
                stage('Build, Test [ master ]') {
                    steps {
                        container('maven') {
                            sh "psql -c 'create database molgenis;' -U postgres"
                            sh "psql -c \"CREATE USER molgenis WITH SUPERUSER PASSWORD 'molgenis';\" -U postgres"
                            sh "psql -c \"grant all privileges on database molgenis to molgenis;\" -U postgres"
                            sh "./gradlew test --info"
                        }
                    }
                }
            }
        }
    }
}
