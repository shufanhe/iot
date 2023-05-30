/*
 *      recheck.dart
 * 
 *    Code to try to compute location periodically
 * 
 */
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
///******************************************************************************


library alds.recheck;

import 'dart:async';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_blue/flutter_blue.dart';
import 'locator.dart';
import 'package:mutex/mutex.dart';
import 'util.dart' as util;

FlutterBlue _flutterBlue = FlutterBlue.instance;
bool _checkLocation = false;
bool _checkBluetooth = false;
Locator _locator = Locator();
final _doingRecheck = Mutex();

Future<void> initialize() async {
  LocationPermission perm = await Geolocator.checkPermission();
  if (perm == LocationPermission.denied) {
    perm = await Geolocator.requestPermission();
  }
  if (perm != LocationPermission.denied) _checkLocation = true;
  util.log("CHECK GEOLOCATION $perm $_checkLocation");
  _flutterBlue.setLogLevel(LogLevel.debug);
  _checkBluetooth = await _flutterBlue.isAvailable;
  bool ison = await _flutterBlue.isOn;
  util.log("CHECK BT $_checkBluetooth $ison ${_flutterBlue.state}");
}

Future<LocationData> recheck() async {
  await _doingRecheck.acquire();
  try {
    util.log("START RECHECK");
    Position? curpos;
    if (_checkLocation) {
      try {
        curpos = await Geolocator.getCurrentPosition()
            .timeout(const Duration(seconds: 5));
        double lat = curpos.latitude;
        double long = curpos.longitude;
        double elev = curpos.altitude;
        double speed = curpos.speed;
        double speeda = curpos.speedAccuracy;
        double posa = curpos.accuracy;
        util.log("GEO FOUND $lat $long $elev $speed $speeda $posa");
      } catch (e) {
        util.log("NO GEO LOCATION $e");
      }
    }

    Stream<ScanResult> st =
        _flutterBlue.scan(timeout: const Duration(seconds: 6));
    List<BluetoothData> btdata = await st.fold([], _btscan2);

    // no way to scan wifi access points on ios

    LocationData rslt = _locator.updateLocation(curpos, btdata);
    util.log("FINISHED RECHECK)");
    return rslt;
  } finally {
    _doingRecheck.release();
    util.flushLogs();
  }
}

void _btscan1(ScanResult r) async {
  int rssi = r.rssi;
  String name = r.device.name;
  String mac = r.device.id.id;
  String typ = r.device.type.name;
  String svd = r.advertisementData.serviceData.toString();
  String mfd = r.advertisementData.manufacturerData.toString();
  bool conn = r.advertisementData.connectable;
  String lname = r.advertisementData.localName;
  util.log("BT FOUND $mac $conn $rssi $typ $svd $mfd $name $lname");
}

List<BluetoothData> _btscan2(List<BluetoothData> bl, ScanResult r) {
  _btscan1(r);
  BluetoothData btd = BluetoothData(r.device.id.id, r.rssi, r.device.name);
  bl.add(btd);
  return bl;
}
