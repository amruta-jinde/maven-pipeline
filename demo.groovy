pipeline {
    agent any
   
    environment {
        APP_NAME = 'mvn-project'
        DOCKERHUB_USER = 'amruta2010'
        IMAGE = "${DOCKERHUB_USER}/${APP_NAME}"
    }
 
    stages {
        stage('pull'){
            steps {
                git branch: 'main', url: 'https://github.com/amruta-jinde/mvn-project.git'
                echo "pulling successfully!"
            }
        }
 
        stage('Build'){
            steps {
                sh 'mvn clean package'
                echo "building successfully!"
            }
        }
 
         stage('Test'){
             steps {
                withSonarQubeEnv(installationName: 'sonar-server', credentialsId: 'sonar-token'){
                    sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=studentsapp'
                }
                echo "testing successfully!"
            }
         } 
 
 
        // Docker Image Build Stage
        stage('Docker Build') {
            steps {
                script {
                    def shortSha = sh(returnStdout: true, script: "git rev-parse --short HEAD").trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortSha}"
                }
                sh "docker build -t ${IMAGE}:${IMAGE_TAG} ."
                sh "docker images | grep ${APP_NAME}"
                echo "Docker image built successfully!"
            }
        }
 
        // Docker Image Push to Dockerhub Stage        
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh """
                      echo "$PASS" | docker login -u "$USER" --password-stdin
                      docker tag ${IMAGE}:${IMAGE_TAG} ${IMAGE}:latest
                      docker push ${IMAGE}:${IMAGE_TAG}
                      docker push ${IMAGE}:latest
                      docker logout
                    """
                }
                echo "Image pushed successfully to Docker Hub!"
            }
        }
 
        // Run Container
        stage('Deploy with Docker') {
           steps {
               sh """
                  docker rm -f ${APP_NAME} || true
                  docker run -d --name ${APP_NAME} -p 8083:8080 ${IMAGE}:latest
                """
                echo "Container deployed successfully and running on port 8083!"
            }
        }
    }
}
 