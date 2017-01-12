#!/usr/bin/env bash

curl -O https://storage.googleapis.com/kubernetes-release/release/v1.4.6/bin/linux/amd64/kubectl
chmod +x kubectl

./kubectl config set-cluster livescore --insecure-skip-tls-verify=true --server=${KUBE_API}
./kubectl config set-credentials ${KUBE_USERNAME} --token ${KUBE_PASSWORD}
./kubectl config set-context livescore --cluster=livescore --user=${KUBE_USERNAME}
./kubectl config use-context livescore
./kubectl set image deployment/livescore-demo-vertx-amqp-bridge dave=scholzj/livescore-demo-vertx-amqp-bridge:${CIRCLE_SHA1}