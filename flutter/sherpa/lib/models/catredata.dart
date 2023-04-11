/*
 *        catredata.dart  
 * 
 *    Generic class for holding data from Catre for sherpa
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
import 'package:flutter/foundation.dart';

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

  String getName() => getString("NAME");
  String getLabel() => getString("LABEL");
  String getDescription() {
    if (!getBool("USERDESC")) return getString("DESCRIPTION");
    return buildDescription();
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
  num getNum(String id, [num dflt = 0]) => catreData[id] as num? ?? dflt;
  @protected
  num? optNum(String id) => catreData[id] as num?;

  @protected
  int getInt(String id, [int dflt = 0]) => catreData[id] as int? ?? dflt;
  @protected
  int? optInt(String id) => catreData[id] as int?;

  @protected
  List<String> getStringList(String id) {
    return catreData[id] as List<String>;
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
}
