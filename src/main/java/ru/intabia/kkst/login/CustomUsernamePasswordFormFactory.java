package ru.intabia.kkst.login;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;

public class CustomUsernamePasswordFormFactory extends UsernamePasswordFormFactory {
  public static final String PROVIDER_ID = "custom-auth-username-password-form";
  private static final String DISPLAY_TYPE = "Custom Username Password Form";
  private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
      AuthenticationExecutionModel.Requirement.REQUIRED,
      AuthenticationExecutionModel.Requirement.ALTERNATIVE,
      AuthenticationExecutionModel.Requirement.DISABLED
  };
  public static final CustomUsernamePasswordForm SINGLETON = new CustomUsernamePasswordForm();

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

  @Override
  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return REQUIREMENT_CHOICES;
  }
}
