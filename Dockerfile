# Build
FROM maven:3.6-jdk-8 AS build  
COPY src /usr/src/app/src  
COPY pom.xml /usr/src/app  
RUN mvn -f /usr/src/app/pom.xml clean package

# Package - using tomcat7 for now but should be compatible with tomcat 8
FROM tomcat:7.0-jdk8-corretto
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

RUN adduser -u 1012 app \
  && chown -R app /usr/local/tomcat /etc/standard-reports
USER app
