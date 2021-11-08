# Tomcat configuration extension for structured logging
ROOT=/usr/local/tomcat/lib
VERSION=1.2.0
export CLASSPATH="$CLASSPATH:$ROOT/ecs-logging-core-$VERSION.jar:$ROOT/jul-ecs-formatter-$VERSION.jar"
