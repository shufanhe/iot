/*
 *        catreuniverse.dart  
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
import 'catredevice.dart';
import 'catreprogram.dart';

/// *****
///      CatreUniverse:  Description of the universe
/// *****

class CatreUniverse extends CatreData {
  late Map<String, CatreDevice> _devices;
  late List<CatreDevice> _deviceList;
  late CatreProgram _theProgram;

  CatreUniverse.fromJson(super.data) : super.outer() {
    catreUniverse = this;
    _deviceList = buildList("DEVICES", CatreDevice.build);
    _devices = {};
    for (CatreDevice cd in _deviceList) {
      _devices[cd.getDeviceId()] = cd;
    }
    _theProgram = buildItem("PROGRAM", CatreProgram.build);
  }

  List<CatreDevice> getDevices() => _deviceList;

  Iterable<CatreDevice> getActiveDevices() {
    return _deviceList.where((cd) => cd.isEnabled());
  }

  Iterable<CatreDevice> getOutputDevices() {
    return _deviceList.where((cd) => cd.isEnabled() && cd.isOutput());
  }

  Iterable<CatreDevice> getInputDevices() {
    return _deviceList.where((cd) => cd.isInput());
  }

  CatreDevice findDevice(String id) {
    CatreDevice? cd = _devices[id];
    if (cd == null) {
      cd = CatreDevice.dummy(this, id);
      _devices[id] = cd;
    }
    return cd;
  }

  CatreDevice? findDeviceByName(String name) {
    for (CatreDevice cd in _deviceList) {
      if (cd.getName() == name || cd.getDeviceId() == name) return cd;
    }
    return null;
  }

  CatreProgram getProgram() => _theProgram;

  String getUserId() => getString("USER_ID");

  List<String> getBridges() => getStringList("BRIDGES");
} // end of class CatreUniverse


