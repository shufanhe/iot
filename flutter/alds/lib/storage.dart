/*
 *      storage.dart
 * 
 *    Persistent storage for ALDS
 * 
 */

library alds.storage;

import 'package:hive_flutter/hive_flutter.dart';

import 'util.dart' as util;

AuthData _authData = AuthData('X', "Y");
List<String> _locations = defaultLocations;

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
  bool setup = appbox.get("setup", defaultValue: false);
  String uid = appbox.get("userid", defaultValue: util.randomString(12));
  String upa = appbox.get("userpass", defaultValue: util.randomString(16));
  _authData = AuthData(uid, upa);
  _locations = appbox.get("locations") ?? defaultLocations;
  if (!setup) {
    await saveData();
  }
  print("LOGIN DATA: $uid $upa");
}

Future<void> saveData() async {
  var appbox = Hive.box('appData');
  await appbox.put('setup', true);
  await appbox.put('userid', _authData.userId);
  await appbox.put('userpass', _authData.userPass);
  await appbox.put('locations', _locations);
}

AuthData getAuthData() {
  return _authData;
}

List<String> getLocations() {
  return _locations;
}
