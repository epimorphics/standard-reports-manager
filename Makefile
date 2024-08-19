.PHONY:	clean image publish test vars

ACCOUNT?=$(shell aws sts get-caller-identity | jq -r .Account)
NAME?=$(shell awk -F: '$$1=="name" {print $$2}' deployment.yaml | sed -e 's/\s//g')
STAGE?=dev
ECR?=${ACCOUNT}.dkr.ecr.eu-west-1.amazonaws.com
IMAGE?=${NAME}/${STAGE}
REPO?=${ECR}/${IMAGE}

BRANCH:=$(shell git rev-parse --abbrev-ref HEAD)
COMMIT:=$(shell git rev-parse --short HEAD)
MVN_VERSION:=$(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
TAG?=$(shell printf '%s_%s_%08d' ${MVN_VERSION} ${COMMIT} ${GITHUB_RUN_NUMBER})

all: publish

image:
	@echo Building ${IMAGE}:${TAG} ...
	@docker build \
		--build-arg git_branch=${BRANCH} \
		--build-arg git_commit_hash=${COMMIT} \
		--build-arg github_run_number=${GITHUB_RUN_NUMBER} \
		--build-arg image_name=${NAME} \
		--build-arg version=${MVN_VERSION} \
		--tag ${IMAGE}:${TAG} \
		.
	@echo Done.

publish: image
	@echo Tagging ${REPO}:${TAG} ...
	@docker tag ${IMAGE}:${TAG} ${REPO}:${TAG}
	@echo Publishing ${REPO}:${TAG} ...
	@docker push ${REPO}:${TAG}
	@echo Done.

run:
	@docker run --network host -p 8080:8080 --rm --name sr-manager ${IMAGE}:${TAG}


tag:
	@echo ${TAG}

clean:
	@mvn clean

vars:
	@echo "Docker: ${REPO}:${TAG}"
	@echo NAME:${NAME}
	@echo MVN_VERSION:${MVN_VERSION}
	@echo BRANCH:${BRANCH}
	@echo COMMIT:${COMMIT}
	@echo IMAGE:${IMAGE}
	@echo REPO:${REPO}
	@echo TAG:${TAG}
