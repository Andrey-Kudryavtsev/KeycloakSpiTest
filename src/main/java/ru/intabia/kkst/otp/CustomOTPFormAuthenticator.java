package ru.intabia.kkst.otp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import ru.intabia.kkst.AuthSource;
import ru.intabia.kkst.gatekeeper.Frame;
import ru.intabia.kkst.gatekeeper.GatekeeperClient;
import ru.intabia.kkst.gatekeeper.GatekeeperClientImpl;

public class CustomOTPFormAuthenticator extends OTPFormAuthenticator {

  private static final String OTP_SEND_ATTEMPTS = "otpSendAttempts";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final GatekeeperClient client = new GatekeeperClientImpl();
  private static final SecureRandom numberGenerator = new SecureRandom();

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    sendOtp(context);
    super.authenticate(context);
  }

  @Override
  public void validateOTP(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
    if (inputData.getFirst("resendOtp") != null) {
      sendOtp(context);
      Response challengeResponse = challenge(context, null);
      context.challenge(challengeResponse);
      return;
    }

    String inputOtp = inputData.getFirst("otp");

    UserModel userModel = context.getUser();
    if (!enabledUser(context, userModel)) {
      // error in context is set in enabledUser/isDisabledByBruteForce
      return;
    }

    if (inputOtp == null) {
      Response challengeResponse = challenge(context, null);
      context.challenge(challengeResponse);
      return;
    }

    boolean valid;
    String otp = context.getAuthenticationSession().getAuthNote("otp");
    if (otp != null) {
      valid = otp.equals(inputOtp);
    } else {
      String otpId = context.getAuthenticationSession().getAuthNote("otpId");
      Frame response = client.otpConfirm(otpId, inputOtp);
      JsonNode body =
          response.getBody() != null ? response.getBody() : objectMapper.createObjectNode();
      JsonNode state = body.path("body").path("state");
      valid = "SUCCESS".equalsIgnoreCase(state.asText()) || "OK".equalsIgnoreCase(state.asText());
    }

    if (!valid) {
      context.getEvent().user(userModel)
          .error(Errors.INVALID_USER_CREDENTIALS);
      Response challengeResponse =
          challenge(context, Messages.INVALID_TOTP, Validation.FIELD_OTP_CODE);
      context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
      return;
    }

    String authSource = context.getAuthenticationSession().getUserSessionNotes().get("authSource");

    RoleModel role = context.getAuthenticationSession().getClient().getRole("otp");
    context.getUser().grantRole(role);
    if (AuthSource.PHONE.getValue().equals(authSource)) {
      RoleModel roleUser = context.getAuthenticationSession().getClient().getRole("role:widget");
      context.getUser().grantRole(roleUser);
    } else {
      RoleModel roleWidget = context.getAuthenticationSession().getClient().getRole("role:user");
      context.getUser().grantRole(roleWidget);
    }
    context.success();
  }

  private void sendOtp(AuthenticationFlowContext context) {
    int otpSendAttempts = Integer.parseInt(getAuthNoteOrDefault(context, OTP_SEND_ATTEMPTS, "0"));
    if (otpSendAttempts == 3) {
      Response challengeResponse = challenge(context, "tooManyAttempts", null);
      context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
      return;
    }
    // TODO: add timer
    String login = context.getAuthenticationSession().getAuthNote("platformUserLogin");
    String phone = context.getAuthenticationSession().getAuthNote("phoneForRecovery");
    String devices = context.getAuthenticationSession().getAuthNote("devices");
    String authSource = context.getAuthenticationSession().getUserSessionNotes().get("authSource");
    Frame response;
    if (AuthSource.PHONE.getValue().equals(authSource)) {
      response = client.otpSign(login, phone);
    } else if (userHasLoggedDevices(devices) && otpSendAttempts == 0) {
      String otp = generateVerificationCode();
      context.getAuthenticationSession().setAuthNote("otp", otp);
      response = client.fcmSend(devices, otp);
    } else {
      response = client.otpSign(login, phone);
    }
    context.getAuthenticationSession().setAuthNote(OTP_SEND_ATTEMPTS, String.valueOf(otpSendAttempts+1));
    validateResponse(context, response);
  }

  private String generateVerificationCode() {
    return String.format("%06d", numberGenerator.nextInt(1000000));
  }

  private void validateResponse(AuthenticationFlowContext context, Frame response) {
    // TODO: add fcmSend response validation
    JsonNode body =
        response.getBody() != null ? response.getBody() : objectMapper.createObjectNode();
    String otpId = body.path("body").path("otpID").asText();
    context.getAuthenticationSession().setAuthNote("otpId", otpId);
  }

  private boolean userHasLoggedDevices(String devices) {
    ArrayNode devicesNode;
    try {
      devicesNode = objectMapper.readValue(devices, ArrayNode.class);
    } catch (JsonProcessingException e) {
      devicesNode = objectMapper.createArrayNode();
    }

    return !devicesNode.isEmpty();
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true;
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
  }

  @Override
  public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {
    return Collections.emptyList();
  }

  @Override
  protected Response createLoginForm(LoginFormsProvider form) {
    return form.createForm("custom-login-otp.ftl");
  }

  private String getAuthNoteOrDefault(AuthenticationFlowContext context, String name, String def) {
    String note = context.getAuthenticationSession().getAuthNote(name);
    return note != null ? note : def;
  }
}
