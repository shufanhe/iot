/*
 *      locator.dart
 * 
 *    Code to store/update current location information
 * 
 */

import 'package:geolocator/geolocator.dart';

class Locator {
  LocationData? _curLocationData;

  static final Locator _locator = Locator._internal();

  factory Locator() {
    return _locator;
  }

  void updateLocation(Position? pos, List<BluetoothData> btdata) {
    LocationData nloc = LocationData(pos, btdata);
    if (_curLocationData != null) {
      LocationData? rloc = _curLocationData?.update(pos, btdata);
      if (rloc == nloc) return;
    }
    // output _curLocation if not null
    // eventually -- compute new location based on nloc
    _curLocationData = nloc;
  }

  void noteLocation(String loc) {
    // output loc as current location validated by user
    // eventually -- update location finder based on loc
  }

  Locator._internal();
}

class LocationData {
  Map<String, BluetoothData> _bluetoothData = {};
  Position? _gpsPosition;
  int _count = 1;

  LocationData(this._gpsPosition, List<BluetoothData> bts) {
    _bluetoothData = {for (BluetoothData bt in bts) bt._id: bt};
  }

  LocationData? update(Position? pos, List<BluetoothData> btdata) {
    int ct = 0;
    for (BluetoothData bd in btdata) {
      if (bd._rssi == 127) continue;
      BluetoothData? match = _bluetoothData[bd._id];
      if (match == null) return null; // new bluetooth item
      int delta = (match._rssi - bd._rssi).abs();
      if (delta > 4) return null;
      ++ct;
    }
    if (ct != _bluetoothData.length) return null;
    Position? gpos = _gpsPosition;
    if (pos != null && gpos != null) {
      if (_gpsPosition != null) {
        double d1 = (pos.latitude - gpos.latitude).abs();
        if (d1 > pos.accuracy / 2) return null;
        double d2 = (pos.longitude - gpos.longitude).abs();
        if (d2 > pos.accuracy / 2) return null;
        double d3 = (pos.altitude - gpos.altitude).abs();
        if (d3 > pos.accuracy / 4) return null;
        double d4 = (pos.speed - gpos.speed).abs();
        if (d4 > pos.speedAccuracy / 2) return null;
      }
    }
    // here we need to merge new values with old
    _count++;
    return this;
  }
}

class BluetoothData {
  String _id;
  int _rssi;
  String _name;

  BluetoothData(this._id, this._rssi, this._name);
}
