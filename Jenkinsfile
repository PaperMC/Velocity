pipeline {
  agent {
    docker {
      image 'gradle:jdk8-slim'
      args '-v m2-cache:/root/.gradle'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh './gradlew shadowJar'
        archiveArtifacts 'build/libs/*.jar'
      }
    }
    stage('Test') {
      steps {
        sh './gradlew test'
        junit 'build/test-results/test/*.xml'
      }
    }
  }
}