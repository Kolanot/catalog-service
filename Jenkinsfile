node ('nimble-jenkins-slave') {

    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/catalog-service.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common   '
        sh 'git clone https://github.com/nimble-platform/common'
        dir ('common') {
            sh 'mvn clean install'
        }
    }

    stage ('Build Java') {
        sh '/bin/bash -xe deploy.sh java-build'
    }

    stage ('Build Docker') {
        sh '/bin/bash -xe deploy.sh docker-build'
    }

    if (env.BRANCH_NAME == 'master') {
        stage('Push Docker') {
            withDockerRegistry([credentialsId: 'NimbleDocker']) {
                sh '/bin/bash -xe deploy.sh docker-push'
            }
        }

        stage('Apply to Cluster') {
            // disabled for now...cluster is down
            //        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
        }
    }
}