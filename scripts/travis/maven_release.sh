#!/usr/bin/env bash
echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

# Run the release plugin - with "[skip ci]" in the release commit message
pushd jodconverter-core
mvn -B \
  "-Darguments=-DskipTests" \
  -Dmaven.javadoc.skip \
  release:clean release:prepare release:perform \
  -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
  -Dusername="${GIT_USERNAME}" \
  -Dpassword="${GIT_PASSWORD}"
popd

popd
set +vex
echo "=========================== Finishing Release Script =========================="

