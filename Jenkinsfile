podTemplate(inheritFrom:'shared', containers: [
    containerTemplate(
      name: 'java',
      image: 'adoptopenjdk:13-jdk-hotspot-bionic',
      ttyEnabled: true,
      command: 'cat',
      args: '',
      resourceRequestCpu: '1',
      resourceRequestMemory: '1Gi'),
    containerTemplate(
      name: 'postgres',
      image: 'postgres:13-alpine',
      workingDir:'',
      command: '',
      args: '-c shared_buffers=256MB -c max_locks_per_transaction=1024',
      resourceRequestCpu: '1',
      resourceRequestMemory: '1Gi',
      resourceLimitMemory: '1Gi',
      envVars: [
        envVar(key: 'POSTGRES_USER', value: 'molgenis'),
        envVar(key: 'POSTGRES_PASSWORD', value: 'molgenis'),
        envVar(key: 'POSTGRES_DB', value: 'molgenis')
      ]
    )
  ]) {
  node(POD_LABEL) {
    environment {
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
    }
    stage('Retrieve build secrets') {
        container('vault') {
            script {
                sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                env.GITHUB_TOKEN = sh(script: 'vault read -field=value secret/ops/token/github', returnStdout: true)
                env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                env.DOCKER_USERNAME = sh(script: 'vault read -field=username secret/gcc/account/dockerhub', returnStdout: true)
                env.DOCKER_PASSWORD = sh(script: 'vault read -field=password secret/gcc/account/dockerhub', returnStdout: true)
            }
        }
        dir("${JENKINS_AGENT_WORKDIR}/.m2") {
            stash includes: 'settings.xml', name: 'maven-settings'
        }
        dir("${JENKINS_AGENT_WORKDIR}/.rancher") {
            stash includes: 'cli2.json', name: 'rancher-config'
        }
    }
    stage('Build, Test') {
        container('java') {
            checkout scm
            sh "apt update"
            sh "apt -y install git"
            sh "apt -y install docker.io"
            sh "git fetch --depth 1000"
            sh "git config user.email \"m.a.swertz@rug.nl\""
            sh "git config user.name \"Jenkins-CI\""
            sh "./gradlew test jacocoMergedReport sonarqube shadowJar jib -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io -Djib.to.auth.username=${DOCKER_USERNAME} -Djib.to.auth.password=${DOCKER_PASSWORD}"
        }
    }

  }
}
