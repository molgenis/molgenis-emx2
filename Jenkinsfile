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
        stage('Retrieve build secrets') {
            container('vault') {
                script {
                    sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                    sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                    sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                    sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                    env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                    env.GITHUB_TOKEN = sh(script: 'vault read -field=token-emx2 secret/ops/token/github', returnStdout: true)
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
                script {
                    checkout scm
                    sh 'apt update'
                    sh 'apt -y install git'
                    sh 'apt -y install docker.io'
                    sh 'git fetch --depth 1000'
                    sh "git config user.email \"m.a.swertz@rug.nl\""
                    sh "git config user.name \"molgenis-jenkins\""
                    sh 'git config url.https://.insteadOf git://'
                    sh "set +x && echo \"${DOCKER_PASSWORD}\" | docker login -u \"${DOCKER_USERNAME}\" --password-stdin"
                    sh "./gradlew test jacocoMergedReport sonarqube shadowJar jib release ci \
                        -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io \
                        -Dorg.ajoberstar.grgit.auth.username=${GITHUB_TOKEN} -Dorg.ajoberstar.grgit.auth.password"  
                    def props = readProperties file: 'build/ci.properties'
                    env['TAG_NAME'] = props['tagName']
                }
            }
            container('rancher') {
                script {
                    if (env.TAG_NAME && !env.TAG_NAME.contains('-SNAPSHOT')) {
                        sh 'rancher context switch dev-molgenis'
                        sh "rancher apps upgrade --set image.tag=${TAG_NAME} --force molgenis-emx2 0.0.13"
                    }
                }
            }
        }
    }
}
