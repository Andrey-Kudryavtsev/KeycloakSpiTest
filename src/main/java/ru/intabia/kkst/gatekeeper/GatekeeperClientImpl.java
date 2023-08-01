package ru.intabia.kkst.gatekeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import org.jboss.logging.Logger;

public class GatekeeperClientImpl implements GatekeeperClient {

  private static final Logger logger = Logger.getLogger(GatekeeperClientImpl.class);
  private static final String HOST = "192.168.1.238";
  private static final int PORT = 7003;
  private static final int TIMEOUT_MS = 10000;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public Frame getUserById(String userId) {
    try (Socket socket = new Socket(HOST, PORT);
         OutputStream out = socket.getOutputStream();
         InputStream in = socket.getInputStream()) {

      socket.setSoTimeout(TIMEOUT_MS);
      ByteBuffer request = RequestFactory.createGetUserByIdRequest(
          socket.getLocalAddress().getHostAddress(), socket.getLocalPort(), userId);

      out.write(request.array());

      return readResponse(in);
    } catch (IOException e) {
      logger.error("Error while getUserById", e);
      return new Frame();
    }
  }

  public Frame findMobileByDocumentNumber(String documentNumber) {
    try (Socket socket = new Socket(HOST, PORT);
         OutputStream out = socket.getOutputStream();
         InputStream in = socket.getInputStream()) {

      socket.setSoTimeout(TIMEOUT_MS);
      ByteBuffer request = RequestFactory.createFindMobileByDocumentNumberRequest(
          socket.getLocalAddress().getHostAddress(), socket.getLocalPort(), documentNumber);

      out.write(request.array());

      return readResponse(in);
    } catch (IOException e) {
      logger.error("Error while findMobileByDocumentNumber", e);
      return new Frame();
    }
  }

  public Frame getUserByPassport(String seria, String number, boolean consent) {
    try (Socket socket = new Socket(HOST, PORT);
         OutputStream out = socket.getOutputStream();
         InputStream in = socket.getInputStream()) {

      socket.setSoTimeout(TIMEOUT_MS);
      ByteBuffer request = RequestFactory.createGetUserByPassportRequest(
          socket.getLocalAddress().getHostAddress(), socket.getLocalPort(),
          seria, number, consent);

      out.write(request.array());

      return readResponse(in);
    } catch (IOException e) {
      logger.error("Error while getUserByPassport", e);
      return new Frame();
    }
  }

  public Frame otpSign(String login, String phone) {
    try (Socket socket = new Socket(HOST, PORT);
         OutputStream out = socket.getOutputStream();
         InputStream in = socket.getInputStream()) {

      socket.setSoTimeout(TIMEOUT_MS);
      ByteBuffer request = RequestFactory.createOtpSignRequest(
          socket.getLocalAddress().getHostAddress(), socket.getLocalPort(), login, phone);

      out.write(request.array());

      return readResponse(in);
    } catch (IOException e) {
      logger.error("Error while otpSign", e);
      return new Frame();
    }
  }

  public Frame fcmSend(String devices, String otp) {
    try (Socket socket = new Socket(HOST, PORT);
         OutputStream out = socket.getOutputStream();
         InputStream in = socket.getInputStream()) {

      socket.setSoTimeout(TIMEOUT_MS);
      ByteBuffer request = RequestFactory.createFcmSendRequest(
          socket.getLocalAddress().getHostAddress(), socket.getLocalPort(), devices, otp);

      out.write(request.array());

      return readResponse(in);
    } catch (IOException e) {
      logger.error("Error while fcmSend", e);
      return new Frame();
    }
  }

  public Frame otpConfirm(String otpId, String otp) {
    try (Socket socket = new Socket(HOST, PORT);
         OutputStream out = socket.getOutputStream();
         InputStream in = socket.getInputStream()) {

      socket.setSoTimeout(TIMEOUT_MS);
      ByteBuffer request = RequestFactory.createOtpConfirmRequest(
          socket.getLocalAddress().getHostAddress(), socket.getLocalPort(), otpId, otp);

      out.write(request.array());

      return readResponse(in);
    } catch (IOException e) {
      logger.error("Error while otpConfirm", e);
      return new Frame();
    }
  }

  private Frame readResponse(InputStream in) {
    byte[] buf = new byte[Integer.BYTES];

    try {
      int readBytes;
      readBytes = in.read(buf, 0, Integer.BYTES);
      if (readBytes != Integer.BYTES) {
        throw new IOException("");
      }
      ByteBuffer response = ByteBuffer.wrap(buf);
      int responseSize = response.getInt();

      buf = new byte[responseSize];
      readBytes = in.read(buf, 0, responseSize);
      if (readBytes != responseSize) {
        throw new IOException("");
      }
    } catch (IOException e) {
      logger.warn("Can't read response", e);
      return new Frame();
    }

    Frame responseFrame;
    try {
      responseFrame = objectMapper.readValue(buf, Frame.class);
    } catch (IOException e) {
      logger.warn("Can't deserialize response", e);
      return new Frame();
    }
    return responseFrame;
  }
}
