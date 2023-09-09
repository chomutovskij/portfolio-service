#!/usr/bin/env bash
set -ex
cd "$(dirname "${BASH_SOURCE[0]}" )"/..

VERSION=$(git describe --tags --always --first-parent)
DEST=build/docker

rm -rf $DEST
mkdir -p $DEST/portfolio-service-server/var/conf

cp ./scripts/Dockerfile $DEST/
tar -xf "./portfolio-service-server/build/distributions/portfolio-service-server-${VERSION}.tar" -C $DEST/portfolio-service-server --strip-components=1
cp ./portfolio-service-server/var/conf/conf.yml $DEST/portfolio-service-server/var/conf

cd $DEST
docker build -t "palantirtechnologies/portfolio-service-server:$VERSION" .
docker tag "palantirtechnologies/portfolio-service-server:$VERSION" "palantirtechnologies/portfolio-service-server:latest"

