package ru.intabia.kkst.gatekeeper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GatekeeperClientMock implements GatekeeperClient {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Frame getUserById(String userId) {
    return null;
  }

  @Override
  public Frame findMobileByDocumentNumber(String documentNumber) {
    return null;
  }

  @Override
  public Frame getUserByPassport(String seria, String number, boolean consent) {
    JsonNode header = objectMapper.createObjectNode()
        .put("requestId", "a9db0d1c-923f-4a54-bd32-af162fef7437")
        .put("hostname", "localhost.localdomain");
    JsonNode body = objectMapper.createObjectNode()
        .put("clientID", "5")
        .put("phoneForRecovery", "71111111112")
        .<ObjectNode>set("tokens", objectMapper.createArrayNode())
        .put("adlogin", "Клиент5");
    return new Frame("message", "", "", header, body, true);
  }

  @Override
  public Frame otpSign(String login, String phone) {
    return null;
  }

  @Override
  public Frame fcmSend(String devices, String otp) {
    return null;
  }

  @Override
  public Frame otpConfirm(String otpId, String otp) {
    return null;
  }
}
