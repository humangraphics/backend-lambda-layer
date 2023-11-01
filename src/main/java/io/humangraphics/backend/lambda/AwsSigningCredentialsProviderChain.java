package io.humangraphics.backend.lambda;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import java.util.List;
import com.sigpwned.httpmodel.aws.AwsSigningCredentials;
import com.sigpwned.httpmodel.aws.AwsSigningCredentialsProvider;
import io.humangraphics.backend.lambda.credentials.ContainerAwsSigningCredentialsProvider;
import io.humangraphics.backend.lambda.credentials.EnvironmentVariablesAwsSigningCredentialsProvider;
import io.humangraphics.backend.lambda.util.LambdaLayer;

public class AwsSigningCredentialsProviderChain implements AwsSigningCredentialsProvider {
  private static final boolean DEBUG = LambdaLayer.DEBUG;

  private final List<AwsSigningCredentialsProvider> credentialProviders;

  public AwsSigningCredentialsProviderChain() {
    this(asList(new ContainerAwsSigningCredentialsProvider(),
        new EnvironmentVariablesAwsSigningCredentialsProvider()));
  }

  public AwsSigningCredentialsProviderChain(
      List<AwsSigningCredentialsProvider> credentialProviders) {
    this.credentialProviders = unmodifiableList(credentialProviders);
  }

  @Override
  public AwsSigningCredentials getCredentials() {
    AwsSigningCredentials result = null;
    for (AwsSigningCredentialsProvider credentialProvider : getCredentialProviders()) {
      result = credentialProvider.getCredentials();
      if (result != null) {
        if (DEBUG) {
          System.err.println("Found credentials " + result + " from " + credentialProvider);
        }
        break;
      }
    }
    return result;
  }

  private List<AwsSigningCredentialsProvider> getCredentialProviders() {
    return credentialProviders;
  }
}
