# backend-lambda-layer

This backend layer makes a few important preparations to the lambda runtime during initialization:

1. Use a custom `AWSLambda` implementation to load the application in the system classloader. By default, the application is loaded in a separate classloader. This is important because native libraries are only allowed to be loaded from one classloader in an application, and splitting classloaders causes problems with JNI loading.
2. Modify the classpath to include the `/var/task` folder, which is where the application is unzipped. This is important because services are only allowed to load from the classpath.
3. Load so-called "tmpdump" archives from S3. This allows applications to load arbitrary application data into the /tmp folder during initialization, which is used for native libraries in practice. This is important because native libraries for ML are frequently large, but the application can only be up to 256MB unzipped.
4. Modify the library loading paths to include `/tmp/lib`. Both `java.library.path` and `LD_LIBRARY_PATH` are customized. This is important because native libraries should be loadable from tmpdumps.

Again, tmpdumps are typically used to house (large) native libraries. To create a tmpdump using javacpp libraries, perform the following steps:

1. Enable debug printin in javacpp using `-Dorg.bytedeco.javacpp.logger.debug=true` and run the application, being sure to exercise all code paths that load libraries.
2. Inspect the debug logs to find the libraries which are loaded and essential to the application. This command can be useful to extract loaded libraries: `cat debug.log | grep 'Loading' | grep '[.]so' | less | awk '{print $3}' | sed -e 's!^.*[.]jar/!/!;'`. This command can be useful to extract loaded classes: `cat debug.log | grep 'Loading class' | awk '{print $4}' | sort | uniq -c | sort -nr`.
3. Add the cache javacpp mojo to POM, caching the essential libraries. It may be prudent to add this to a profile.
4. Run the build using `mvn -Dorg.bytedeco.javacpp.cachedir=target/javacpp/lib -Dorg.bytedeco.javacpp.cachedir.nosubdir=true -Djavacpp.platform=linux-x86_64 clean compile install`.
5. Add the cached libraries from the x86_64 platform to a ZIP file at "lib/library.so". To save space, use symlinks for library versions, using the `zip -y` flag.
6. Upload to S3 at `$BUCKET/tmpdump/$LAMBDA_NAME.zip`.

Example instructions for refining a tmpdump:

    cd target/javacpp
    rm -f ../../tmpdump.zip 
    zip -y -r ../../tmpdump.zip lib/
    cd ../..
    zip -d tmpdump.zip 'lib/.lock'
    zip -d tmpdump.zip 'lib/*openblas_nolapack*'
 
TODO: Figure out how to load AVX2 platform libs.
