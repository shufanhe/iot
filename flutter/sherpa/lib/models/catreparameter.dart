/*
 *        catreparameter.dart 
 * 
 *    Dart representation of a CATRE universe
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

import 'catredata.dart';
import 'catreprogram.dart';
import 'catreuniverse.dart';

/// *****
///      CatreParameter -- information about a parameter
/// *****

class CatreParameter extends CatreData {
  CatreParameter.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>);

  String getParameterType() => getString("TYPE");
  bool isSensor() => getBool("ISSENSOR");
  num getUseCount() => getNum("USECOUNT");
  List<String>? getValues() => optStringList("VALUES");
  List<String>? getAllUnits() => optStringList("UNITS");
  String? getDefaultUnit() => optString("UNIT");
  String? getValue() => optString("VALUE");

  bool isValidSensor(bool trig) {
    if (!isSensor()) return false;
    switch (getParameterType()) {
      case "BOOLEAN":
      case "ENUM":
      case "STRINGLIST":
      case "ENUMREF":
        List<String>? vals = getValues();
        if (vals == null || vals.isEmpty) return false;
        break;
      case "TIME":
      case "DATETIME":
        break;
      case "DATE":
        if (trig) return false;
        break;
      case "INTEGER":
      case "REAL":
        num v1 = getMinValue();
        num v2 = getMaxValue();
        if (v2 - v1 > 200) return false;
        break;
      case "STRING":
        return false;
    }
    return true;
  }

// ENUMREF Parameter
  CatreParamRef getParameterReference() {
    return buildItem("PARAMREF", CatreParamRef.build);
  }

// INT, REAL parameter
  num getMinValue() => getNum("MIN");
  num getMaxValue() => getNum("MAX");

  List<String> getOperators() {
    List<String> ops = [];
    switch (getParameterType()) {
      case "STRING":
      case "BOOLEAN":
      case "SET":
      case "ENUM":
      case "STRINGLIST":
      case "ENUMREF":
        ops.add("EQL");
        ops.add("NEQ");
        break;
      case "INTEGER":
      case "REAL":
        ops.add("GEQ");
        ops.add("GTR");
        ops.add("LEQ");
        ops.add("LSS");
        break;
      case "TIME":
      case "DATETIME":
        ops.add("LEQ");
        ops.add("GEQ");
        break;
      case "DATE":
        ops.add("LEQ");
        ops.add("EQL");
        ops.add("GEQ");
        break;
    }
    return ops;
  }

  List<String> getTriggerOperators() {
    List<String> ops = [];
    switch (getParameterType()) {
      case "STRING":
      case "BOOLEAN":
      case "SET":
      case "ENUM":
      case "STRINGLIST":
      case "ENUMREF":
      case "INTEGER":
      case "REAL":
        ops.add("EQL");
        ops.add("NEQ");
        break;
      case "TIME":
      case "DATETIME":
        ops.add("EQL");
        break;
    }
    return ops;
  }
}

/// *****
///     CatreParameterSet -- list of parameters with values
///  *****

class CatreParameterSet extends CatreData {
  late List<CatreParameter> _parameters;
  CatreParameterSet.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>) {
    _parameters = buildList("PARAMETERS", CatreParameter.build);
  }

  List<CatreParameter> getParameters() => _parameters;
}

