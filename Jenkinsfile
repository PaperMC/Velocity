pipeline {
  agent none
  stages {
    stage('Build') {
      agent {
        docker {
          image 'velocitypowered/openjdk8-plus-git:slim'
          args '-v gradle-cache:/root/.gradle:rw'
        }
      }
      steps {
        sh './gradlew build'
        archiveArtifacts 'proxy/build/libs/*-all.jar,api/build/libs/*-all.jar'
      }
    }
    stage('Deploy Artifacts') {
      when {
        expression {
          GIT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
          return GIT_BRANCH == 'master'
        }
      }
      agent {
        docker {
          image 'velocitypowered/openjdk8-plus-git:slim'
          args '-v maven-repo:/maven-repo:rw'
        }
      }
      steps {
        sh 'export MAVEN_DEPLOYMENT=true; ./gradlew publish'
      }
    }
    stage('Deploy Javadoc') {
      when {
        expression {
          GIT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
          return GIT_BRANCH == 'master'
        }
      }
      agent {
        docker {
          image 'velocitypowered/openjdk8-plus-git:slim'
          args '-v javadoc:/javadoc:rw'
        }
      }
      steps {
        sh 'rsync -av --delete ./api/build/docs/javadoc/ /javadoc'
      }
    }
  }
}