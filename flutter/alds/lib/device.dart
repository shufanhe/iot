/*
 *      device.dart
 * 
 *    Talk to CATRE as a device
 * 
 */

import 'storage.dart' as storage;
import 'dart:convert' as convert;
import 'util.dart' as util;
import 'globals.dart' as globals;
import 'package:http/http.dart' as http;
import 'package:mutex/mutex.dart';
import 'locator.dart';

const String noAuth = '*';

class Cedes {
  static final Cedes _cedes = Cedes._internal();

  factory Cedes() {
    return _cedes;
  }
  Cedes._internal();

  String _authCode = noAuth;
  final _doingPing = Mutex();
  DateTime _nextTime = DateTime.fromMillisecondsSinceEpoch(0);
  String? _lastLoc;

  Future<void> ping() async {
    await _doingPing.acquire();
    try {
      if (_authCode == noAuth) {
        if (DateTime.now().isAfter(_nextTime)) {
          await _setup();
        } else
          return;
      }
      if (_authCode != noAuth) {
        storage.AuthData ad = storage.getAuthData();
        Map<String, dynamic>? resp =
            await _sendToCedes('ping', {"uid": ad.userId});
        String sts = "FAIL";
        if (resp != null) sts = resp["status"] ?? "FAIL";
        switch (sts) {
          case "DEVICES":
            await _sendDeviceInfo();
            break;
          case "COMMAND":
            break;
          case "OK":
            break;
          default:
            _authCode = noAuth;
            break;
        }
      }
      _nextTime = DateTime.now()
          .add(const Duration(seconds: globals.accessEverySeconds));
    } finally {
      _doingPing.release();
    }
    if (_authCode != noAuth && _lastLoc == null) {
      Locator loc = Locator();
      String? lloc = loc.lastLocation;
      if (lloc != null) updateLocation(lloc);
    }
  }

  Future<void> updateLocation(String loc) async {
    if (_authCode == noAuth) return;
    await _doingPing.acquire();
    try {
      await _sendToCedes("event", {
        "event": {
          "DEVICE": storage.getDeviceId(),
          "TYPE": "PARAMETER",
          "PARAMETER": "Location",
          "VALUE": loc
        }
      });
      _lastLoc = loc;
    } finally {
      _doingPing.release();
    }
  }

  Future<void> _sendDeviceInfo() async {
    storage.AuthData ad = storage.getAuthData();
    Map<String, dynamic> dev = {};
    dev["UID"] = storage.getDeviceId();
    dev["NAME"] = "ALDS_${ad.userId}";
    dev["LABEL"] = "ALDS locator for ${ad.userId}";
    dev["BRIDGE"] = "generic";
    Map<String, dynamic> p0 = {
      "NAME": "Location",
      "ISSENSOR": true,
      "ISTARGET": false,
      "TYPE": "STRING"
    };
    dev["TRANSITIONS"] = [];
    dev["PARAMETERS"] = [p0];
    await _sendToCedes("devices", {
      "devices": [dev]
    });
  }

  Future<void> _setup() async {
    if (_authCode != noAuth) return;
    storage.AuthData ad = storage.getAuthData();
    if (ad.userId == '*' || ad.userPass == '*') return;
    Map<String, dynamic>? attach =
        await _sendToCedes("attach", {"uid": ad.userId});
    if (attach == null) return;
    String seed = attach["seed"] ?? "*";
    if (seed == '*') return;
    String p0 = util.hasher(ad.userPass);
    String p1 = util.hasher(p0 + ad.userId);
    String p2 = util.hasher(p1 + seed);
    Map<String, dynamic>? auth =
        await _sendToCedes("authorize", {"uid": ad.userId, "patencoded": p2});
    if (auth == null) return;
    _authCode = auth["token"] ?? noAuth;
  }

  Future<Map<String, dynamic>?> _sendToCedes(String cmd, dynamic data) async {
    var url = Uri.https('sherpa.cs.brown.edu:3333', '/generic/$cmd');
    Map<String, String> headers = {};
    if (_authCode != noAuth) {
      headers['Authorization'] = "Bearer $_authCode";
    }
    headers['content-type'] = 'application/json';
    headers['accept'] = 'application/json';
    var hdata = convert.jsonEncode(data);
    var resp = await http.post(url, body: hdata, headers: headers);
    if (resp.statusCode >= 400) return null;
    var dresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    return dresp;
  }
}
