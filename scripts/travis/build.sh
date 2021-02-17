#!/usr/bin/env bash
echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# The tests don't work unless we have libreoffice installed and configured locally.
# Currently we try out the code in a T-Engine.
pushd jodconverter-core
mvn -B -V install -DskipTests
popd

popd
set +vex
echo "=========================== Finishing Build Script =========================="

