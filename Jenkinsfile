pipeline {
  agent {
    docker {
      image 'openjdk:8-jdk-slim'
      args '-v gradle-cache:/root/.gradle:rw'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh './gradlew shadowJar'
        archiveArtifacts 'proxy/build/libs/*-all.jar'
      }
    }
    stage('Test') {
      steps {
        sh './gradlew test'
      }
    }
  }
}