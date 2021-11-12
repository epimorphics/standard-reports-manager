.PHONY:	image publish run tag test vars

ACCOUNT?=$(shell aws sts get-caller-identity | jq -r .Account)
AWS_REGION?=eu-west-1
STAGE?=dev
NAME?=$(shell awk -F: '$$1=="name" {print $$2}' deployment.yaml | sed -e 's/[[:blank:]]//g')
ECR?=${ACCOUNT}.dkr.ecr.eu-west-1.amazonaws.com
TAG?=$(shell if git describe > /dev/null 2>&1 ; then   git describe; else   git rev-parse --short HEAD; fi)

IMAGE?=${NAME}/${STAGE}
REPO?=${ECR}/${IMAGE}

all: publish

image: 
	@echo Building ${REPO}:${TAG} ...
	@docker build --tag ${REPO}:${TAG} .
	@echo Done.

publish: image
	@echo Publishing image: ${REPO}:${TAG} ...
	@docker push ${REPO}:${TAG} 2>&1
	@echo Done.

run:
	@docker run --network host -p 8080:8080 --rm --name sr-manager ${REPO}:${TAG}

tag:
	@echo ${TAG}

vars:
	@echo "Docker: ${REPO}:${TAG}"
