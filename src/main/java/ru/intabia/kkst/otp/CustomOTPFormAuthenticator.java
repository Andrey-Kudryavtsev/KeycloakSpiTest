package ru.intabia.kkst.otp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
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

  private static final Logger log = Logger.getLogger(CustomOTPFormAuthenticator.class.getName());
  private static final String OTP_RESEND_ATTEMPTS = "otpResendAttempts";
  private static final String OTP_LAST_RESEND_ATTEMPT_MS = "otpLastResendAttemptMs";
  private static final int NO_OTP_RESEND_ATTEMPTS = 0;
  private static final int MAX_OTP_RESEND_ATTEMPTS = 3;
  private static final long OTP_RESEND_COOLDOWN_MS = 30000;
  private static final boolean SEND_SMS_ON_FAIL = true; // always true in GW
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

    UserModel userModel = context.getUser();
    if (!enabledUser(context, userModel)) {
      // error in context is set in enabledUser/isDisabledByBruteForce
      return;
    }

    boolean valid = confirmInputOtp(context, inputData);

    if (!valid) {
      context.getEvent().user(userModel)
          .error(Errors.INVALID_USER_CREDENTIALS);
      Response challengeResponse =
          challenge(context, Messages.INVALID_TOTP, Validation.FIELD_OTP_CODE);
      context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
      return;
    }

    String authSource = context.getAuthenticationSession().getUserSessionNotes().get("authSource");

    setRoles(context, authSource);

    context.success();
  }

  private boolean confirmInputOtp(AuthenticationFlowContext context,
                                  MultivaluedMap<String, String> inputData) {
    String inputOtp = inputData.getFirst("otp");
    if (inputOtp == null) {
      return false;
    }

    String otp = context.getAuthenticationSession().getAuthNote("otp");
    if (otp != null) {
      return otp.equals(inputOtp);
    } else {
      String otpId = context.getAuthenticationSession().getAuthNote("otpId");
      Frame response = client.otpConfirm(otpId, inputOtp);
      JsonNode body =
          response.getBody() != null ? response.getBody() : objectMapper.createObjectNode();
      JsonNode state = body.path("body").path("state");
      return "SUCCESS".equalsIgnoreCase(state.asText()) || "OK".equalsIgnoreCase(state.asText());
    }
  }

  private void setRoles(AuthenticationFlowContext context, String authSource) {
    RoleModel role = context.getAuthenticationSession().getClient().getRole("otp");
    context.getUser().grantRole(role);
    if (AuthSource.PHONE.getValue().equals(authSource)) {
      RoleModel roleUser = context.getAuthenticationSession().getClient().getRole("role:widget");
      context.getUser().grantRole(roleUser);
    } else {
      RoleModel roleWidget = context.getAuthenticationSession().getClient().getRole("role:user");
      context.getUser().grantRole(roleWidget);
    }
  }

  private void sendOtp(AuthenticationFlowContext context) {
    boolean isFirstSend =
        context.getAuthenticationSession().getAuthNote(OTP_RESEND_ATTEMPTS) == null;

    boolean success = handleOtpResend(context, isFirstSend);
    if (!success) {
      return;
    }

    String devices = context.getAuthenticationSession().getAuthNote("devices");
    String authSource = context.getAuthenticationSession().getUserSessionNotes().get("authSource");
    if (AuthSource.PHONE.getValue().equals(authSource)) {
      sendOtpSignRequest(context);
    } else if (userHasLoggedDevices(devices) && isFirstSend) {
      sendFcmRequest(context);
    } else {
      sendOtpSignRequest(context);
    }
  }

  private void sendOtpSignRequest(AuthenticationFlowContext context) {
    String login = context.getAuthenticationSession().getAuthNote("platformUserLogin");
    String phone = context.getAuthenticationSession().getAuthNote("phoneForRecovery");
    UserModel user = context.getUser();

    Frame response = client.otpSign(login, phone, user.getFirstName(), user.getLastName(), "");
    validateOtpResponse(context, response);
  }

  private void sendFcmRequest(AuthenticationFlowContext context) {
    String devices = context.getAuthenticationSession().getAuthNote("devices");

    String otp = generateVerificationCode();
    context.getAuthenticationSession().setAuthNote("otp", otp);

    Frame response = client.fcmSend(devices, otp);
    validateFcmResponse(context, response);
  }

  private boolean handleOtpResend(AuthenticationFlowContext context, boolean isFirstSend) {
    if (!isFirstSend) {
      int otpResendAttempts =
          Integer.parseInt(context.getAuthenticationSession().getAuthNote(OTP_RESEND_ATTEMPTS));

      if (otpResendAttempts >= MAX_OTP_RESEND_ATTEMPTS) {
        long otpLastResendAttemptMs = Long.parseLong(context.getAuthenticationSession().getAuthNote(OTP_LAST_RESEND_ATTEMPT_MS));
        long otpLastResendAttemptTimeElapsedMs = System.currentTimeMillis() - otpLastResendAttemptMs;

        if (otpLastResendAttemptTimeElapsedMs >= OTP_RESEND_COOLDOWN_MS) {
          otpResendAttempts = NO_OTP_RESEND_ATTEMPTS;
          context.getAuthenticationSession().setAuthNote(OTP_RESEND_ATTEMPTS, String.valueOf(NO_OTP_RESEND_ATTEMPTS));
          context.getAuthenticationSession().setAuthNote(OTP_LAST_RESEND_ATTEMPT_MS, null);
        } else {
          long waitForSec = (OTP_RESEND_COOLDOWN_MS - otpLastResendAttemptTimeElapsedMs) / 1000;
          Response challengeResponse =
              challenge(context, String.format("Too many attempts, wait %ds", waitForSec), null);
          context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
          return false;
        }
      }

      otpResendAttempts++;
      context.getAuthenticationSession().setAuthNote(OTP_RESEND_ATTEMPTS, String.valueOf(otpResendAttempts));
      context.getAuthenticationSession().setAuthNote(OTP_LAST_RESEND_ATTEMPT_MS, String.valueOf(System.currentTimeMillis()));
    } else {
      context.getAuthenticationSession().setAuthNote(OTP_RESEND_ATTEMPTS, String.valueOf(NO_OTP_RESEND_ATTEMPTS));
    }
    return true;
  }

  private String generateVerificationCode() {
    return String.format("%06d", numberGenerator.nextInt(1000000));
  }

  private void validateOtpResponse(AuthenticationFlowContext context, Frame response) {
    JsonNode body =
        response.getBody() != null ? response.getBody() : objectMapper.createObjectNode();
    String otpId = body.path("body").path("otpID").asText();
    context.getAuthenticationSession().setAuthNote("otpId", otpId);
  }

  private void validateFcmResponse(AuthenticationFlowContext context, Frame response) {
    JsonNode body =
        response.getBody() != null ? response.getBody() : objectMapper.createObjectNode();
    if (body.path("success").asInt() == 0) {
      log.warning("Не удалось отправить push-уведомление");
      if (SEND_SMS_ON_FAIL) {
        sendOtpSignRequest(context);
      } else {
        Response challengeResponse =
            challenge(context, "Не удалось отправить push-уведомление", null);
        context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challengeResponse);
      }
    }
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
}
