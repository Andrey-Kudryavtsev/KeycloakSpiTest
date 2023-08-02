package ru.intabia.kkst.resetpassword;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialChooseUser;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import ru.intabia.kkst.gatekeeper.Frame;
import ru.intabia.kkst.gatekeeper.GatekeeperClient;
import ru.intabia.kkst.gatekeeper.GatekeeperClientImpl;
import ru.intabia.kkst.gatekeeper.GatekeeperClientMock;

public class ResetCredentialChooseUserByPassport extends ResetCredentialChooseUser {

  private static final List<String> SUPPORTED_PLATFORMS = Arrays.asList("ANDROID", "IOS");
  private static final String PROVIDER_ID = "reset-creds-choose-user-by-passport";
  private static final String DISPLAY_TYPE = "Choose user by passport";
  private static final String AUTH_SOURCE_PASSPORT = "2";
  private static final GatekeeperClient client = new GatekeeperClientImpl();

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    Response challenge = context.form().createForm("custom-login-reset-password.ftl");
    context.challenge(challenge);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    EventBuilder event = context.getEvent();
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

    String seria = formData.getFirst("seria");
    String number = formData.getFirst("number");
    boolean consent = true; // TODO: where to get it?
    if (seria == null || seria.isEmpty() || number == null || number.isEmpty()) {
      event.error(Errors.USERNAME_MISSING);
      Response challenge = context.form()
          .addError(new FormMessage("number", "seriaAndNumberMissing"))
          .createForm("custom-login-reset-password.ftl");
      context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
      return;
    }


    Frame response;
    if ("mock".equals(seria) && "mock".equals(number)) {
      GatekeeperClient mockClient = new GatekeeperClientMock();
      response = mockClient.getUserByPassport(seria, number, consent);
    } else {
      response = client.getUserByPassport(seria, number, consent);
    }

    String clientId = "";
    String phoneNumber = "";
    String adLogin = "";
    String devices = "[]";
    JsonNode body = response.getBody();
    if (body != null) {
      clientId = body.path("clientID").asText();
      phoneNumber = body.path("phoneForRecovery").asText();
      adLogin = body.path("adlogin").asText();
      Stream<JsonNode> devicesStream = StreamSupport.stream(
          body.path("tokens").spliterator(), false);
      List<JsonNode> devicesList = devicesStream.filter(token -> SUPPORTED_PLATFORMS.contains(token.path("deviceType").asText()))
          .map(token -> token.path("deviceToken"))
          .collect(Collectors.toList());
      devices = devicesList.toString();
    }

    context.getAuthenticationSession().setUserSessionNote("clientID", clientId);
    context.getAuthenticationSession().setUserSessionNote("authSource", AUTH_SOURCE_PASSPORT);
    context.getAuthenticationSession().setAuthNote("phoneForRecovery", phoneNumber);
    context.getAuthenticationSession().setAuthNote("devices", devices);

    RealmModel realm = context.getRealm();
    UserModel user = context.getSession().users().getUserByUsername(realm, adLogin);

    context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, adLogin);

    // we don't want people guessing usernames, so if there is a problem, just continue, but don't set the user
    // a null user will notify further executions, that this was a failure.
    if (user == null) {
      event.clone()
          .detail(Details.USERNAME, adLogin)
          .error(Errors.USER_NOT_FOUND);
      context.clearUser();
    } else if (!user.isEnabled()) {
      event.clone()
          .detail(Details.USERNAME, adLogin)
          .user(user).error(Errors.USER_DISABLED);
      context.clearUser();
    } else {
      context.setUser(user);
    }

    context.success();
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
