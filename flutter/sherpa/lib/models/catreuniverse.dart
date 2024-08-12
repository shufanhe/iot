/*
 *        catreuniverse.dart  
 * 
 *    Dart representation of a CATRE universe
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
import 'catredevice.dart';
import 'catreprogram.dart';
import 'catrebridge.dart';

/// *****
///      CatreUniverse:  Description of the universe
/// *****

class CatreUniverse extends CatreData {
  late Map<String, CatreDevice> _devices;
  late List<CatreDevice> _deviceList;
  late CatreProgram _theProgram;
  late Map<String, CatreBridge> _bridgeData = {};

  CatreUniverse.fromJson(super.data) : super.outer() {
    catreUniverse = this;
    _deviceList = buildList("DEVICES", CatreDevice.build);
    _devices = {};
    for (CatreDevice cd in _deviceList) {
      _devices[cd.getDeviceId()] = cd;
      String s = cd.getName().toLowerCase();
      _devices[s] = cd;
    }
    _theProgram = buildItem("PROGRAM", CatreProgram.build);
  }

  void addBridges(dynamic obj) {
    List<dynamic> data = obj["BRIDGES"];
    List<CatreBridge> brl = buildListFromObject(data, CatreBridge.build);
    _bridgeData = {};
    for (CatreBridge br in brl) {
      String nm = br.getBridgeName();
      _bridgeData[nm] = br;
    }
  }

  List<CatreDevice> getDevices() => _deviceList;

  CatreBridge? getBridge(String nm) => _bridgeData[nm];

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
      String s = id.toLowerCase();
      cd = _devices[s];
    }
    if (cd == null) {
      cd = CatreDevice.dummy(this, id);
      _devices[id] = cd;
    }
    return cd;
  }

  CatreDevice? findOptDevice(String? id) {
    if (id == null) return null;
    return findDevice(id);
  }

  CatreDevice? findDeviceByName(String name) {
    for (CatreDevice cd in _deviceList) {
      if (cd.getName() == name || cd.getDeviceId() == name) return cd;
    }
    return null;
  }

  CatreProgram getProgram() => _theProgram;

  Map<String, CatreCondition> getSharedConditions() {
    return _theProgram.getSharedConditions();
  }

  String getUserId() => getString("USER_ID");

  List<String> getBridgeNames() => getStringList("BRIDGES");
} // end of class CatreUniverse







