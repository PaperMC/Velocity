pipeline {
  agent none
  options {
    disableConcurrentBuilds()
  }
  stages {
    stage('Build') {
      agent {
        docker {
          image 'velocitypowered/openjdk8-plus-git:slim'
          args '-v gradle-cache:/root/.gradle:rw'
        }
      }
      steps {
        sh './gradlew build --no-daemon'
        archiveArtifacts 'proxy/build/libs/*-all.jar,api/build/libs/*-all.jar'
      }
    }
    stage('Deploy') {
      when {
        expression {
          GIT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
          return GIT_BRANCH == 'master'
        }
      }
      agent {
        docker {
          image 'velocitypowered/openjdk8-plus-git:slim'
          args '-v gradle-cache:/root/.gradle:rw -v maven-repo:/maven-repo:rw -v javadoc:/javadoc:rw'
        }
      }
      steps {
        sh 'export MAVEN_DEPLOYMENT=true; ./gradlew publish --no-daemon'
        sh 'rsync -av --delete ./api/build/docs/javadoc/ /javadoc'
      }
    }
  }
}