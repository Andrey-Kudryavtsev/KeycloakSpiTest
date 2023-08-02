package ru.intabia.kkst.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import ru.intabia.kkst.AuthSource;
import ru.intabia.kkst.gatekeeper.Frame;
import ru.intabia.kkst.gatekeeper.GatekeeperClient;
import ru.intabia.kkst.gatekeeper.GatekeeperClientImpl;

public class CustomUsernamePasswordForm extends UsernamePasswordForm {

  private static final List<String> SUPPORTED_PLATFORMS = Arrays.asList("ANDROID", "IOS");
  private static final GatekeeperClient client = new GatekeeperClientImpl();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    Response challenge = context.form().createForm("custom-login.ftl");
    context.challenge(challenge);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    if (formData.containsKey("cancel")) {
      context.cancelLogin();
      return;
    }
    String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);
    String password = formData.getFirst(CredentialRepresentation.PASSWORD);
    String phone = formData.getFirst("phone");
    if (!phone.isEmpty()) {
      validatePhone(context, phone);
    } else if (!username.isEmpty() && !password.isEmpty()) {
      validateUsernamePassword(context, formData);
    } else {
      noCredentialsError(context);
    }
  }

  private void validateUsernamePassword(AuthenticationFlowContext context,
                                        MultivaluedMap<String, String> formData) {
    String platformUserLogin = formData.getFirst(AuthenticationManager.FORM_USERNAME);
    context.getAuthenticationSession().setAuthNote("platformUserLogin", platformUserLogin);

    Frame response = client.getUserById(platformUserLogin);

    JsonNode body =
        response.getBody() != null ? response.getBody() : objectMapper.createObjectNode();

    String adLogin = body.path("adlogin").asText();

    formData.putSingle(AuthenticationManager.FORM_USERNAME, adLogin);
    if (!validateForm(context, formData)) {
      return;
    }

    String clientId = body.path("clientID").asText();
    context.getAuthenticationSession().setUserSessionNote("clientID", clientId);

    String phoneNumber = body.path("phoneForRecovery").asText();
    context.getAuthenticationSession().setAuthNote("phoneForRecovery", phoneNumber);

    Stream<JsonNode> devicesStream = StreamSupport.stream(
        body.path("tokens").spliterator(), false);
    List<JsonNode> devicesList = devicesStream.filter(
            token -> SUPPORTED_PLATFORMS.contains(token.path("deviceType").asText()))
        .map(token -> token.path("deviceToken"))
        .collect(Collectors.toList());
    String devices = devicesList.toString();
    context.getAuthenticationSession().setAuthNote("devices", devices);

    context.getAuthenticationSession()
        .setUserSessionNote("authSource", AuthSource.LOGIN.getValue());

    context.success();
  }

  private void validatePhone(AuthenticationFlowContext context,
                             String phone) {
    Response challengeResponse = challenge(context, "notImplemented", "phone");
    context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);

//    context.clearUser();
//    UserModel user = context.getSession().users()
//        .searchForUserByUserAttributeStream(context.getRealm(), "phone", phone)
//        .findFirst()
//        .orElse(null);
//    if (user == null) {
//      context.getEvent().error(Errors.USER_NOT_FOUND);
//      Response challengeResponse = challenge(context, "invalidPhoneMessage", "phone");
//      context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
//    } else {
//      context.setUser(user);
//      context.getAuthenticationSession()
//          .setUserSessionNote("authSource", AuthSource.PHONE.getValue());
//      context.success();
//    }
  }

  private void noCredentialsError(AuthenticationFlowContext context) {
    context.getEvent().error(Errors.USER_NOT_FOUND);
    Response challengeResponse = challenge(context, "missingCredentials", null);

    context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
  }

  @Override
  protected Response challenge(AuthenticationFlowContext context, String error, String field) {
    LoginFormsProvider form = context.form()
        .setExecution(context.getExecution().getId());
    if (error != null) {
      if (field != null) {
        form.addError(new FormMessage(field, error));
      } else {
        form.setError(error);
      }
    }
    return form.createForm("custom-login.ftl");
  }
}
