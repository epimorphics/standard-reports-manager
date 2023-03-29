.PHONY:	clean image publish test vars

ACCOUNT?=$(shell aws sts get-caller-identity | jq -r .Account)
NAME?=$(shell awk -F: '$$1=="name" {print $$2}' deployment.yaml | sed -e 's/\s//g')
STAGE?=dev
ECR?=${ACCOUNT}.dkr.ecr.eu-west-1.amazonaws.com
IMAGE?=${NAME}/${STAGE}
REPO?=${ECR}/${IMAGE}
SHORTNAME?=$(shell echo ${NAME} | cut -f2 -d/)

BRANCH:=$(shell git rev-parse --abbrev-ref HEAD)
COMMIT:=$(shell git rev-parse --short HEAD)
MVN_VERSION:=$(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
TAG?=$(shell printf '%s_%s_%08d' ${MVN_VERSION} ${COMMIT} ${GITHUB_RUN_NUMBER})

PORT?=8084

all: publish

image:
	@echo Building ${REPO}:${TAG} ...
	@docker build \
		--build-arg build_date=`date -Iseconds` \
		--build-arg git_branch=${BRANCH} \
		--build-arg git_commit_hash=${COMMIT} \
		--build-arg github_run_number=${GITHUB_RUN_NUMBER} \
		--build-arg image_name=${NAME} \
		--build-arg version=${MVN_VERSION} \
		--tag ${REPO}:${TAG} \
		.
	@echo Done.

publish: image
	@echo Tagging ${REPO}:${TAG} ...
	@docker tag ${IMAGE}:${TAG} ${REPO}:${TAG}
	@echo Publishing ${REPO}:${TAG} ...
	@docker push ${REPO}:${TAG}
	@echo Done.

run:
	@if docker network inspect dnet > /dev/null 2>&1; then echo "Using docker network dnet"; else echo "Create docker network dnet"; docker network create dnet; sleep 2; fi
	@if docker stop ${SHORTNAME} > /dev/null 2>&1; then sleep 5; fi
	@echo "Starting ${SHORTNAME} ..." 
	@docker run --network dnet -p ${PORT}:8080 -v $$(pwd)/dev/app.conf:/etc/standard-reports/app.conf --rm --name ${SHORTNAME} ${REPO}:${TAG}


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
	@echo SHORTNAME:${SHORTNAME}
	@echo TAG:${TAG}
