/*
 *      storage.dart
 * 
 *    Persistent storage for ALDS
 * 
 */

library alds.storage;

import 'package:hive_flutter/hive_flutter.dart';

import 'util.dart' as util;

AuthData _authData = AuthData('*', "*");
List<String> _locations = defaultLocations;
String _deviceId = "*";

const List<String> defaultLocations = [
  'Office',
  'Home',
  'Dining',
  'Meeting',
  'Class',
  'Driving',
  'Gym',
  'Bed',
  'Shopping',
  'Home Office',
  'Other',
];

class AuthData {
  late String userId;
  late String userPass;

  AuthData(this.userId, this.userPass);
}

Future<void> setupStorage() async {
  await Hive.initFlutter();
  var appbox = await Hive.openBox('appData');
  // appbox.clear(); // REMOVE IN PRODUCTION
  bool setup = await appbox.get("setup", defaultValue: false);
  String uid = await appbox.get("userid", defaultValue: util.randomString(12));
  String upa =
      await appbox.get("userpass", defaultValue: util.randomString(16));
  _authData = AuthData(uid, upa);
  _locations = appbox.get("locations", defaultValue: defaultLocations);
  _deviceId =
      appbox.get("deviceid", defaultValue: "ALDS_${util.randomString(20)}");
  if (!setup) {
    await saveData();
  }
  // util.log("LOGIN DATA: $uid $upa");
}

Future<void> saveData() async {
  var appbox = Hive.box('appData');
  await appbox.put('setup', true);
  await appbox.put('userid', _authData.userId);
  await appbox.put('userpass', _authData.userPass);
  await appbox.put('locations', _locations);
  await appbox.put('deviceid', _deviceId);
}

AuthData getAuthData() {
  return _authData;
}

List<String> getLocations() {
  return _locations;
}

String getDeviceId() {
  return _deviceId;
}

Future<void> saveLocatorData(String json) async {
  var appbox = Hive.box('appData');
  await appbox.put("locdata", json);
}

Future<String?> readLocationData() async {
  var appbox = Hive.box('appData');
  return await appbox.get('locdata');
}
