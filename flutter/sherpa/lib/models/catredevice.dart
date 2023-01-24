/*
 *        catredevice.dart  
 * 
 *    Dart representation of a CATRE device
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
import 'catreparameter.dart';

/// *****
///      CatreDevice description of a device
/// *****

class CatreDevice extends CatreData {
  late List<CatreParameter> _parameters;
  late List<CatreTransition> _transitions;

  CatreDevice.build(dynamic d) : super(d as Map<String, dynamic>) {
    _parameters = buildList("PARAMETERS", CatreParameter.build);
    _transitions = buildList("TRANSITIONS", CatreTransition.build);
  }

  String getDeviceId() => getString("UID");

  bool isEnabled() => getBool("ENABLED");
  bool isCalendarDevice() => getBool("ISCALENDAR");

  List<CatreParameter> getParameters() => _parameters;
  List<CatreTransition> getTransitions() => _transitions;

  String? getVirtualDeviceType() => optString("VTYPE");
  String getWeatherCity() => getString("CITY");
  String getWeatherUnits() => getString("UNITS");
} // end of CatreDevice

/// *****
///      CatreTransition -- transition for a device
/// *****

class CatreTransition extends CatreData {
  late List<CatreParameter> _parameters;

  CatreTransition.build(dynamic data) : super(data as Map<String, dynamic>) {
    _parameters = buildList("DEFAULTS", CatreParameter.build);
  }

  List<CatreParameter> getParameters() => _parameters;
}
