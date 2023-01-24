/*
 *        catremodel.dart
 * 
 *    top-level interface to Catre models
 * 
 **/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.				 *
///										 *
///			  All Rights Reserved					 *
///										 *
///  Permission to use, copy, modify, and distribute this software and its	 *
///  documentation for any purpose other than its incorporation into a		 *
///  commercial product is hereby granted without fee, provided that the 	 *
///  above copyright notice appear in all copies and that both that		 *
///  copyright notice and this permission notice appear in supporting		 *
///  documentation, and that the name of Brown University not be used in 	 *
///  advertising or publicity pertaining to distribution of the software 	 *
///  without specific, written prior permission. 				 *
///										 *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
///  OF THIS SOFTWARE.								 *
///										 *
///*******************************************************************************/

import 'catreuniverse.dart';
import 'package:sherpa/globals.dart' as globals;
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;

class CatreModel {
  static final CatreModel _catreModel = CatreModel._internal();

  CatreUniverse? _theUniverse;

  factory CatreModel() {
    return _catreModel;
  }
  CatreModel._internal();

  Future<CatreUniverse> loadUniverse() async {
    var path = "/universe?${globals.catreSession}=${globals.sessionId}";

    var url = Uri.https(globals.catreURL, path);
    var resp = await http.get(url);
    var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (jresp["STATUS"] != "OK") throw Exception("Lost connection to CATRE");
    CatreUniverse u = CatreUniverse.fromJson(jresp);
    _theUniverse = u;
    return u;
  }

  Future<CatreUniverse> getUniverse() async {
    CatreUniverse? u = _theUniverse;
    if (u != null) {
      return u;
    } else {
      return await loadUniverse();
    }
  }
}
