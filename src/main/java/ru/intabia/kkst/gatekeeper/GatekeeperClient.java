package ru.intabia.kkst.gatekeeper;

public interface GatekeeperClient {

  /**
   * Получить информацию о пользователе по логину на платформе.
   *
   * @param userId логин пользователя
   * @return
   */
  Frame getUserById(String userId);

  Frame findMobileByDocumentNumber(String documentNumber);

  Frame getUserByPassport(String seria, String number, boolean consent);

  Frame otpSign(String login, String phone, String firstName, String lastName, String middleName);

  Frame fcmSend(String devices, String otp);

  Frame otpConfirm(String otpId, String otp);
}
