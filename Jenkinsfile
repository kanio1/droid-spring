#!/usr/bin/env groovy

// BSS CI/CD Pipeline
// Supports: dev, staging, production environments
// Features: multi-branch, parallel tests, security scanning, deployment

pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        preserveStashes(buildCount: 5)
        parallelsAlwaysFailFast()
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Target environment for deployment'
        )
        choice(
            name: 'SERVICE',
            choices: ['all', 'backend', 'frontend'],
            description: 'Service to deploy'
        )
        booleanParam(
            name: 'RUN_LOAD_TESTS',
            defaultValue: false,
            description: 'Run load tests after deployment'
        )
        booleanParam(
            name: 'SKIP_DB_BACKUP',
            defaultValue: false,
            description: 'Skip database backup (not recommended for production)'
        )
    }

    environment {
        JAVA_VERSION = '21'
        MAVEN_VERSION = '3.9'
        NODE_VERSION = '20'
        PNPM_VERSION = '9'

        // Docker registry
        REGISTRY = 'ghcr.io'
        IMAGE_NAME = "${GITHUB_REPOSITORY ?: 'bss'}"

        // Environment variables
        DEV_DB_URL = credentials('dev-db-url')
        STAGING_DB_URL = credentials('staging-db-url')
        PROD_DB_URL = credentials('prod-db-url')

        BACKEND_IMAGE = "${REGISTRY}/${IMAGE_NAME}/backend:${BRANCH_NAME}-${BUILD_NUMBER}"
        FRONTEND_IMAGE = "${REGISTRY}/${IMAGE_NAME}/frontend:${BRANCH_NAME}-${BUILD_NUMBER}"
    }

    stages {
        stage('Prepare') {
            parallel {
                stage('Backend Preparation') {
                    when {
                        anyOf {
                            changeset 'backend/**'
                            equals expected: true, actual: true // Always run if parameter is set
                        }
                    }
                    steps {
                        dir('backend') {
                            script {
                                sh 'mvn -B -version'
                            }
                        }
                    }
                }

                stage('Frontend Preparation') {
                    when {
                        anyOf {
                            changeset 'frontend/**'
                            equals expected: true, actual: true
                        }
                    }
                    steps {
                        dir('frontend') {
                            script {
                                sh 'node -v'
                                sh 'pnpm -v'
                            }
                        }
                    }
                }

                stage('Infrastructure Check') {
                    steps {
                        script {
                            sh '''
                                docker --version
                                docker-compose --version
                                which psql
                            '''
                        }
                    }
                }
            }
        }

        stage('Test') {
            parallel {
                stage('Backend Unit Tests') {
                    when {
                        anyOf {
                            changeset 'backend/**'
                            branch 'main'
                            branch 'develop'
                        }
                    }
                    steps {
                        dir('backend') {
                            sh '''
                                mvn -B clean verify \
                                    -DskipTests=false \
                                    -Dspring.profiles.active=test
                            '''
                        }
                    }
                    post {
                        always {
                            junit 'backend/target/surefire-reports/**/*.xml'
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'backend/target/site/jacoco',
                                reportFiles: 'index.html',
                                reportName: 'Backend Coverage Report'
                            ])
                        }
                    }
                }

                stage('Backend Integration Tests') {
                    when {
                        anyOf {
                            changeset 'backend/**'
                            branch 'main'
                        }
                    }
                    steps {
                        dir('backend') {
                            sh '''
                                # Start test infrastructure
                                docker-compose -f dev/compose.yml up -d postgres redis kafka

                                # Wait for services
                                sleep 30

                                # Run tests
                                mvn -B verify \
                                    -Dspring.datasource.url=jdbc:postgresql://localhost:5432/bss \
                                    -Dspring.datasource.username=bss_app \
                                    -Dspring.datasource.password=${POSTGRES_PASSWORD} \
                                    -Dspring.redis.host=localhost \
                                    -Dspring.kafka.bootstrap-servers=localhost:9092

                                # Cleanup
                                docker-compose -f dev/compose.yml down
                            '''
                        }
                    }
                }

                stage('Frontend Tests') {
                    when {
                        anyOf {
                            changeset 'frontend/**'
                            branch 'main'
                            branch 'develop'
                        }
                    }
                    steps {
                        dir('frontend') {
                            sh '''
                                pnpm install --frozen-lockfile
                                pnpm run lint
                                pnpm run typecheck
                                pnpm run test:unit -- --run
                            '''
                        }
                    }
                    post {
                        always {
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'frontend/coverage',
                                reportFiles: 'index.html',
                                reportName: 'Frontend Coverage Report'
                            ])
                        }
                    }
                }
            }
        }

        stage('Build') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            parallel {
                stage('Build Backend') {
                    when {
                        anyOf {
                            changeset 'backend/**'
                            equals expected: true, actual: true
                        }
                    }
                    steps {
                        dir('backend') {
                            sh '''
                                mvn -B clean package -DskipTests
                                docker build -t ${BACKEND_IMAGE} .
                            '''
                        }
                    }
                }

                stage('Build Frontend') {
                    when {
                        anyOf {
                            changeset 'frontend/**'
                            equals expected: true, actual: true
                        }
                    }
                    steps {
                        dir('frontend') {
                            sh '''
                                pnpm install --frozen-lockfile
                                pnpm run build
                                docker build -t ${FRONTEND_IMAGE} .
                            '''
                        }
                    }
                }
            }
        }

        stage('Security Scan') {
            parallel {
                stage('SAST Scan') {
                    steps {
                        sh '''
                            # Run SonarQube or other SAST tools
                            echo "Running SAST scan..."
                        '''
                    }
                }

                stage('Container Scan') {
                    steps {
                        sh '''
                            # Scan Docker images for vulnerabilities
                            trivy image ${BACKEND_IMAGE}
                            trivy image ${FRONTEND_IMAGE}
                        '''
                    }
                }

                stage('Dependency Check') {
                    steps {
                        dir('backend') {
                            sh 'mvn -B dependency-check:check'
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    equals expected: true, actual: true
                }
            }
            steps {
                script {
                    // Determine environment
                    def targetEnv = params.ENVIRONMENT
                    if (env.BRANCH_NAME == 'main') {
                        targetEnv = 'production'
                    } else if (env.BRANCH_NAME == 'develop') {
                        targetEnv = 'staging'
                    }

                    // Create database backup
                    if (!params.SKIP_DB_BACKUP && targetEnv != 'dev') {
                        sh """
                            ./dev/database/backup/restore/backup.sh \
                                --env ${targetEnv} \
                                --output /tmp/backup-${targetEnv}-${BUILD_NUMBER}.sql
                        """
                    }

                    // Deploy
                    sh """
                        echo "Deploying to ${targetEnv}..."
                        # Add deployment commands here (kubectl, docker-compose, etc.)
                        # Example:
                        # kubectl set image deployment/backend backend=${BACKEND_IMAGE}
                        # kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE}
                    """

                    // Run database migrations
                    sh '''
                        cd backend
                        mvn -B flyway:migrate \
                            -Dflyway.url=${DB_URL} \
                            -Dflyway.user=${DB_USER} \
                            -Dflyway.password=${DB_PASSWORD}
                    '''
                }
            }
        }

        stage('Post-Deployment Tests') {
            when {
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'develop'
                    }
                    environment name: 'ENVIRONMENT', value: 'staging'
                }
            }
            steps {
                script {
                    sh '''
                        sleep 60  # Wait for deployment to be ready

                        # Health checks
                        curl -f ${API_URL}/actuator/health
                        curl -f ${FRONTEND_URL}

                        # Load tests (if requested)
                        if [ "${RUN_LOAD_TESTS}" = "true" ]; then
                            cd dev/k6/scripts
                            k6 run --vus 100 --duration 5m spike-test.js
                        fi
                    '''
                }
            }
        }

        stage('Load Testing') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    equals expected: true, actual: true
                }
            }
            steps {
                script {
                    if (params.RUN_LOAD_TESTS) {
                        sh '''
                            cd dev/k6/scripts

                            # Spike test
                            k6 run --vus 1000 --duration 5m spike-test.js

                            # Volume test
                            k6 run --vus 500 --iterations 2000 volume-test-1m.js
                        '''
                    }
                }
            }
        }

        stage('Generate Report') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                sh '''
                    mkdir -p build-reports
                    cat > build-reports/deployment-report.md << 'EOF'
                    # Deployment Report

                    **Date:** $(date)
                    **Environment:** ${ENVIRONMENT}
                    **Branch:** ${BRANCH_NAME}
                    **Build:** ${BUILD_NUMBER}
                    **Commit:** ${GIT_COMMIT}

                    ## Images Deployed
                    - Backend: ${BACKEND_IMAGE}
                    - Frontend: ${FRONTEND_IMAGE}

                    ## Test Results
                    - Unit Tests: ${UNIT_TEST_RESULT}
                    - Integration Tests: ${INTEGRATION_TEST_RESULT}
                    - E2E Tests: ${E2E_TEST_RESULT}
                    - Load Tests: ${LOAD_TEST_RESULT}
                    EOF
                '''

                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'build-reports',
                    reportFiles: 'deployment-report.md',
                    reportName: 'Deployment Report'
                ])
            }
        }
    }

    post {
        always {
            sh '''
                # Cleanup
                docker system prune -f
                docker volume prune -f
            '''

            // Archive artifacts
            archiveArtifacts artifacts: 'backend/target/*.jar, frontend/.output/**', allowEmptyArchive: true

            // Publish test results
            publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'

            // Cleanup workspace
            cleanWs()
        }

        success {
            // Notify on success
            emailext (
                subject: "Build Successful: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build completed successfully.\nView the build at: ${env.BUILD_URL}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }

        failure {
            // Notify on failure
            emailext (
                subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build failed. Please check the build logs.\nView the build at: ${env.BUILD_URL}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }

        unstable {
            // Notify on unstable build
            emailext (
                subject: "Build Unstable: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build completed with test failures.\nView the build at: ${env.BUILD_URL}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
