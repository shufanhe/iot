package edu.brown.cs.catre.catserve;

import com.sun.net.httpserver.HttpExchange;

public interface ICatreHandler {
    public void handle(HttpExchange e);
}
