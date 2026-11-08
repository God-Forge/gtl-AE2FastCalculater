#!/usr/bin/env bash
# Sync sources to an ASCII-only build directory and run a Gradle build there.
#
# Why: this sandbox's JVM misreports Files.isWritable() for the CJK project path, which
# makes ForgeGradle's access-transformer zip handling read-only, and Gradle's worker
# argument files get mangled. Building from a pure-ASCII path avoids both problems.
set -uo pipefail

SRC="/d/我的小项目/ECO-Calcer"
DST="/c/Users/neolx/ecocalcer-build"

mkdir -p "$DST"
rm -rf "$DST/src"
cp -r "$SRC/src" "$DST/src"
cp "$SRC/build.gradle" "$SRC/settings.gradle" "$SRC/gradle.properties" "$DST/"
# Local dependency mirrors must match the source tree too.
rm -rf "$DST/libs" "$DST/maven-repo"
cp -r "$SRC/libs" "$SRC/maven-repo" "$DST/"

export JAVA_HOME="/c/Users/neolx/.jdks/zulu21.52.203-ca-jdk21.0.12.1-win_x64"
export PATH="$JAVA_HOME/bin:/c/Users/neolx/gradle-8.5/bin:$PATH"
export GRADLE_USER_HOME="/c/Users/neolx/gh-eco"

cd "$DST"
gradle -Djava.io.tmpdir='C:\Users\neolx\ecocalcer-build\.tmp' "$@" --no-daemon --console=plain
