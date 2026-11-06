pipeline {
    agent any
    options { timestamps(); disableConcurrentBuilds() }

    environment {
        AWS_REGION = 'us-east-2'
        ECR_REPOSITORY = 'employee-api'
        EKS_CLUSTER = 'REPLACE_WITH_EKS_CLUSTER_NAME'
        K8S_NAMESPACE = 'devops'
        DEPLOYMENT_NAME = 'employee-api'
        CONTAINER_NAME = 'employee-api'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build and Unit Test') {
            steps {
                sh 'mvn -B -f application/pom.xml clean verify'
            }
            post {
                always {
                    junit testResults: 'application/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}"
                    env.AWS_ACCOUNT_ID = sh(
                        script: "aws sts get-caller-identity --query Account --output text",
                        returnStdout: true
                    ).trim()
                    env.ECR_REGISTRY = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
                    env.IMAGE_URI = "${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}"
                }
                sh 'docker build -f docker/Dockerfile -t "$IMAGE_URI" .'
            }
        }
        stage('Push Image to ECR') {
            steps {
                sh '''
                    aws ecr get-login-password --region "$AWS_REGION" |
                      docker login --username AWS --password-stdin "$ECR_REGISTRY"
                    docker push "$IMAGE_URI"
                '''
            }
        }
        stage('Deploy to EKS') {
            when { branch 'main' }
            steps {
                sh '''
                    aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER"
                    kubectl -n "$K8S_NAMESPACE" set image deployment/"$DEPLOYMENT_NAME"                       "$CONTAINER_NAME"="$IMAGE_URI"
                    kubectl -n "$K8S_NAMESPACE" rollout status deployment/"$DEPLOYMENT_NAME" --timeout=180s
                '''
            }
        }
    }
    post {
        success { echo 'Build, test, image publish and (on main) deployment completed.' }
        failure { echo 'Pipeline failed. Review the stage logs above.' }
        always { sh 'docker logout "$ECR_REGISTRY" || true' }
    }
}
