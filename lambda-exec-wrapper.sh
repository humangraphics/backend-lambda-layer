#!/bin/bash

# Example Java17 CLI a/o Oct 2023:
# /var/lang/bin/java
#   -XX:MaxHeapSize=3945472k
#   -javaagent:/var/runtime/lib/Log4jHotPatch.jar=log4jFixerVerbose=false
#   -XX:+UseSerialGC
#   -Xshare:on
#   -XX:SharedArchiveFile=/var/lang/lib/server/runtime.jsa
#   -XX:+TieredCompilation
#   -XX:TieredStopAtLevel=1
#   --add-opens=java.base/java.io=ALL-UNNAMED
#   -Dorg.crac.Core.Compat=com.amazonaws.services.lambda.crac
#   -Djava.net.preferIPv4Stack=true
#   -XX:+ErrorFileToStderr
#   -Dcom.amazonaws.services.lambda.runtime.api.client.runtimeapi.NativeClient.JNI=/var/runtime/lib/jni/libaws-lambda-jni.linux-x86_64.so
#   -classpath
#   /var/runtime/lib/aws-lambda-java-core-1.2.3.jar:/var/runtime/lib/aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:/var/runtime/lib/aws-lambda-java-serialization-1.1.2.jar
#   com.amazonaws.services.lambda.runtime.api.client.AWSLambda
#   com.sigpwned.lambdainternals.App::handleRequest 

# Unpack our tmpdump, if we have one
pushd /tmp
/var/lang/bin/java -classpath /var/runtime/lib/aws-lambda-java-core-1.2.3.jar:/var/runtime/lib/aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:/var/runtime/lib/aws-lambda-java-serialization-1.1.2.jar:/opt/humangraphics io.humangraphics.backend.lambda.TmpDump "s3://$HUMANGRAPHICS_BUCKET/tmpdump/$AWS_LAMBDA_FUNCTION_NAME.zip"
popd

# Grab our args
ARGS=("$@")

# Do a pass to rewrite some of the args
for (( i=0; i<${#ARGS[@]}; i++ ))
do
  if [ ${ARGS[$i]} = "-classpath" ]
  then
    # Add /var/task, /opt/humangraphics to the classpath so ServiceLoader works
    ARGS[$i+1]="${ARGS[$i+1]}:/var/task:/opt/humangraphics"
  elif [ ${ARGS[$i]} = "-XX:+TieredCompilation" ]
  then
    # We actually do NOT want tiered compilation, thank you
    ARGS[$i]="-XX:-TieredCompilation"
  elif [ ${ARGS[$i]} = "-XX:TieredStopAtLevel=1" ]
  then
    # We don't want this, so skip it.
    continue
  elif [ ${ARGS[$i]} = "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" ]
  then
    # We want our entry point, not theirs, thank you
    ARGS[$i]="io.humangraphics.backend.lambda.thirdparty.com.amazonaws.services.lambda.runtime.api.client.AWSLambda"
  else
    # Nothing special to do
    true
  fi
done

# start the runtime with the rewritten args
exec "${ARGS[@]}"

