/*
 *        catreprogram.dart
 * 
 *    Dart representation of a CATRE program w/ conditions and actions
 * 
 **/
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
///*******************************************************************************/

import 'catredata.dart';
import 'catreuniverse.dart';
import 'triggertime.dart';
import 'catredevice.dart';
import 'package:sherpa/levels.dart';

/// *****
///      CatreProgram : the current user program
/// *****

class CatreProgram extends CatreData {
  late List<CatreRule> _theRules;

  CatreProgram.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>) {
    _theRules = buildList("RULES", CatreRule.build);
  }

  List<CatreRule> getRules() => _theRules;

  List<CatreRule> getSelectedRules(PriorityLevel? lvl, CatreDevice? dev) {
    List<CatreRule> rslt = [];
    for (CatreRule cr in _theRules) {
      if (lvl != null) {
        if (cr.getPriority() < lvl.lowPriority) continue;
        if (cr.getPriority() >= lvl.highPriority) continue;
      }
      if (dev != null && cr.getDevice() != dev) continue;
      rslt.add(cr);
    }
    return rslt;
  }
} // end of CatreProgram

/// *****
///      CatreRule : description of a single rule
/// *****

class CatreRule extends CatreData {
  late CatreCondition _condition;
  late List<CatreAction> _actions;

  CatreRule.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>) {
    _condition = buildItem("CONDITION", CatreCondition.build);
    _actions = buildList("ACTIONS", CatreAction.build);
  }

  num getPriority() => getNum("PRIORITY");
  bool isTrigger() => _condition.isTrigger();

  CatreCondition getCondition() => _condition;
  List<CatreCondition> getAndConditions() {
    List<CatreCondition> rslt = [];
    _addConditions(_condition, rslt);
    return rslt;
  }

  void _addConditions(CatreCondition cc, List<CatreCondition> rslt) {
    if (cc.getCatreType() != 'AND') {
      rslt.add(cc);
    } else {
      for (CatreCondition scc in cc.getSubConditions()) {
        _addConditions(scc, rslt);
      }
    }
  }

  void setCondition(CatreCondition cc) {
    if (setField("CONDITION", cc)) {
      _condition = cc;
    }
  }

  void setAndConditions(List<CatreCondition> ccs) {
    if (ccs.length == 1) {
      setCondition(ccs[0]);
    } else {
      // build and condition
      // then set condition that
    }
  }

  List<CatreAction> getActions() => _actions;

  void setActions(List<CatreAction> acts) {
    if (setListField("ACTIONS", acts)) {
      _actions = acts;
    }
  }

  CatreDevice? getDevice() {
    for (CatreAction ca in _actions) {
      CatreDevice? cd = ca.getTransitionRef().getDevice();
      if (cd != null) return cd;
    }
    return null;
  }
}

/// *****
///      CatreCondition : description of condition of a rule
/// *****

class CatreCondition extends CatreData {
  CatreParamRef? _paramRef;
  CatreCondition? _subCondition;
  List<CatreCondition>? _subConditions;
  CatreTimeSlot? _timeSlot;
  CatreTriggerTime? _triggerTime;
  List<CatreCalendarMatch>? _calendarFields;
  late CatreConditionType _conditionType;

  CatreCondition.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>) {
    setup();
  }

  @override
  void setup() {
    _paramRef = optItem("PARAMREF", CatreParamRef.build);
    _subCondition = optItem("CONDITION", CatreCondition.build);
    _subConditions = optList("CONDITIONS", CatreCondition.build);
    _timeSlot = optItem("EVENT", CatreTimeSlot.build);
    String? t = optString("TIME");
    if (t != null) _triggerTime = CatreTriggerTime(t);
    _calendarFields = optList("FIELDS", CatreCalendarMatch.build);

    String typ = getString("TYPE");
    bool trig = getBool("TRIGGER");
    List<CatreConditionType> ctyps =
        (trig ? triggerConditionTypes : ruleConditionTypes);
    _conditionType = ctyps[0];
    for (CatreConditionType ct in ctyps) {
      if (ct._catreType == typ && ct._isTrigger == trig) {
        _conditionType = ct;
        break;
      }
    }
  }

  CatreConditionType getConditionType() => _conditionType;
  String getCatreType() => getString("TYPE");
  bool isTrigger() => getBool("TRIGGER");
  CatreCondition getSubcondition() => _subCondition as CatreCondition;
  List<CatreCondition> getSubConditions() =>
      _subConditions as List<CatreCondition>;

// Parameter conditions
//      isTrigger

  String getOperator() => getString("OPERATOR");
  String getTargetValue() => getString("STATE");
  CatreParamRef getParameterReference() => _paramRef as CatreParamRef;

// Disable conditions
  String getDeviceId() => getString("DEVICE");
  bool isCheckForEnabled() => getBool("ENABLED");

// Debounce conditions
//        getSubCondition
  num getOnTime() => getNum("ONTIME");
  num getOffTime() => getNum("OFFTIME");

// Duration conditions
//    getSubcondition(), isTrigger
  num getMinTime() => getNum("MINTIME");
  num getMaxTime() => getNum("MAXTIME");

// Latch conditions
//    getSubcondition
  num? getResetTime() => optNum("RESETTIME");
  num getResetAfter() => getNum("RESETAFTER");
  num getOffAfter() => getNum("OFFAFTER");

// Range conditions
//    getParameterReference, isTrigger
  num? getLowValue() => optNum("LOW");
  num? getHighValue() => optNum("HIGH");

// Timer conditions
  CatreTimeSlot getTimeSlot() => _timeSlot as CatreTimeSlot;

// TriggerTime conditions
  CatreTriggerTime getTriggerTime() => _triggerTime as CatreTriggerTime;

// CalendarEvent conditions
//      getParameterReference
  List<CatreCalendarMatch> getFields() =>
      _calendarFields as List<CatreCalendarMatch>;
}

class CatreConditionType {
  final String label;
  final String _catreType;
  final bool _isTrigger;

  const CatreConditionType(this.label, this._catreType, this._isTrigger);

  bool isTrigger() => _isTrigger;

  String get name {
    String s = _catreType;
    if (_isTrigger) s += "_T";
    return s;
  }
}

const List<CatreConditionType> ruleConditionTypes = [
  CatreConditionType("Parameter", "Parameter", false),
  CatreConditionType("Time Period", "Time", false),
  CatreConditionType("Duration", "Duration", false),
  CatreConditionType("Latched", "Latch", false),
  CatreConditionType("Parameter Range", "Range", false),
  CatreConditionType("Calendar Event", "CalendarEvent", false),
  CatreConditionType("Device Enabled/Disabled", "Disabled", false),
  CatreConditionType("Parameter Stable", "Debounce", false),
];

const List<CatreConditionType> triggerConditionTypes = [
  CatreConditionType("Trigger on Parameter", "Parameter", true),
  CatreConditionType("Trigger at Time", "TriggerTime", true),
  CatreConditionType("Trigger After Duration", "Duration", true),
  CatreConditionType("Trigger on Parameter Range", "Range", true),
  CatreConditionType("Trigger on Parameter Stable", "Debounce", true),
];

/// *****
///      CatreAction : description of action of a rule
/// *****

class CatreAction extends CatreData {
  late CatreTransitionRef _transition;

  CatreAction.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>) {
    _transition = buildItem("TRANSITION", CatreTransitionRef.build);
  }

  CatreTransitionRef getTransitionRef() => _transition;
  CatreDevice getDevice() => _transition.getDevice() as CatreDevice;
  CatreTransition getTransition() => _transition.getTransition() as CatreTransition;

  bool isValid() => _transition.getTransition() != null;

  Map<String, dynamic> getValues() {
    return catreData["PARAMETERS"] as Map<String, dynamic>;
  }
}

/// *****
///      CatreParamRef : description of reference to a parameter
/// *****

class CatreParamRef extends CatreData {
  CatreParamRef.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>);

  String getDeviceId() => getString("DEVICE");
  String getParameterName() => getString("PARAMETER");
}

/// *****
///      CatreTransitionRef : reference to a transition
/// *****

class CatreTransitionRef extends CatreData {
  CatreTransitionRef.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>);

  String getDeviceId() => getString("DEVICE");
  String getTransitionName() => getString("TRANSITION");

  CatreDevice? getDevice() {
    return catreUniverse.findDevice(getDeviceId());
  }

  CatreTransition? getTransition() {
    CatreDevice? cd = getDevice();
    if (cd == null) return null;
    return cd.findTransition(getTransitionName());
  }
}

/// *****
///      CatreTimeSlot :: desription of a time slot for a condition
/// *****

class CatreTimeSlot extends CatreData {
  CatreTimeSlot.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>);

  num? getFromDate() => optNum("FROMDATE");
  num? getFromTime() => optNum("FROMTIME");
  num? getToDate() => optNum("TODATE");
  num? getToTime() => optNum("TOTIME");
  String getDays() => getString("DAYS");
  num getRepeatInterval() => getNum("INTERVAL");
  List<num>? getExcludeDates() => optNumList("EXCLUDE");
}

/// *****
///      CatreCalendarMatch -- description of a field match for calendar
/// *****

class CatreCalendarMatch extends CatreData {
  CatreCalendarMatch.build(CatreUniverse cu, dynamic data)
      : super(cu, data as Map<String, dynamic>);

  String getFieldName() => getString("NAME");
  String getNullType() => getString("NULL");
  String getMatchType() => getString("MATCH");
  String? getMatchValue() => optString("MATCHVALUE");
}
