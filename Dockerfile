# Build
FROM maven:3.6-jdk-8 AS build  
COPY src /usr/src/app/src  
COPY pom.xml /usr/src/app  
RUN mvn -f /usr/src/app/pom.xml clean package

# Package
FROM tomcat:8.5.95-jdk8-corretto-al2
RUN yum -y install shadow-utils

## set up structured logging for tomcat
ARG ECS_LOGGING_VERSION=1.2.0
RUN curl -s https://repo1.maven.org/maven2/co/elastic/logging/ecs-logging-core/${ECS_LOGGING_VERSION}/ecs-logging-core-${ECS_LOGGING_VERSION}.jar > /usr/local/tomcat/lib/ecs-logging-core-${ECS_LOGGING_VERSION}.jar \
    && curl -s https://repo1.maven.org/maven2/co/elastic/logging/jul-ecs-formatter/${ECS_LOGGING_VERSION}/jul-ecs-formatter-${ECS_LOGGING_VERSION}.jar > /usr/local/tomcat/lib/jul-ecs-formatter-${ECS_LOGGING_VERSION}.jar
COPY conf/setenv.sh /usr/local/tomcat/bin
COPY conf/logging.properties /usr/local/tomcat/conf

## install sr-manager
COPY --from=build /usr/src/app/target/sr-manager*.war /usr/local/tomcat/webapps/sr-manager.war
COPY src/main/webapp/WEB-INF/app.conf /etc/standard-reports/app.conf
COPY ./scripts /usr/local/bin

RUN adduser -u 1012 app \
  && chown -R app /usr/local/tomcat /etc/standard-reports
USER app

ARG image_name
ARG git_branch
ARG git_commit_hash
ARG github_run_number
ARG version

LABEL com.epimorphics.name=$image_name \
      com.epimorphics.branch=$git_branch \
      com.epimorphics.build=$github_run_number \
      com.epimorphics.commit=$git_commit_hash \
      com.epimorphics.version=$version
