#!/usr/bin/env bash
set -ex
cd "$(dirname "${BASH_SOURCE[0]}" )"/..

VERSION=$(git describe --tags --always --first-parent)
DEST=build/docker

rm -rf $DEST
mkdir -p $DEST/portfolio-service-server/var/conf
mkdir -p $DEST/portfolio-service-server/var/certs

cp ./scripts/Dockerfile $DEST/
tar -xf "./portfolio-service-server/build/distributions/portfolio-service-server-${VERSION}.tar" -C $DEST/portfolio-service-server --strip-components=1
cp ./portfolio-service-server/var/conf/conf.yml $DEST/portfolio-service-server/var/conf
cp ./portfolio-service-server/var/certs/* $DEST/portfolio-service-server/var/certs

cd $DEST
docker build -t "chomutovskij/portfolio-service-server:$VERSION" .
docker tag "chomutovskij/portfolio-service-server:$VERSION" "chomutovskij/portfolio-service-server:latest"
