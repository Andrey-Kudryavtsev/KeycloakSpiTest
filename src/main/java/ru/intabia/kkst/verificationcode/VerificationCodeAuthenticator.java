package ru.intabia.kkst.verificationcode;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class VerificationCodeAuthenticator implements Authenticator,
    CredentialValidator<VerificationCodeCredentialProvider> {
  private static final String TEST_CODE = "123123";

  @Override
  public VerificationCodeCredentialProvider getCredentialProvider(KeycloakSession session) {
    return (VerificationCodeCredentialProvider) session.getProvider(
        CredentialProvider.class, VerificationCodeCredentialProviderFactory.PROVIDER_ID);
  }

  @Override
  public boolean requiresUser() {
    return true;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true;//getCredentialProvider(session).isConfiguredFor(realm, user, getType(session)); TODO
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
//    user.addRequiredAction("VERIFICATION_CODE_CONFIG");
  }

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    // send otp code?
    Response challenge = context.form()
        .createForm("verification-code.ftl");
    context.challenge(challenge);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    boolean validated = validateAnswer(context);
    if (!validated) {
      Response challenge =  context.form()
          .setError("badCode")
          .createForm("verification-code.ftl");
      context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
      return;
    }
    context.success();
  }

  protected boolean validateAnswer(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    String secret = formData.getFirst("verification_code");
    // check otp code
    return TEST_CODE.equals(secret);
//    String credentialId = formData.getFirst("credentialId");
//    if (credentialId == null || credentialId.isEmpty()) {
//      credentialId = getCredentialProvider(context.getSession())
//          .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser()).getId();
//    }
//
//    UserCredentialModel
//        input = new UserCredentialModel(credentialId, getType(context.getSession()), secret);
//    return getCredentialProvider(context.getSession()).isValid(context.getRealm(), context.getUser(), input);
  }

  @Override
  public void close() {

  }
}
