import 'package:flutter/material.dart';
import 'selectpage.dart';
import 'storage.dart' as storage;
import 'globals.dart' as globals;
import 'recheck.dart' as recheck;
import 'device.dart' as device;
import 'util.dart' as util;
import "locator.dart";
import 'dart:async';

void main() {
  initialize();
  runApp(
    const MaterialApp(
      title: "ALDS Location Selector",
      home: AldsSelect(),
    ),
  );
}

void initialize() async {
  await util.setup();
  await storage.setupStorage();
  await recheck.initialize();
  Locator loc = Locator();
  loc.setup();

  Timer.periodic(
      const Duration(seconds: globals.recheckEverySeconds), _handleRecheck);
  Timer.periodic(
      const Duration(seconds: globals.pingEverySeconds), _handleDevice);
}

void _handleRecheck(Timer timer) async {
  await recheck.recheck();
}

void _handleDevice(Timer timer) async {
  device.Cedes cedes = device.Cedes();
  await cedes.ping();
}
