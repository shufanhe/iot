/*
 *      recheck.dart
 * 
 *    Code to try to compute location periodically
 * 
 */

library alds.recheck;

import 'dart:async';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_blue/flutter_blue.dart';
import 'locator.dart';

FlutterBlue _flutterBlue = FlutterBlue.instance;
bool _checkLocation = false;
bool _checkBluetooth = false;
Locator _locator = Locator();

Future<void> initialize() async {
  LocationPermission perm = await Geolocator.checkPermission();
  if (perm == LocationPermission.denied) {
    perm = await Geolocator.requestPermission();
  }
  if (perm != LocationPermission.denied) _checkLocation = true;
  print("CHECK GEOLOCATION $perm $_checkLocation");
  _flutterBlue.setLogLevel(LogLevel.debug);
  _checkBluetooth = await _flutterBlue.isAvailable;
  bool ison = await _flutterBlue.isOn;
  print("CHECK BT $_checkBluetooth $ison ${_flutterBlue.state}");
  // _flutterBlue.scanResults.listen(_btscanner);
}

Future<void> recheck() async {
  print("START RECHECK");
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
      print("GEO FOUND $lat $long $elev $speed $speeda $posa");
    } catch (e) {
      print("NO GEO LOCATION $e");
    }
  }

  Stream<ScanResult> st =
      _flutterBlue.scan(timeout: const Duration(seconds: 10));
  List<BluetoothData> btdata = await st.fold([], _btscan2);

  // no way to scan wifi access points on ios

  _locator.updateLocation(curpos, btdata);

  print("FINISH RECHECK");
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
  print("BT FOUND $mac $conn $rssi $typ $svd $mfd $name $lname");
}

List<BluetoothData> _btscan2(List<BluetoothData> bl, ScanResult r) {
  _btscan1(r);
  BluetoothData btd = BluetoothData(r.device.id.id, r.rssi, r.device.name);
  bl.add(btd);
  return bl;
}
