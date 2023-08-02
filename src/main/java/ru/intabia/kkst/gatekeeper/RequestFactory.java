package ru.intabia.kkst.gatekeeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import org.jboss.logging.Logger;
import ru.intabia.kkst.AuthSource;

public class RequestFactory {

  private static final Logger logger = Logger.getLogger(RequestFactory.class);
  private static final String REQUEST_ID = "requestId";
  private static final String CLIENT_IP = "clientIp";
  private static final String CALL_GID = "callGID";
  private static final String SEND_TYPE = "send";
  private static final int OTP_TIMEOUT_SEC = 3 * 60; // TODO: from config
  private static final String GET_USER_BY_ID_ADDR = "ru.fisgroup.ws.bp.vwbr.pos#GetUserByID";
  private static final String GET_USER_BY_PASSPORT_ADDR =
      "ru.fisgroup.ws.bp.vwbr.pos#GetUserByPassport";
  private static final String FIND_MOBILE_BY_DOCUMENT_NUMBER_ADDR =
      "com.vwfs.crm.CRMService#findMobileByDocumentNumber";
  private static final String OTP_SIGN_ADDR = "com.vwfs.crm.OTPService#sign";
  private static final String OTP_CONFIRM_ADDR = "com.vwfs.crm.OTPService#confirm";
  private static final String FCM_SEND_ADDR = "ru.fisgroup.gatekeeper.push.fcm#send";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_OPERATION_CODE = "00000000-0000-0000-0000-000000000000";
  private static final ObjectNode operationCode = objectMapper.createObjectNode() // TODO: from config
      .put("loginByUsername", DEFAULT_OPERATION_CODE)
      .put("loginByPassport", DEFAULT_OPERATION_CODE)
      .put("loginByPhone", DEFAULT_OPERATION_CODE);

  private RequestFactory() {
  }

  public static ByteBuffer createGetUserByIdRequest(String clientIp, int clientPort,
                                                    String userId) {
    Frame frame = new Frame(SEND_TYPE, GET_USER_BY_ID_ADDR, clientIp + ":" + clientPort,
        objectMapper.createObjectNode()
            .put(REQUEST_ID, UUID.randomUUID().toString())
            .put(CLIENT_IP, clientIp),
        objectMapper.createObjectNode()
            .put("userID", userId),
        true);

    return serialize(frame);
  }

  public static ByteBuffer createGetUserByPassportRequest(String clientIp, int clientPort,
                                                          String seria, String number,
                                                          Boolean consent) {
    Frame frame = new Frame(SEND_TYPE, GET_USER_BY_PASSPORT_ADDR, clientIp + ":" + clientPort,
        objectMapper.createObjectNode()
            .put(REQUEST_ID, UUID.randomUUID().toString())
            .put(CLIENT_IP, clientIp),
        objectMapper.createObjectNode()
            .put("seria", seria)
            .put("number", number)
            .put("consent", consent),
        true);

    return serialize(frame);
  }

  public static ByteBuffer createFindMobileByDocumentNumberRequest(String clientIp, int clientPort,
                                                                   String documentNumber) {
    String id = UUID.randomUUID().toString();
    String nowDate = getNowDate();
    Frame frame =
        new Frame(SEND_TYPE, FIND_MOBILE_BY_DOCUMENT_NUMBER_ADDR, clientIp + ":" + clientPort,
            objectMapper.createObjectNode()
                .put(REQUEST_ID, id)
                .put(CLIENT_IP, clientIp),
            objectMapper.createObjectNode()
                .<ObjectNode>set("header", objectMapper.createObjectNode()
                    .put(CALL_GID, id))
                .set("body", objectMapper.createObjectNode()
                    .put("documentNumber", documentNumber)
                    .put("dateSend", nowDate)
                    .put("dateReceive", nowDate)),
            true);

    return serialize(frame);
  }

  public static ByteBuffer createOtpSignRequest(String clientIp, int clientPort,
                                                String login, String phone, AuthSource authSource,
                                                String firstName, String lastName, String middleName) {
    String id = UUID.randomUUID().toString();
    String nowDate = getNowDate();
    Frame frame = new Frame(SEND_TYPE, OTP_SIGN_ADDR, clientIp + ":" + clientPort,
        objectMapper.createObjectNode()
            .put(REQUEST_ID, id)
            .put(CLIENT_IP, clientIp),
        objectMapper.createObjectNode()
            .<ObjectNode>set("headers", objectMapper.createObjectNode()
                .put(CALL_GID, id))
            .set("body", objectMapper.createObjectNode()
                .put("sendDate", nowDate)
                .<ObjectNode>set("operation", objectMapper.createObjectNode()
                    .put("id", getOperationCode(authSource)))
                .set("client", objectMapper.createObjectNode()
                    .put("login", login)
                    .put("phone", phone)
                    .put("ip", clientIp)
                    .set("fullName", objectMapper.createObjectNode()
                        .put("firstName", firstName)
                        .put("lastName", lastName)
                        .put("middleName", middleName)))),
        true);

    return serialize(frame);
  }

  private static String getOperationCode(AuthSource authSource) {
    if (authSource == AuthSource.LOGIN) {
      return operationCode.path("loginByUsername").asText();
    }
    if (authSource == AuthSource.PASSPORT) {
      return operationCode.path("loginByPassport").asText();
    }
    if (authSource == AuthSource.PHONE) {
      return operationCode.path("loginByPhone").asText();
    }
    return DEFAULT_OPERATION_CODE;
  }

  public static ByteBuffer createFcmSendRequest(String clientIp, int clientPort, String devices,
                                                String otp) {
    String id = UUID.randomUUID().toString();
    ArrayNode devicesNode;
    try {
      devicesNode = objectMapper.readValue(devices, ArrayNode.class);
    } catch (JsonProcessingException e) {
      devicesNode = objectMapper.createArrayNode();
    }

    ObjectNode body = objectMapper.createObjectNode()
        .put("priority", "high")
        .put("time_to_live", OTP_TIMEOUT_SEC)
        .<ObjectNode>set("notification", objectMapper.createObjectNode()
            .put("body",
                String.format("Вы входите в приложение Volkswagen Bank. Ваш код: %s", otp)));
    if (devicesNode.size() == 1) {
      body.put("to", devicesNode.get(0).asText());
    } else {
      body.set("registration_ids", devicesNode);
    }

    Frame frame = new Frame(SEND_TYPE, FCM_SEND_ADDR, clientIp + ":" + clientPort,
        objectMapper.createObjectNode()
            .put(REQUEST_ID, id)
            .put(CLIENT_IP, clientIp),
        body,
        true);

    return serialize(frame);
  }

  public static ByteBuffer createOtpConfirmRequest(String clientIp, int clientPort,
                                                   String otpId, String otpCode) {
    String id = UUID.randomUUID().toString();
    String nowDate = getNowDate();
    Frame frame = new Frame(SEND_TYPE, OTP_CONFIRM_ADDR, clientIp + ":" + clientPort,
        objectMapper.createObjectNode()
            .put(REQUEST_ID, id)
            .put(CLIENT_IP, clientIp),
        objectMapper.createObjectNode()
            .<ObjectNode>set("headers", objectMapper.createObjectNode()
                .put(CALL_GID, id))
            .set("body", objectMapper.createObjectNode()
                .put("sendDate", nowDate)
                .put("otpID", otpId)
                .put("code", otpCode)
                .put("ip", clientIp)),
        true);

    return serialize(frame);
  }

  private static ByteBuffer serialize(Frame frame) {
    byte[] byteFrame;
    try {
      byteFrame = objectMapper.writeValueAsBytes(frame);
    } catch (JsonProcessingException e) {
      logger.warn("Can't serialize request", e);
      return ByteBuffer.allocate(0);
    }
    return ByteBuffer.allocate(byteFrame.length + Integer.BYTES)
        .putInt(byteFrame.length)
        .put(byteFrame);
  }

  private static String getNowDate() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(tz);
    return df.format(new Date());
  }
}
