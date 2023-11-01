package io.humangraphics.backend.lambda;

import static java.lang.String.format;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.json.JSONObject;
import com.sigpwned.httpmodel.aws.AwsSigningCredentials;
import com.sigpwned.httpmodel.aws.AwsSigningCredentialsProvider;
import io.humangraphics.backend.lambda.util.ByteStreams;

/**
 * Fetches credentials from the endpoint in environment variable AWS_CONTAINER_CREDENTIALS_FULL_URI.
 * Assumes JSON response has structure {AccessKeyId, SecretKeyId, Token}, per existing code. Only
 * loads credentials once, and does not monitor token expiration.
 */
public class ContainerAwsSigningCredentialsProvider implements AwsSigningCredentialsProvider {
  private AwsSigningCredentials credentials;

  @Override
  public AwsSigningCredentials getCredentials() {
    if (credentials == null)
      credentials = fetchCredentials();
    return credentials;
  }

  private static final String AWS_CONTAINER_CREDENTIALS_FULL_URI =
      Optional.ofNullable(System.getenv("AWS_CONTAINER_CREDENTIALS_FULL_URI"))
          .orElseThrow(() -> new AssertionError(
              "No value for environment variable AWS_CONTAINER_CREDENTIALS_FULL_URI"));

  private static final String AWS_CONTAINER_AUTHORIZATION_TOKEN =
      Optional.ofNullable(System.getenv("AWS_CONTAINER_AUTHORIZATION_TOKEN"))
          .orElseThrow(() -> new AssertionError(
              "No value for environment variable AWS_CONTAINER_AUTHORIZATION_TOKEN"));

  /**
   * https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/auth/BaseCredentialsFetcher.java#L55C1-L62C1
   * https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/internal/EC2ResourceFetcher.java#L38
   * https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/internal/ConnectionUtils.java#L78
   * https://github.com/aws/aws-lambda-runtime-interface-emulator/blob/bf7e2486034742b84a1a25d28478e147b5e65f06/lambda/rapi/handler/credentials.go#L20
   */
  private AwsSigningCredentials fetchCredentials() {
    JSONObject response;

    try {
      HttpURLConnection cn =
          (HttpURLConnection) new URL(AWS_CONTAINER_CREDENTIALS_FULL_URI).openConnection();
      try {
        cn.setRequestProperty("authorization", AWS_CONTAINER_AUTHORIZATION_TOKEN);
        if (cn.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String responseBody =
              new String(ByteStreams.toByteArray(cn.getInputStream()), StandardCharsets.UTF_8);
          System.out.println("Received response: " + responseBody);
          response = new JSONObject(responseBody);
        } else {
          String errorBody =
              new String(ByteStreams.toByteArray(cn.getErrorStream()), StandardCharsets.UTF_8);
          throw new IOException(
              format("Request failed; status=%d; body=%s", cn.getResponseCode(), errorBody));
        }
      } finally {
        cn.disconnect();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to retrieve container credentials", e);
    }

    String accessKeyId = response.getString("AccessKeyId");
    String secretAccessKey = response.getString("SecretAccessKey");
    String sessionToken = response.getString("Token");

    return AwsSigningCredentials.of(accessKeyId, secretAccessKey, sessionToken);
  }
}
