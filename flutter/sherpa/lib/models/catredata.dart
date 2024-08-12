/*
 *        catredata.dart  
 * 
 *    Generic class for holding data from Catre for sherpa
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

import 'catreuniverse.dart';
import 'dart:convert' as convert;
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/globals.dart' as globals;

/// *****
///      CatreData:  generic holder of JSON map for data from CATRE
/// *****

class CatreData {
  Map<String, dynamic> catreData;
  Map<String, dynamic> baseData;
  late CatreUniverse catreUniverse;
  List<Map<String, dynamic>>? _stack;
  bool changed = false;

  CatreData.outer(Map<String, dynamic> data)
      : catreData = data,
        baseData = Map.from(data);
  CatreData(CatreUniverse cu, Map<String, dynamic> data)
      : catreData = data,
        baseData = Map.from(data),
        catreUniverse = cu;
  CatreData.clone(CatreData cd)
      : catreData = Map.from(cd.catreData),
        baseData = Map.from(cd.catreData),
        catreUniverse = cd.catreUniverse;

  void rebuild(Map<String, dynamic> data) {
    catreData = data;
    baseData = Map.from(data);
    setup();
  }

  String getName() => getString("NAME");
  CatreUniverse getUniverse() => catreUniverse;
  String getLabel() => getString("LABEL");
  String getDescription() {
    if (optString("DESCRIPTION") == null) {
      return getLabel();
    }
    if (!getBool("USERDESC")) return getString("DESCRIPTION");
    return buildDescription();
  }

  Map<String, dynamic> getCatreOutput() {
    return catreData;
  }

  List<Map<String, dynamic>>? listCatreOutput(Iterable<CatreData>? itms) {
    if (itms == null) return null;
    List<Map<String, dynamic>> rslt = [];
    for (CatreData cd in itms) {
      Map<String, dynamic> out = cd.getCatreOutput();
      rslt.add(out);
    }
    return rslt;
  }

  @protected
  String buildDescription() {
    return getString("DESCRIPTION");
  }

  @protected
  List<T> buildList<T>(String id, T Function(CatreUniverse, dynamic) fun) {
    List<dynamic>? rdevs = catreData[id] as List<dynamic>?;
    if (rdevs == null) return <T>[];
    List<T> devs = rdevs.map<T>(((x) => fun(catreUniverse, x))).toList();
    return devs;
  }

  @protected
  List<T> buildListFromObject<T>(
    List<dynamic>? rdevs,
    T Function(CatreUniverse, dynamic) fun,
  ) {
    if (rdevs == null) return <T>[];
    List<T> devs = rdevs.map<T>(((x) => fun(catreUniverse, x))).toList();
    return devs;
  }

  @protected
  List<T>? optList<T>(String id, T Function(CatreUniverse, dynamic) fun) {
    if (catreData[id] == null) return null;
    List<dynamic>? rdevs = catreData[id] as List<dynamic>?;
    if (rdevs == null) return <T>[];
    List<T> devs = rdevs.map<T>((x) => fun(catreUniverse, x)).toList();
    return devs;
  }

  @protected
  T buildItem<T>(String id, T Function(CatreUniverse, dynamic) fun) {
    return fun(catreUniverse, catreData[id]);
  }

  @protected
  T? optItem<T>(String id, T Function(CatreUniverse, dynamic) fun) {
    dynamic data = catreData[id];
    if (data == null) return null;
    return fun(catreUniverse, data);
  }

  @protected
  String getString(String id) => catreData[id] as String;
  @protected
  String? optString(String id) => catreData[id] as String?;

  @protected
  bool getBool(String id) => catreData[id] as bool? ?? false;
  @protected
  bool? optBool(String id) => catreData[id] as bool?;

  @protected
  num getNum(String id, [num dflt = 0]) {
    num v = catreData[id] as num? ?? dflt;
    if (!v.isFinite) v = dflt;
    if (v.isNaN) v = dflt;
    return v;
  }

  @protected
  num? optNum(String id) {
    num? v = catreData[id] as num?;
    if (v == null) {
      return null;
    } else if (!v.isFinite) {
      return null;
    } else if (v.isNaN) {
      return null;
    }
    return v;
  }

  @protected
  int getInt(String id, [int dflt = 0]) {
    int v = catreData[id] as int? ?? dflt;
    if (!v.isFinite) {
      v = dflt;
    } else if (v.isNaN) {
      v = dflt;
    }
    return v;
  }

  @protected
  int? optInt(String id) {
    int? v = catreData[id] as int?;
    if (v == null) {
      return null;
    } else if (!v.isFinite) {
      return null;
    } else if (v.isNaN) {
      return null;
    }
    return v;
  }

  @protected
  List<String> getStringList(String id) {
    return catreData[id] as List<String>;
  }

  @protected
  List<String> stringOrStringList(String id) {
    dynamic v = catreData[id];
    if (v == null) return [];
    if (v!.runtimeType == String) {
      return [v as String];
    }
    if (v!.runtimeType == List<String>) {
      return v as List<String>;
    }
    return [v.toString()];
  }

  @protected
  List<String>? optStringList(String id) {
    List<dynamic>? v = catreData[id];
    if (v == null) return null;
    List<String> rslt = [];
    for (dynamic d in v) {
      rslt.add(d.toString());
    }
    return rslt;
  }

  @protected
  List<num>? optNumList(String id) {
    return catreData[id] as List<num>?;
  }

  String? setName(dynamic text) {
    // returns null so they can be used for onSaved, onCondition
    setField("NAME", text);
    return null;
  }

  String? setLabel(dynamic text) {
    setField("LABEL", text);
    return null;
  }

  String? setDescription(dynamic text) {
    setField("DESCRIPTION", text);
    return null;
  }

  bool setField(String fld, dynamic val) {
    if (val == catreData[fld]) return false;
    catreData[fld] = val;
    changed = true;
    return true;
  }

  void defaultField(String fld, dynamic val) {
    if (catreData[fld] != null) return;
    catreData[fld] = val;
    changed = true;
  }

  bool setListField(String fld, List<dynamic> val) {
    if (listEquals(val, catreData[fld])) return false;
    catreData[fld] = val;
    changed = true;
    return true;
  }

  void revert() {
    catreData = Map.from(baseData);
    setup();
  }

  @protected
  void setup() {}

  void push() {
    _stack ??= [];
    _stack?.add(Map.from(catreData));
  }

  bool pop() {
    List<Map<String, dynamic>> s = _stack ?? [];
    if (s.isEmpty) return false;
    Map<String, dynamic> cd = s.removeLast();
    catreData = cd;
    setup();
    return true;
  }

  void save() {
    baseData = catreData;
  }

  @override
  bool operator ==(Object other) {
    if (other.runtimeType != runtimeType) return false;
    CatreData cd = other as CatreData;
    return mapEquals(catreData, cd.catreData);
  }

  @override
  int get hashCode {
    int hc = 0;
    catreData.forEach((k, v) {
      hc += k.hashCode;
      hc ^= v.hashCode;
    });
    return hc;
  }

  Future<Map<String, dynamic>?> issueCommand(String cmd, String argname) async {
    var url = Uri.https(util.getServerURL(), cmd);
    var body = {
      globals.catreSession: globals.sessionId,
      argname: convert.jsonEncode(getCatreOutput()),
    };
    var resp = await http.post(url, body: body);
    if (resp.statusCode >= 400) return null;
    return convert.jsonDecode(resp.body) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>?> issueCommandWithArgs(
    String cmd,
    Map<String, dynamic> args,
  ) async {
    var url = Uri.https(util.getServerURL(), cmd);
    var body = {
      globals.catreSession: globals.sessionId,
    };
    for (String key in args.keys) {
      body[key] = convert.jsonEncode(args[key]);
    }
    var resp = await http.post(url, body: body);
    if (resp.statusCode >= 400) return null;
    return convert.jsonDecode(resp.body) as Map<String, dynamic>;
  }
}
