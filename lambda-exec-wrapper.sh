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

# Example Java17 CLI a/o Oct 2024:
# /var/lang/bin/java
#   -XX:MaxHeapSize=445645k
#   -javaagent:/var/runtime/lib/Log4jHotPatch.jar=log4jFixerVerbose=false
#   -XX:+UseSerialGC
#   -Xshare:on
#   -XX:SharedArchiveFile=/var/lang/lib/server/runtime.jsa
#   -XX:+TieredCompilation
#   -XX:TieredStopAtLevel=1
#   --add-opens=java.base/java.io=ALL-UNNAMED
#   -Dorg.crac.Core.Compat=com.amazonaws.services.lambda.crac
#   -XX:+ErrorFileToStderr
#   -Dcom.amazonaws.services.lambda.runtime.api.client.runtimeapi.NativeClient.JNI=/var/runtime/lib/jni/libaws-lambda-jni.linux-x86_64.so
#   -classpath
#   /var/runtime/lib/aws-lambda-java-core-1.2.3.jar:/var/runtime/lib/aws-lambda-java-runtime-interface-client-2.5.1-linux-x86_64.jar:/var/runtime/lib/aws-lambda-java-serialization-1.1.5.jar
#   com.amazonaws.services.lambda.runtime.api.client.AWSLambda
#   com.sigpwned.lambdainternals.App::handleRequest

# To check or update the above command line, run the lambdainternals Lambda
# Function in AWS account product-humangraphics-prod (382363607278). That
# implementation comes from the following GitHub repo:
# https://github.com/sigpwned/the-lambda-iceberg-dissecting-the-official-java-runtimes  

# Dump our env
echo "ENVIRONMENT:"
env
echo

# List our runtime libs
echo "RUNTIME LIBS:"
ls -l /var/runtime/lib
echo

# Get our runtime version
echo "RUNTIME VERSION:"
cat /var/runtime/runtime-release
echo

# Detect key library versions
pushd /var/runtime/lib
AWS_LAMBDA_JAVA_CORE_JAR=`ls aws-lambda-java-core-*.jar`
AWS_LAMBDA_JAVA_RUNTIME_INTERFACE_CLIENT_JAR=`ls aws-lambda-java-runtime-interface-client-*.jar`
AWS_LAMBDA_JAVA_SERIALIZATION_JAR=`ls aws-lambda-java-serialization-*.jar`
popd

# Unpack our tmpdump, if we have one
# Remember, /opt/humangraphics is where this custom layer ("humangraphics") will
# be unpacked.
pushd /tmp
/var/lang/bin/java -classpath /var/runtime/lib/$AWS_LAMBDA_JAVA_CORE_JAR:/var/runtime/lib/$AWS_LAMBDA_JAVA_RUNTIME_INTERFACE_CLIENT_JAR:/var/runtime/lib/$AWS_LAMBDA_JAVA_SERIALIZATION_JAR.jar:/opt/humangraphics io.humangraphics.backend.lambda.TmpDump "s3://$HUMANGRAPHICS_BUCKET/tmpdump/$AWS_LAMBDA_FUNCTION_NAME.zip"
popd

# Grab our args
ARGS=("$@")

# Do a pass to rewrite some of the args
for (( i=0; i<${#ARGS[@]}; i++ ))
do
  if [ ${ARGS[$i]} = "-classpath" ]
  then
    # Add /var/task, /opt/humangraphics to the classpath so ServiceLoader works
    #
    # Reading the original AWSLambda suggests that we should add /opt/java too.
    # Consulting CustomerClassLoader indicates we ACTUALLY need to add all JARs
    # in /var/task/lib and /opt/java/lib.
    #
    # Checking the actual lambda container reveals there are no such JARs. So,
    # the below will do.
    ARGS[$i+1]="${ARGS[$i+1]}:/var/task:/opt/humangraphics"
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

