package io.humangraphics.backend.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Optional;
import org.json.JSONObject;
import com.sigpwned.httpmodel.aws.AwsSigningCredentials;
import com.sigpwned.httpmodel.aws.AwsSigningCredentialsProvider;

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

  /**
   * https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/auth/BaseCredentialsFetcher.java#L55C1-L62C1
   */
  private AwsSigningCredentials fetchCredentials() {
    JSONObject response;
    try (InputStream in = new URL(AWS_CONTAINER_CREDENTIALS_FULL_URI).openStream()) {
      response = new JSONObject(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to retrieve credentials", e);
    }

    String accessKeyId = response.getString("AccessKeyId");
    String secretAccessKey = response.getString("SecretAccessKey");
    String sessionToken = response.getString("Token");

    return AwsSigningCredentials.of(accessKeyId, secretAccessKey, sessionToken);
  }
}
