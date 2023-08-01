package ru.intabia.kkst.verificationcode;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

public class VerificationCodeCredentialProviderFactory implements
    CredentialProviderFactory<VerificationCodeCredentialProvider> {

  public static final String PROVIDER_ID = "verification-code";

  @Override
  public CredentialProvider create(KeycloakSession session) {
    return new VerificationCodeCredentialProvider(session);
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
