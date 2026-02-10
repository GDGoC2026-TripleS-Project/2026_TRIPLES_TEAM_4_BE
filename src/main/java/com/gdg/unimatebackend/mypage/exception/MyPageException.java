package com.gdg.unimatebackend.mypage.exception;

import lombok.Getter;

@Getter
public class MyPageException extends RuntimeException {

  private final String code;
  private final int status;

  public MyPageException(String code, String message, int status) {
    super(message);
    this.code = code;
    this.status = status;
  }
}
