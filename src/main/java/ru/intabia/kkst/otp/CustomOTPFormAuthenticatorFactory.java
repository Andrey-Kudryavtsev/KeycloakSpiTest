package ru.intabia.kkst.otp;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

public class CustomOTPFormAuthenticatorFactory extends OTPFormAuthenticatorFactory {
  public static final String PROVIDER_ID = "custom-auth-otp-form";
  private static final String DISPLAY_TYPE = "Custom OTP Form";
  public static final CustomOTPFormAuthenticator SINGLETON = new CustomOTPFormAuthenticator();

  @Override
  public Authenticator create(KeycloakSession session) {
    return SINGLETON;
  }

  @Override
  public String getDisplayType() {
    return DISPLAY_TYPE;
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
