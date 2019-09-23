// Define Global variables
def server = Artifactory.newServer url: 'http://192.168.169.130:8081/artifactory', username: 'admin', password: 'password!'
def buildInfo = Artifactory.newBuildInfo()

pipeline {
	agent any
	options {
		buildDiscarder(logRotator(daysToKeepStr: '3', numToKeepStr: '5'))
	}
	stages {
   		stage('Check Out Code') {
			 steps {
    				git branch: 'test-promotion', url: 'https://github.com/roguetech/simple-java-maven-app.git'
   			}
		  }
   		stage('Build') {
			 steps {
    				sh "mvn clean compile test"
   		 }
		  }
   		stage('Push') {
			// This stage uploads the build to artifactory, it also states a build retention for the artifactory repository
			// The this means is that it will delete old artifactory build
			 steps {
				script {
        	def uploadSpec = """{
          	"files": [
            	{
              	"pattern": "*",
								"target": "libs-release-local/com/vsware/services/test-app1/${BUILD_NUMBER}/"
              }]
          }"""
					// Below you can change when you want to delete the Artifacts, maxBuilds and MaxDays to keep the artifacts
					buildInfo.retention maxBuilds: 5, maxDays: 7, deleteBuildArtifacts: true
        	server.upload spec: uploadSpec, buildInfo: buildInfo
        	server.publishBuildInfo buildInfo
   			}
			 }
			}
   		stage('Promotion') {
				// This stage promotes the build into a different repository if the the git tag prod exists
				when { expression {
				     sh(returnStdout: true, script: "git tag --sort version:refname").trim() == "prod"}
				}
				steps {
					script {
					   def promotionConfig = [
                        // Mandatory parameters
                        'buildName'          : buildInfo.name,
                        'buildNumber'        : buildInfo.number,
                        'targetRepo'         : 'mvn-prod',
                        // Optional parameters
                        'comment'            : 'moving to prod',
                        //'sourceRepo'         : 'libs-staging-local',
                        'status'             : 'Released',
                        'includeDependencies': true,
                        'copy'               : true,
                        // 'failFast' is true by default.
                        // Set it to false, if you don't want the promotion to abort upon receiving the first error.
                        'failFast'           : true
                    ]
              server.promote promotionConfig
					 }
				}
		}
	}
}
