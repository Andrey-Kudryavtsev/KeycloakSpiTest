package ru.intabia.kkst.verificationcode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationCodeSecretData {
  private final String code;

  @JsonCreator
  public VerificationCodeSecretData(@JsonProperty("code") String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
