package ru.intabia.kkst.verificationcode;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

public class VerificationCodeCredentialProvider implements
    CredentialProvider<VerificationCodeCredentialModel>, CredentialInputValidator {

  protected KeycloakSession keycloakSession;

  public VerificationCodeCredentialProvider(KeycloakSession keycloakSession) {
    this.keycloakSession = keycloakSession;
  }

  private UserCredentialStore getCredentialStore() {
    return keycloakSession.userCredentialManager();
  }

  @Override
  public String getType() {
    return VerificationCodeCredentialModel.TYPE;
  }

  @Override
  public VerificationCodeCredentialModel getCredentialFromModel(CredentialModel model) {
    return VerificationCodeCredentialModel.createFromCredentialModel(model);
  }

  @Override
  public CredentialModel createCredential(RealmModel realm, UserModel user,
                                          VerificationCodeCredentialModel credentialModel) {
    if (credentialModel.getCreatedDate() == null) {
      credentialModel.setCreatedDate(Time.currentTimeMillis());
    }
    return getCredentialStore().createCredential(realm, user, credentialModel);
  }

  @Override
  public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
    return getCredentialStore().removeStoredCredential(realm, user, credentialId);
  }

  @Override
  public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
    if (!(credentialInput instanceof UserCredentialModel)) {
      System.out.println("Expected instance of UserCredentialModel for CredentialInput");
      return false;
    }
    if (!credentialInput.getType().equals(getType())) {
      return false;
    }
    String challengeResponse = credentialInput.getChallengeResponse();
    if (challengeResponse == null) {
      return false;
    }
    CredentialModel credentialModel =
        getCredentialStore().getStoredCredentialById(realm, user, credentialInput.getCredentialId());
    VerificationCodeCredentialModel model = getCredentialFromModel(credentialModel);
    return model.getVerificationCodeSecretData().getCode().equals(challengeResponse);
  }

  @Override
  public boolean supportsCredentialType(String credentialType) {
    return getType().equals(credentialType);
  }

  @Override
  public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
    if (!supportsCredentialType(credentialType)) return false;
    return getCredentialStore().getStoredCredentialsByTypeStream(realm, user, credentialType)
        .findAny().isPresent();
  }

  @Override
  public CredentialTypeMetadata getCredentialTypeMetadata(
      CredentialTypeMetadataContext metadataContext) {
    return CredentialTypeMetadata.builder()
        .type(getType())
        .category(CredentialTypeMetadata.Category.TWO_FACTOR)
        .displayName(VerificationCodeCredentialProviderFactory.PROVIDER_ID)
        .helpText("verification-code-text")
        .createAction(VerificationCodeAuthenticatorFactory.PROVIDER_ID)
        .removeable(false)
        .build(keycloakSession);
  }
}
