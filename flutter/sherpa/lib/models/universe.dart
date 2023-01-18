/*
 *        universe.dart  
 * 
 *    Dart representation of a CATRE universe
 * 
 **/

import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;

Future<CatreUniverse?> loadUniverse() async {
  var path = "/universe?${globals.catreSession}=${globals.sessionId}";

  var url = Uri.https(globals.catreURL, path);
  var resp = await http.get(url);
  var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
  if (jresp["STATUS"] != "OK") return null;
  return CatreUniverse.fromJson(jresp);
}

class CatreUniverse {
  CatreUniverse.fromJson(Map<String, dynamic> json);
}
