import 'package:flutter/material.dart';
import 'selectpage.dart';
import 'storage.dart' as storage;
import 'globals.dart' as globals;
import 'recheck.dart' as recheck;
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
  await storage.setupStorage();
  await recheck.initialize();
  Timer.periodic(
      const Duration(seconds: globals.recheckEverySeconds), _handleRecheck);
}

void _handleRecheck(Timer timer) async {
  await recheck.recheck();
}
