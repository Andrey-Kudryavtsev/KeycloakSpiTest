package ru.intabia.kkst.verificationcode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationCodeCredentialData {
  private final String cred;

  @JsonCreator
  public VerificationCodeCredentialData(@JsonProperty("cred") String cred) {
    this.cred = cred;
  }

  public String getCred() {
    return cred;
  }
}
