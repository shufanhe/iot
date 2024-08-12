/*
 *        util.dart
 * 
 *    Utility methods
 * 
 */
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

library sherpa.util;

import 'dart:convert' as convert;
import 'package:crypto/crypto.dart' as crypto;
import 'package:day_picker/day_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:logging/logging.dart';
import 'package:intl/intl.dart';
import 'package:sherpa/globals.dart';

String hasher(String msg) {
  final bytes = convert.utf8.encode(msg);
  crypto.Digest rslt = crypto.sha512.convert(bytes);
  String srslt = convert.base64.encode(rslt.bytes);
  return srslt;
}

bool validateEmail(String email) {
  const res =
      r'^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$';
  final regExp = RegExp(res);
  if (!regExp.hasMatch(email)) return false;
  return true;
}

bool validatePassword(String? pwd) {
  if (pwd == null || pwd == '') return false;
  // check length, contents
  return true;
}

///
///     Logging methods
///

Logger? _sherpaLog;

void setupLogging() {
  Logger.root.level = Level.ALL;
  Logger.root.onRecord.listen((record) {
    print('${record.level.name}: ${record.time}: ${record.message}');
  });
  _sherpaLog = Logger('SHERPA');
}

void logI(String msg) {
  _sherpaLog?.info(msg);
}

void logD(String msg) {
  _sherpaLog?.fine(msg);
}

void logE(String msg) {
  _sherpaLog?.severe(msg);
}

void flushLogs() {}

List<T> skipNulls<T>(List<T?> items) {
  List<T> rslt = [];
  for (T? t in items) {
    if (t != null) rslt.add(t);
  }
  return rslt;
}

List<DayInWeek> getDays({String? given}) {
  List<DayInWeek> rslt = [];
  List<String> std = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  DateFormat fmt = DateFormat.E();
  DateTime dt = DateTime.now();
  while (dt.weekday != 1) {
    dt = dt.add(const Duration(days: 1));
  }
  for (int i = 0; i < 7; ++i) {
    String name = fmt.format(dt);
    DayInWeek diw = DayInWeek(name, dayKey: name);
    if (given != null && given.contains(std[i])) {
      diw.isSelected = true;
    }
    rslt.add(diw);
    dt = dt.add(const Duration(days: 1));
  }
  return rslt;
}

List<String> mapDays(List<String> days) {
  List<String> rslt = [];
  List<String> std = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  DateFormat fmt = DateFormat.E();
  DateTime dt = DateTime.now();
  while (dt.weekday != 1) {
    dt = dt.add(const Duration(days: 1));
  }
  for (int i = 0; i < 7; ++i) {
    String name = fmt.format(dt);
    if (days.contains(name)) {
      rslt.add(std[i]);
    }
    dt = dt.add(const Duration(days: 1));
  }

  return rslt;
}

String getServerURL() {
  if (kDebugMode) {
    return catreURL;
  }
  return catreURL;
}

class RepeatOption {
  String name;
  int value;

  RepeatOption(this.name, this.value);
}

List<RepeatOption> getRepeatOptions() {
  List<RepeatOption> rslt = [];
  rslt.add(RepeatOption("No Repeat", 0));
  rslt.add(RepeatOption("Every Week", 7));
  rslt.add(RepeatOption("Every 2 Weeks", 14));
  rslt.add(RepeatOption("Every 3 Weeks", 21));
  rslt.add(RepeatOption("Every 4 Weeks", 28));
  rslt.add(RepeatOption("Monthly", -1));
  return rslt;
}

int getIntValue(dynamic value, num dflt) {
  value ??= dflt;
  if (value is String) {
    String s = value;
    try {
      value = double.parse(s);
      value = value.toInt();
      // ignore: empty_catches
    } catch (e) {}
  }
  if (value is! num) {
    value = dflt;
  }
  return value.toInt();
}

double getDoubleValue(dynamic value, num dflt) {
  value ??= dflt;
  if (value is String) {
    String s = value;
    try {
      value = double.parse(s);
      // ignore: empty_catches
    } catch (e) {}
  }
  if (value is! double && value is! int) {
    value = dflt;
  }
  return value.toDouble();
}

