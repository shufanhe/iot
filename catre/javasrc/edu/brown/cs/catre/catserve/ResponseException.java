package edu.brown.cs.catre.catserve;

public class ResponseException extends Exception {
    private int statusCode;
    private String responseMessage;

    public ResponseException(int statusCode, String responseMessage) {
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
    }

    public int getStatus() {
        return statusCode;
    }

    public String getMessage() {
        return responseMessage;
    }
}