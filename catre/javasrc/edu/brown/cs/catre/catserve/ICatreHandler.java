package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreSession;
import com.sun.net.httpserver.HttpExchange;

public interface ICatreHandler {
    public void handle(CatreSession cs, HttpExchange e);
}
