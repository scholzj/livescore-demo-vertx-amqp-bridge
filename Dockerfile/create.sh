#!/usr/bin/env bash

# Copy the binaries
cp -r -v ./target/vertx-amqp-bridge-1.0-SNAPSHOT/vertx-amqp-bridge-1.0-SNAPSHOT ./Dockerfile/vertx-amqp-bridge-1.0-SNAPSHOT

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker build -t scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1} ./Dockerfile/
docker tag scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1} docker.io/scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1}
docker push scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1}
docker tag scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1} docker.io/scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_BRANCH}
docker push scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_BRANCH}
docker tag scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1} docker.io/scholzj/livescore-demo-vertx-amqp-bridge:latest
docker push scholzj/livescore-demo-vertx-amqp-bridge:latest

# Delete the binaries
rm -rf ./Dockerfile/vertx-amqp-bridge-1.0-SNAPSHOT