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

import 'package:meta/meta.dart';
import 'catreuniverse.dart';

/// *****
///      CatreData:  generic holder of JSON map for data from CATRE
/// *****

class CatreData {
  Map<String, dynamic> catreData;
  late CatreUniverse catreUniverse;

  CatreData.outer(Map<String, dynamic> data) : catreData = data;
  CatreData(CatreUniverse cu, Map<String, dynamic> data)
      : catreData = data,
        catreUniverse = cu;

  String getName() => getString("NAME");
  String getLabel() => getString("LABEL");
  String getDescription() => getString("DESCRIPTION");

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
  num getNum(String id) => catreData[id] as num;
  @protected
  num? optNum(String id) => catreData[id] as num?;

  @protected
  List<String> getStringList(String id) {
    return catreData[id] as List<String>;
  }

  @protected
  List<String>? optStringList(String id) {
    return catreData[id] as List<String>?;
  }

  @protected
  List<num>? optNumList(String id) {
    return catreData[id] as List<num>?;
  }
}
