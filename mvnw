#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

set -euo pipefail

if [ -z "${MAVEN_SKIP_RC-}" ] ; then

  if [ -f /usr/local/etc/mavenrc ] ; then
    . /usr/local/etc/mavenrc
  fi

  if [ -f "$HOME/.mavenrc" ] ; then
    . "$HOME/.mavenrc"
  fi

fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true ;;
  Darwin*) darwin=true ;;
esac

if [ -z "${JAVA_HOME-}" ] ; then
  JAVA_CMD="$(command -v java || true)"
  if [ -z "$JAVA_CMD" ] ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." 1>&2
    echo "Please set the JAVA_HOME variable in your environment to match the" 1>&2
    echo "location of your Java installation." 1>&2
    exit 1
  fi
else
  JAVA_CMD="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVA_CMD" ] ; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" 1>&2
    echo "Please set the JAVA_HOME variable in your environment to match the" 1>&2
    echo "location of your Java installation." 1>&2
    exit 1
  fi
fi

if [ -z "${MAVEN_PROJECTBASEDIR-}" ] ; then
  if [ -n "${M2_HOME-}" ] ; then
    MAVEN_PROJECTBASEDIR="$M2_HOME"
  else
    SAVED="$(pwd)"
    cd "$(dirname "$0")" >/dev/null
    MAVEN_PROJECTBASEDIR="$(pwd -P)"
    cd "$SAVED" >/dev/null
  fi
fi

if [ -z "${MAVEN_HOME-}" ] ; then
  MAVEN_HOME="$MAVEN_PROJECTBASEDIR"
fi

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if [ "$cygwin" = "true" ] || [ "$mingw" = "true" ] ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  [ -n "$MAVEN_PROJECTBASEDIR" ] && MAVEN_PROJECTBASEDIR="$(cygpath --unix "$MAVEN_PROJECTBASEDIR")"
  WRAPPER_JAR="$(cygpath --path --unix "$WRAPPER_JAR")"
fi

if [ ! -r "$WRAPPER_JAR" ]; then
  echo "Couldn't find $WRAPPER_JAR, downloading it ..."
  if [ -n "${MVNW_REPOURL-}" ]; then
    jarUrl="$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  else
    jarUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  fi
  wrapperJarDir="$(dirname "$WRAPPER_JAR")"
  mkdir -p "$wrapperJarDir"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$jarUrl" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget "${jarUrl}" -O "$WRAPPER_JAR"
  else
    echo "ERROR: Couldn't download Maven Wrapper jar. Neither curl nor wget available." 1>&2
    exit 1
  fi
fi

MAVEN_OPTS="${MAVEN_OPTS-}"

exec "$JAVA_CMD" \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  ${MAVEN_OPTS} \
  $WRAPPER_LAUNCHER \
  "$@"
