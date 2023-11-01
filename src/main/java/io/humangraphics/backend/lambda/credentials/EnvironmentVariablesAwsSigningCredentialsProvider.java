package io.humangraphics.backend.lambda.credentials;

import com.sigpwned.httpmodel.aws.AwsSigningCredentials;
import com.sigpwned.httpmodel.aws.AwsSigningCredentialsProvider;

/**
 * Fetches credentials from the endpoint in environment variable AWS_CONTAINER_CREDENTIALS_FULL_URI.
 * Assumes JSON response has structure {AccessKeyId, SecretKeyId, Token}, per existing code. Only
 * loads credentials once, and does not monitor token expiration.
 */
public class EnvironmentVariablesAwsSigningCredentialsProvider
    implements AwsSigningCredentialsProvider {
  private AwsSigningCredentials credentials;

  @Override
  public AwsSigningCredentials getCredentials() {
    if (credentials == null)
      credentials = fetchCredentials();
    return credentials;
  }

  private static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";

  private static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";

  private static final String AWS_SESSION_TOKEN = "AWS_SESSION_TOKEN";

  private AwsSigningCredentials fetchCredentials() {
    final String accessKeyId = System.getenv(AWS_ACCESS_KEY_ID);
    final String secretAccessKey = System.getenv(AWS_SECRET_ACCESS_KEY);
    final String sessionToken = System.getenv(AWS_SESSION_TOKEN);
    return accessKeyId != null && secretAccessKey != null
        ? new AwsSigningCredentials(accessKeyId, secretAccessKey, sessionToken)
        : null;
  }
}
