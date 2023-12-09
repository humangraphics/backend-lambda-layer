package io.humangraphics.backend.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import com.sigpwned.aws.sdk.lite.core.auth.AwsCredentialsProvider;
import com.sigpwned.aws.sdk.lite.core.auth.credentials.provider.chain.DefaultAwsCredentialsProviderChain;
import com.sigpwned.aws.sdk.lite.s3.S3Client;
import com.sigpwned.aws.sdk.lite.s3.exception.AccessDeniedException;
import com.sigpwned.aws.sdk.lite.s3.exception.NoSuchBucketException;
import com.sigpwned.aws.sdk.lite.s3.exception.NoSuchKeyException;
import com.sigpwned.aws.sdk.lite.s3.model.GetObjectRequest;
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

    byte[] entity = s3get(uri).orElse(null);

    if (entity != null) {
      try (SeekableByteChannel channel = new SeekableInMemoryByteChannel(entity);
          ZipFile zip = new ZipFile(channel)) {
        unzip(zip);
      }
    }
  }

  public static final String S3_SERVICE_NAME = "s3";

  private static final Pattern S3_URI_PATTERN = Pattern.compile("^s3://([-a-z0-9]+)/(.*)$");

  public static Optional<byte[]> s3get(String uri) throws IOException {
    Matcher s3UriMatcher = S3_URI_PATTERN.matcher(uri);
    if (!s3UriMatcher.matches())
      throw new IllegalArgumentException("Invalid S3 uri");
    final String bucketName = s3UriMatcher.group(1).toLowerCase();
    final String key = s3UriMatcher.group(2);

    final String region = getenv(AWS_REGION_ENV_NAME);

    if (DEBUG) {
      System.err.println("Found bucketName " + bucketName);
      System.err.println("Found path " + key);
      System.err.println("Found region " + region);
    }

    return s3get(new DefaultAwsCredentialsProviderChain(), bucketName, key, region);
  }

  /* default */ static Optional<byte[]> s3get(AwsCredentialsProvider credentialsProvider,
      String bucketName, String key, String region) throws IOException {
    try (S3Client client =
        S3Client.builder().credentialsProvider(credentialsProvider).region(region).build()) {
      try (InputStream in =
          client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())) {
        if (DEBUG) {
          System.err.println("Found object: s3://" + bucketName + "/" + key);
        }
        return Optional.of(ByteStreams.toByteArray(in));
      } catch (NoSuchKeyException e) {
        if (DEBUG) {
          System.err.println("No such key: " + key);
          e.printStackTrace(System.err);
        }
        return Optional.empty();
      } catch (NoSuchBucketException e) {
        if (DEBUG) {
          System.err.println("No such bucket: " + bucketName);
          e.printStackTrace(System.err);
        }
        return Optional.empty();
      } catch (AccessDeniedException e) {
        if (DEBUG) {
          System.err.println("Access denied to object: s3://" + bucketName + "/" + key);
          e.printStackTrace(System.err);
        }
        return Optional.empty();
      } catch (RuntimeException e) {
        throw e;
      } catch (IOException e) {
        throw e;
      } catch (Exception e) {
        throw new IOException("Failed to download given object: s3://" + bucketName + "/" + key, e);
      }
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
