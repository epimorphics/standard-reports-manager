# Build
FROM maven:3.6-jdk-8 AS build  
COPY src /usr/src/app/src  
COPY pom.xml /usr/src/app  
RUN mvn -f /usr/src/app/pom.xml clean package

# Package - sing tomcat7 for now but should be compatible with tomcat 8
FROM tomcat:7.0-jdk8-corretto
RUN yum -y install shadow-utils

COPY --from=build /usr/src/app/target/sr-manager*.war /usr/local/tomcat/webapps/sr-manager.war
COPY src/main/webapp/WEB-INF/app.conf /etc/standard-reports/app.conf

RUN adduser -u 1012 app \
  && chown -R app /usr/local/tomcat /etc/standard-reports
USER app
