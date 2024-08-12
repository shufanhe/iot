/*
 *        catrebridgeinfo.dart 
 * 
 *    Dart representation of a CATRE bridge
 * 
 **/
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.                           *
///                                                                              *
///                       All Rights Reserved                                    *
///                                                                              *
///  Permission to use, copy, modify, and distribute this software and its       *
///  documentation for any purpose other than its incorporation into a           *
///  commercial product is hereby granted without fee, provided that the         *
///  above copyright notice appear in all copies and that both that              *
///  copyright notice and this permission notice appear in supporting            *
///  documentation, and that the name of Brown University not be used in         *
///  advertising or publicity pertaining to distribution of the software         *
///  without specific, written prior permission.                                 *
///                                                                              *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS               *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND           *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY     *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY         *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,             *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS              *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE         *
///  OF THIS SOFTWARE.                                                           *
///                                                                              *
///*******************************************************************************/

import 'catredata.dart';
import 'catreuniverse.dart';

class CatreBridge extends CatreData {
  late List<CatreBridgeField> _fields;

  CatreBridge.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>) {
    _fields = buildList("FIELDS", CatreBridgeField.build);
  }

  String getHelpText() => getString("HELP");
  List<CatreBridgeField> getFields() => _fields;
  bool isSingle() => getBool("SINGLE");
  String getBridgeName() => getString("BRIDGE");
  int getCount() {
    int? v = optInt("COUNT");
    v ??= 1;
    return v;
  }

  Future<void> addOrUpdateBridge() async {
    Map<String, dynamic> args = {"BRIDGE": getBridgeName()};
    for (int i = 0; i < getCount(); ++i) {
      for (CatreBridgeField fld in _fields) {
        String key = fld.getKeyName();
        key = key.replaceFirst("#", i.toString());
        String? val = fld.getValue(i);
        if (val != null) {
          args[key] = val;
        } else if (fld.isOptional()) {
          args[key] = "*";
        }
      }
    }
    await issueCommandWithArgs("/bridge/add", args);
  }
}

class CatreBridgeField extends CatreData {
  CatreBridgeField.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>);

  String getHint() => getString("HINT");
  String getType() => getString("TYPE");
  String getKeyName() => getString("KEY");
  String? getValue(int index) {
    if (index < 0) index = 0;
    List<String> vl = stringOrStringList("VALUE");
    if (index >= vl.length) return null;
    return vl[index];
  }

  bool isOptional() {
    bool? v = optBool("OPTIONAL");
    v ??= false;
    return v;
  }

  void setValue(int index, String v) {
    List<String> vl = stringOrStringList("VALUE");
    while (index >= vl.length) {
      vl.add("");
    }
    vl[index] = v;
    setField("VALUE", vl);
    if (v.isNotEmpty || isOptional()) {
      setField("COUNT", vl.length);
    }
  }
}


// end of module catrebridgeinfo.dart  



