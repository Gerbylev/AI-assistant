#!/bin/bash
set -e


echo Build.SH: $1
ROOT=$(realpath $(dirname $0))
BRANCH=$(git rev-parse --abbrev-ref HEAD)
BUILD_DATE=$(date +"%Y-%m-%dT%T")
BUILD_COMMIT_ID=$(git rev-parse HEAD)


echo Builing docker image ...
LABEL=`echo "$BRANCH" | tr '[:upper:]' '[:lower:]'`
echo $LABEL

mvn clean package

docker build . -f Dockerfile -t cc-back:$LABEL

docker tag cc-back:$LABEL registry.gitlab.com/cognitive-companion/cc-back:$LABEL

docker push registry.gitlab.com/cognitive-companion/cc-back:$LABEL


