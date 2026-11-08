#!/usr/bin/env bash
# Local development build helper for this workspace.
# Runs Gradle with Java 21 (Gradle 8.5) and uses Java 17 as the project toolchain.
set -uo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR" || exit 1

export JAVA_HOME="/c/Users/neolx/.jdks/zulu21.52.203-ca-jdk21.0.12.1-win_x64"
export PATH="$JAVA_HOME/bin:$PROJECT_DIR/.work/gradle-8.5/bin:$PATH"
export GRADLE_USER_HOME="$PROJECT_DIR/.work/gradle-home"

gradle -Djava.io.tmpdir='D:\我的小项目\ECO-Calcer\.work\tmp' \
    "$@" --no-daemon --console=plain
