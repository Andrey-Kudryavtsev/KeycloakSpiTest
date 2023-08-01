package ru.intabia.kkst.gatekeeper;

import com.fasterxml.jackson.databind.JsonNode;

public class Frame {
  private String type;
  private String address;
  private String replyAddress;
  private JsonNode headers;
  private JsonNode body;
  private Boolean send;

  public Frame() {
  }

  public Frame(String type, String address, String replyAddress, JsonNode headers, JsonNode body,
               Boolean send) {
    this.type = type;
    this.address = address;
    this.replyAddress = replyAddress;
    this.headers = headers;
    this.body = body;
    this.send = send;
  }

  @Override
  public String toString() {
    return "Frame{" +
        "type='" + type + '\'' +
        ", address='" + address + '\'' +
        ", replyAddress='" + replyAddress + '\'' +
        ", headers=" + headers +
        ", body=" + body +
        ", send=" + send +
        '}';
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getReplyAddress() {
    return replyAddress;
  }

  public void setReplyAddress(String replyAddress) {
    this.replyAddress = replyAddress;
  }

  public JsonNode getHeaders() {
    return headers;
  }

  public void setHeaders(JsonNode headers) {
    this.headers = headers;
  }

  public JsonNode getBody() {
    return body;
  }

  public void setBody(JsonNode body) {
    this.body = body;
  }

  public Boolean getSend() {
    return send;
  }

  public void setSend(Boolean send) {
    this.send = send;
  }
}
