package edu.brown.cs.catre.catserve;

public class ResponseException extends Exception {
    private int statusCode;
    private String responseMessage;

    public ResponseException(int statuscode, String responsemessage) {
        this.statusCode = statuscode;
        this.responseMessage = responsemessage;
    }

    public int getStatus() {
        return statusCode;
    }

    public String getMessage() {
        return responseMessage;
    }
}