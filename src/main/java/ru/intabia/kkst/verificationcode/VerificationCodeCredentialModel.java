package ru.intabia.kkst.verificationcode;

import java.io.IOException;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;

public class VerificationCodeCredentialModel extends CredentialModel {
  public static final String TYPE = "VERIFICATION_CODE";

  private final VerificationCodeSecretData verificationCodeSecretData;
  private final VerificationCodeCredentialData verificationCodeCredentialData;

  private VerificationCodeCredentialModel(VerificationCodeSecretData verificationCodeSecretData,
                                          VerificationCodeCredentialData verificationCodeCredentialData) {
    this.verificationCodeSecretData = verificationCodeSecretData;
    this.verificationCodeCredentialData = verificationCodeCredentialData;
  }

  public VerificationCodeSecretData getVerificationCodeSecretData() {
    return verificationCodeSecretData;
  }

  public VerificationCodeCredentialData getVerificationCodeCredentialData() {
    return verificationCodeCredentialData;
  }

  public static VerificationCodeCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
    try {
      VerificationCodeSecretData secretData =
          JsonSerialization.readValue(credentialModel.getSecretData(), VerificationCodeSecretData.class);
      VerificationCodeCredentialData credentialData =
          JsonSerialization.readValue(credentialModel.getSecretData(), VerificationCodeCredentialData.class);

      VerificationCodeCredentialModel model =
          new VerificationCodeCredentialModel(secretData, credentialData);
      model.setUserLabel(credentialModel.getUserLabel());
      model.setCreatedDate(credentialModel.getCreatedDate());
      model.setType(TYPE);
      model.setId(credentialModel.getId());
      model.setSecretData(credentialModel.getSecretData());
      model.setCredentialData(credentialModel.getCredentialData());

      return model;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
