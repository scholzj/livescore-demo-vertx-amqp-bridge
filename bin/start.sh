#!/bin/bash

WHEREAMI=`dirname "${0}"`
if [ -z "${SERVICE_ROOT}" ]; then
    export SERVICE_ROOT=`cd "${WHEREAMI}/../" && pwd`
fi

SERVICE_LIB=${SERVICE_ROOT}/lib
SERVICE_ETC=${SERVICE_ROOT}/etc
export SERVICE_LOG=${SERVICE_ROOT}/log

java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
     -Dlogback.configurationFile=${SERVICE_ETC}/logback.xml \
     -jar ${SERVICE_LIB}/livescore-demo-vertx-amqp-bridge-1.0-SNAPSHOT-fat.jar -conf ${SERVICE_ETC}/config.json