pipeline {
    agent any

    tools {
        // Doit correspondre au nom dans Manage Jenkins > Tools > Installations JDK
        jdk 'JAVA_HOME'
        // Node pour scripts/wait-quality-gate.js
        nodejs 'Nodejs'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        // Projet à la racine (pas de sous-dossier type api-gateway)
        IMAGE = 'mouadhfersi/edugame-auth-backend'
        CONTAINER_NAME = 'edugame-auth-backend'
        APP_PORT = '8081'
        SONAR_PROJECT_KEY = 'edugame-auth-backend'
        SONAR_PROJECT_NAME = 'EduGame Auth Backend'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -eu
                    . scripts/ensure-java-home.sh
                    chmod +x mvnw
                    ./mvnw -B -q clean package -DskipTests
                    ls -la target/*.jar
                '''
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: false
            }
        }

        stage('Tests & Coverage') {
            steps {
                sh '''
                    set -eu
                    . scripts/ensure-java-home.sh
                    ./mvnw -B -q test
                    test -f target/site/jacoco/jacoco.xml
                    echo "Rapport Jacoco : target/site/jacoco/jacoco.xml"
                '''
            }
        }

        stage('Analyse SonarQube') {
            steps {
                withSonarQubeEnv(installationName: 'SonarQube',
                                 credentialsId: 'sonarqube-token') {
                    sh '''
                        set -eu
                        . scripts/ensure-java-home.sh
                        ./mvnw -B -q \
                          org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                          -Dsonar.java.binaries=target/classes \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                withSonarQubeEnv(installationName: 'SonarQube',
                                 credentialsId: 'sonarqube-token') {
                    sh '''
                        set -eu
                        node scripts/wait-quality-gate.js
                    '''
                }
            }
        }

        stage('Image Docker') {
            environment {
                TAG = "${env.BUILD_NUMBER}"
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-token',
                                                   usernameVariable: 'REG_USER',
                                                   passwordVariable: 'REG_PASS')]) {
                    sh '''
                        set -e
                        echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin
                        docker build -t "$IMAGE:$TAG" -t "$IMAGE:latest" .

                        push_with_retry() {
                            ref="$1"
                            for attempt in 1 2 3 4 5; do
                                if docker push "$ref"; then
                                    return 0
                                fi
                                echo "Push echoue (tentative $attempt/5), nouvel essai dans 10s..."
                                sleep 10
                            done
                            return 1
                        }

                        if push_with_retry "$IMAGE:$TAG" && push_with_retry "$IMAGE:latest"; then
                            echo "Image publiee : $IMAGE:$TAG"
                        else
                            echo "ERREUR : Docker Hub inaccessible. Kubernetes doit pouvoir tirer $IMAGE:$TAG"
                            exit 1
                        fi
                        docker logout || true
                    '''
                }
            }
        }

        stage('Deploy') {
            environment {
                TAG = "${env.BUILD_NUMBER}"
            }
            steps {
                sh '''
                    set -eu
                    echo "Deploiement Docker de $IMAGE:$TAG"

                    docker pull "$IMAGE:$TAG"

                    docker stop "$CONTAINER_NAME" 2>/dev/null || true
                    docker rm "$CONTAINER_NAME" 2>/dev/null || true

                    docker run -d \
                      --name "$CONTAINER_NAME" \
                      --restart unless-stopped \
                      -p "${APP_PORT}:8081" \
                      "$IMAGE:$TAG"

                    echo "Conteneur demarre : $CONTAINER_NAME ($IMAGE:$TAG) sur le port $APP_PORT"
                    docker ps --filter "name=$CONTAINER_NAME"
                '''
            }
        }
    }

    post {
        always {
            echo "Pipeline termine — build #${env.BUILD_NUMBER}"
        }
        success {
            echo "Succes : image $IMAGE:${env.BUILD_NUMBER} deployee (conteneur $CONTAINER_NAME)"
        }
        failure {
            echo "Echec du pipeline — verifier les logs des stages ci-dessus"
        }
    }
}
