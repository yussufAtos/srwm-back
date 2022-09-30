pipeline {
    agent { label 'srbd' }

    tools {
        maven 'maven-3.6.3'
        jdk 'jdk-17-SRBD'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        disableConcurrentBuilds() 
        gitLabConnection('Gitlab')
    }

    environment {
        srbdGitCredId='git_srbd'
        mavenSettingsId='afp-global-maven-settings'
        projectName='iris-sr-wm'
        sonarProjectName="$projectName:$BRANCH_NAME"
        scannerHome = tool 'sonar Scanner'
    }

    stages {
    
        stage('Checkout') {
            steps {
                updateGitlabCommitStatus name: 'build', state: 'running'
                git branch: "${env.BRANCH_NAME}",
                        credentialsId: "${env.srbdGitCredId}",
                        url: "ssh://git@gitlab.afp.com:2222/srbe/${env.projectName}.git"
            }
        }
        
        stage('Build') {
            steps {
                withMaven(globalMavenSettingsConfig: env.mavenSettingsId, maven: 'maven-3.6.3', publisherStrategy: 'EXPLICIT') {
                    sh 'mvn clean package'
                    archiveArtifacts artifacts: '**/*rpm', fingerprint: true
                } 
            }
        }
        
        stage('Coverage') {
            steps {
                jacoco buildOverBuild: true,
                       changeBuildStatus: false,
                       deltaBranchCoverage: '2',
                       deltaClassCoverage: '2',
                       deltaComplexityCoverage: '2',
                       deltaInstructionCoverage: '2',
                       deltaLineCoverage: '2',
                       deltaMethodCoverage: '2',
                       maximumBranchCoverage: '70',
                       maximumClassCoverage: '70',
                       maximumComplexityCoverage: '70',
                       maximumInstructionCoverage: '70',
                       maximumLineCoverage: '70',
                       maximumMethodCoverage: '70',
                       minimumBranchCoverage: '70',
                       minimumClassCoverage: '70',
                       minimumComplexityCoverage: '70',
                       minimumInstructionCoverage: '70',
                       minimumLineCoverage: '70',
                       minimumMethodCoverage: '70',
                       exclusionPattern: '**/*MonitoringController.class, **/*SoftConfigurations.class',
                       sourceExclusionPattern: '**/*MonitoringController.java, **/*SoftConfigurations.java'

            }
        }

 	//	stage('Sonar') {
    //        steps {
    //        
    //            withSonarQubeEnv('sonarQube') {
    //               sh '''${scannerHome}/bin/sonar-scanner\
    //                 -Dsonar.projectVersion=1.0 \
    //                 -Dsonar.sourceEncoding=UTF-8 \
    //                 -Dsonar.language=java \
    //                 -Dsonar.java.source=17 \
    //                 -Dsonar.java.binaries=**/target/classes \
    //                 -Dsonar.projectKey=$sonarProjectName \
    //                 -Dsonar.projectName=$sonarProjectName \
    //                 -Dsonar.sources=.\
    //                 -Dsonar.test.inclusions=**/src/test/**\
    //                 -Dsonar.exlusions=**/src/test/**\
    //                 -Dsonar.inclusions=**/*.java\
    //                 '''
    //            }      
                
    //            timeout(time: 10, unit: 'MINUTES') {
    //           		 waitForQualityGate abortPipeline: true
	//			}
    //        }
    //    }
   
   
    }
    
    post { 
        unsuccessful {
          updateGitlabCommitStatus name: 'build', state: 'failed'
          script {
                if(env.BRANCH_NAME == 'master') {
                      emailext body: "see :  ${env.BUILD_URL}", to: 'ServiceSRBD@afp.com', subject: "Build failed in Jenkins: #${env.JOB_NAME} #${env.BUILD_NUMBER}"
                } else {
                    
                     emailext body: "see :  ${env.BUILD_URL}", recipientProviders: [requestor()], subject: "Build failed in Jenkins: #${env.JOB_NAME} #${env.BUILD_NUMBER}"
                }
          }    
        }
        success {
          updateGitlabCommitStatus name: 'build', state: 'success'
        }
        always { 
            cleanWs()
        }
    }
}
