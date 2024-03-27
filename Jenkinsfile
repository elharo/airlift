pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        GPG_SECRET     = credentials('presto-release-gpg-secret')
        GPG_TRUST      = credentials("presto-release-gpg-trust")
        GPG_PASSPHRASE = credentials("presto-release-gpg-passphrase")

        GITHUB_OSS_TOKEN_ID = 'github-personal-token-wanglinsong'

        SONATYPE_NEXUS_CREDS    = credentials('presto-sonatype-nexus-creds')
        SONATYPE_NEXUS_PASSWORD = "$SONATYPE_NEXUS_CREDS_PSW"
        SONATYPE_NEXUS_USERNAME = "$SONATYPE_NEXUS_CREDS_USR"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '100'))
        disableConcurrentBuilds()
        disableResume()
        overrideIndexTriggers(false)
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    parameters {
        booleanParam(name: 'PERFORM_MAVEN_RELEASE',
                     defaultValue: false,
                     description: 'Release and update development version')
    }

    stages {
        stage('Setup') {
            steps {
                sh 'apt update && apt install -y bash build-essential git gpg python3 python3-venv'
            }
        }

        stage ('Checkout') {
            steps {
                checkout $class: 'GitSCM',
                         branches: [[name: '*/master']],
                         doGenerateSubmoduleConfigurations: false,
                         extensions: [[
                             $class: 'RelativeTargetDirectory',
                             relativeTargetDir: 'airlift'
                         ]],
                         submoduleCfg: [],
                         userRemoteConfigs: [[
                             credentialsId: "${GITHUB_OSS_TOKEN_ID}",
                             url: 'https://github.com/prestodb/airlift.git'
                         ]]
                 sh '''
                    cd airlift
                    git config --global --add safe.directory ${PWD}
                    git config --global user.email "oss-release-bot@prestodb.io"
                    git config --global user.name "oss-release-bot"
                    git config pull.rebase false
                    git switch -c master
                    git branch
                '''
            }
        }

        stage ('Prepare to Release') {
            steps {
                script {
                    env.AIRLIFT_CURRENT_VERSION = sh(
                        script: 'cd airlift; mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -ntp -DforceStdout',
                        returnStdout: true).trim()
                }
                echo "Airlift current version: ${AIRLIFT_CURRENT_VERSION}"

                sh '''
                    cd airlift
                    mvn release:prepare -B -DskipTests \
                        -DautoVersionSubmodules=true \
                        -DgenerateBackupPoms=false

                    git branch
                    git log --pretty="format:%ce: %s" -8
                    SCM_TAG=$(cat release.properties | grep scm.tag=)
                    echo ${SCM_TAG#*=} > airlift-release-tag.txt
                '''

                script {
                    env.AIRLIFT_RELEASE_TAG = sh(
                        script: 'cat airlift/airlift-release-tag.txt',
                        returnStdout: true).trim()
                }
                echo "Airlift release tag: ${AIRLIFT_RELEASE_TAG}"
            }
        }

        stage ('Release Airlift') {
            when {
                expression { params.PERFORM_MAVEN_RELEASE }
            }

            steps {
                echo "Update GitHub Repo"

                sh '''#!/bin/bash -ex
                    export GPG_TTY=${TTY}
                    echo $GPG_TTY
                    gpg --batch --import ${GPG_SECRET}
                    echo ${GPG_TRUST} | gpg --import-ownertrust -
                    gpg --list-secret-keys
                    echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
                '''

                withCredentials([usernamePassword(
                        credentialsId: "${GITHUB_OSS_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd airlift
                        git --no-pager log --since="60 days ago" --graph --pretty=format:'%C(auto)%h%d%Creset %C(cyan)(%cd)%Creset %C(green)%cn <%ce>%Creset %s'
                        head -n 18 pom.xml
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/airlift.git"
                        git push --follow-tags --set-upstream ${ORIGIN} master
                    '''
                }

                echo "Release Airlift ${AIRLIFT_RELEASE_TAG} maven artifacts"
                sh '''#!/bin/bash -ex
                    export GPG_TTY=${TTY}

                    cd airlift
                    git checkout ${AIRLIFT_RELEASE_TAG}
                    git status
                    git branch
                    git log -8
                    head -n 18 pom.xml

                    mvn -s ${WORKSPACE}/settings.xml -V -B -U -e -T2C deploy \
                        -DautoReleaseAfterClose=true \
                        -Dgpg.passphrase=${GPG_PASSPHRASE} \
                        -DkeepStagingRepositoryOnCloseRuleFailure=true \
                        -DkeepStagingRepositoryOnFailure=true \
                        -DskipTests \
                        -DstagingProfileId=28a0d8c4350ed \
                        -DstagingProgressTimeoutMinutes=60
                '''
            }
        }
    }
}
