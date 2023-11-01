package io.humangraphics.backend.lambda;

import static java.lang.String.format;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import com.sigpwned.httpmodel.aws.AwsSigningCredentialsProvider;
import com.sigpwned.httpmodel.aws.AwsSigningModelHttpClient;
import com.sigpwned.httpmodel.aws.signer.SigV4AwsSigner;
import com.sigpwned.httpmodel.core.ModelHttpClient;
import com.sigpwned.httpmodel.core.client.UrlConnectionModelHttpClient;
import com.sigpwned.httpmodel.core.model.ModelHttpEntity;
import com.sigpwned.httpmodel.core.model.ModelHttpHeaders;
import com.sigpwned.httpmodel.core.model.ModelHttpRequest;
import com.sigpwned.httpmodel.core.model.ModelHttpResponse;
import com.sigpwned.httpmodel.core.model.ModelHttpUrl;
import io.humangraphics.backend.lambda.util.ByteStreams;
import io.humangraphics.backend.lambda.util.LambdaLayer;

/**
 * Download a ZIP file from S3 and unzip it to the current directory
 */
public class TmpDump {
  private static boolean DEBUG = LambdaLayer.DEBUG;

  public static final String AWS_REGION_ENV_NAME = "AWS_REGION";

  private static String getenv(String name) {
    return Optional.ofNullable(System.getenv(name)).orElseThrow(
        () -> new IllegalArgumentException("No value for environment variable " + name));
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1 || args[0].equals("-help")) {
      System.err.println("Syntax: java io.humangraphics.backend.lambda.TmpDump <s3uri>");
      System.exit(1);
    }

    String uri = args[0];

    ModelHttpEntity entity = s3get(uri).orElse(null);

    if (entity != null) {
      try (SeekableByteChannel channel = new SeekableInMemoryByteChannel(entity.toByteArray());
          ZipFile zip = new ZipFile(channel)) {
        unzip(zip);
      }
    }
  }

  public static final String S3_SERVICE_NAME = "s3";

  private static final Pattern S3_URI_PATTERN = Pattern.compile("s3://([-a-z0-9]+)(/.*)");

  public static Optional<ModelHttpEntity> s3get(String uri) throws IOException {
    Matcher s3UriMatcher = S3_URI_PATTERN.matcher(uri);
    if (!s3UriMatcher.matches())
      throw new IllegalArgumentException("Invalid S3 uri");
    final String bucketName = s3UriMatcher.group(1).toLowerCase();
    final String path = s3UriMatcher.group(2);

    final String region = getenv(AWS_REGION_ENV_NAME);

    if (DEBUG) {
      System.err.println("Found bucketName " + bucketName);
      System.err.println("Found path " + path);
      System.err.println("Found region " + region);
    }

    return s3get(new AwsSigningCredentialsProviderChain(), uri, bucketName, path, region);
  }

  /* default */ static Optional<ModelHttpEntity> s3get(
      AwsSigningCredentialsProvider credentialsProvider, String uri, String bucketName, String path,
      String region) throws IOException {
    /**
     * https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-bucket-intro.html
     */
    final String url =
        format("https://%s.%s.amazonaws.com/%s%s", S3_SERVICE_NAME, region, bucketName, path);

    if (DEBUG) {
      System.err.println("Computed url " + url);
    }

    try (ModelHttpClient client = new AwsSigningModelHttpClient(
        new SigV4AwsSigner(credentialsProvider), new UrlConnectionModelHttpClient())) {
      ModelHttpResponse response = client.send(ModelHttpRequest.builder().version("1.1")
          .method("GET").headers(ModelHttpHeaders.of()).url(ModelHttpUrl.fromString(url)).build());
      if (DEBUG) {
        System.err.println("Got status code " + response.getStatusCode());
      }
      if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
        if (DEBUG) {
          System.err.println(
              response.getEntity().map(e -> e.toString(StandardCharsets.UTF_8)).orElse(""));
        }
        throw new IOException(format("S3 Get Failure %s (%d)", url, response.getStatusCode()));
      }
      return response.getEntity();
    }
  }

  public static void unzip(ZipFile zip) throws IOException {
    Enumeration<ZipArchiveEntry> entries = zip.getEntries();
    while (entries.hasMoreElements()) {
      ZipArchiveEntry entry = entries.nextElement();
      Path file = Paths.get(entry.getName());
      if (entry.isDirectory()) {
        Files.createDirectories(file);
      } else {
        if (file.getParent() != null && Files.notExists(file.getParent()))
          Files.createDirectories(file.getParent());
        if (entry.isUnixSymlink()) {
          Path target;
          try (InputStream input = zip.getInputStream(entry)) {
            target = Paths.get(new String(ByteStreams.toByteArray(input), StandardCharsets.UTF_8));
          }
          if (DEBUG) {
            System.err.println("Writing zip symlink " + file + " -> " + target);
          }
          Files.createSymbolicLink(file, target);
        } else {
          try (OutputStream output = Files.newOutputStream(file, StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING)) {
            try (InputStream input = zip.getInputStream(entry)) {
              ByteStreams.copyTo(input, output);
            }
            if (DEBUG) {
              System.err.println("Writing zip file " + file);
            }
          }
        }
      }
    }
  }
}
