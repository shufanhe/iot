/*
 *        triggertime.dart
 * 
 *    Representation of a time trigger
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

import 'package:sherpa/stringtokenizer.dart';

/// *****
///      CatreTriggerTime -- Time triggers
/// *****

class CatreTriggerTime {
  Set<num> minutes = {};
  Set<num> hours = {};
  Set<num> days = {};
  Set<num> months = {};
  Set<num> weekdays = {};
  final String _stringRep;

  static final _valueMap = <String, int>{
    "SUN": 0,
    "MON": 1,
    "TUE": 2,
    "WED": 3,
    "THU": 4,
    "FRI": 5,
    "SAT": 6,
    "JAN": 1,
    "FEB": 2,
    "MAR": 3,
    "APR": 4,
    "MAY": 5,
    "JUN": 6,
    "JUL": 7,
    "AUG": 8,
    "SEP": 9,
    "OCT": 10,
    "NOV": 11,
    "DEC": 12,
  };

  CatreTriggerTime(this._stringRep) {
    List<String> items = _stringRep.split(' ');
    minutes = _decodeSet(items[0], 0, 59);
    hours = _decodeSet(items[1], 0, 23);
    days = _decodeSet(items[2], 1, 31);
    months = _decodeSet(items[3], 1, 12);
    weekdays = _decodeSet(items[4], 0, 7);
    if (weekdays.contains(7)) {
      weekdays.add(0);
      weekdays.remove(7);
    }
  }

  Set<num> _decodeSet(String what, int min, int max) {
    Set<num> rslt = {};
    if (what == "" || what == "*") {
      rslt = _addItems(rslt, min, max + 1);
    } else {
      StringTokenizer tok = StringTokenizer(what, ',-/', true);
      int last = min;
      int from = -1;
      int to = -1;
      String? next;
      while (tok.hasMoreTokens()) {
        String t = next ?? tok.nextToken();
        next = null;
        if (t == ',') {
          from = -1;
        } else if (t == '-') {
          from = last;
        } else if (isNumeric(t)) {
          last = int.parse(t);
          if (from < 0) {
            rslt.add(last);
          } else if (to > 0) {
            int d = last;
            last = from;
            for (int i = from; i <= to; i += d) {
              rslt.add(i);
              last = i;
            }
            from = to = -1;
          } else {
            if (tok.hasMoreTokens()) next = tok.nextToken();
            if (next != null && next == '/') {
              to = last;
              continue;
            }
            rslt = _addItems(rslt, from, last + 1);
          }
          from = -1;
        } else {
          if (t.length > 3) t = t.substring(0, 3);
          t = t.toUpperCase();
          int? v = _valueMap[t];
          if (v != null) rslt.add(v);
        }
      }
    }
    return rslt;
  }

  Set<num> _addItems(Set<num> rslt, int f, int t, [int delta = 1]) {
    for (int i = f; i < t; i += delta) {
      rslt.add(i);
    }
    return rslt;
  }

  bool isNumeric(String s) {
    return int.tryParse(s) != null;
  }
}
