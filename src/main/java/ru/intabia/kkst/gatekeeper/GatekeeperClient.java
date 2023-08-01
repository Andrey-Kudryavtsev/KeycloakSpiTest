package ru.intabia.kkst.gatekeeper;

public interface GatekeeperClient {

  Frame getUserById(String userId);

  Frame findMobileByDocumentNumber(String documentNumber);

  Frame getUserByPassport(String seria, String number, boolean consent);

  Frame otpSign(String login, String phone);

  Frame fcmSend(String devices, String otp);

  Frame otpConfirm(String otpId, String otp);
}
