/*
 *        catreprogram.dart
 *
 *    Dart representation of a CATRE program w/ conditions and actions
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

import 'catredata.dart';
import 'catreuniverse.dart';
import 'triggertime.dart';
import 'catredevice.dart';
import 'catreparameter.dart';
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/levels.dart';
import 'package:flutter/material.dart';

/// *****
///      CatreProgram : the current user program
/// *****

class CatreProgram extends CatreData {
  late List<CatreRule> _theRules;
  final Map<String, CatreCondition> _sharedConditions = {};

  CatreProgram.build(CatreUniverse cu, dynamic data)
      : super(
          cu,
          data as Map<String, dynamic>,
        ) {
    _theRules = buildList("RULES", CatreRule.build);
    List<CatreCondition> sc = buildList("SHARED", CatreCondition.build);
    for (CatreCondition cc in sc) {
      _sharedConditions[cc.getName()] = cc;
    }
  }

  @override
  Map<String, dynamic> getCatreOutput() {
    setField("RULES", listCatreOutput(_theRules));
    setField("SHARED", listCatreOutput(_sharedConditions.values));
    return super.getCatreOutput();
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

  CatreRule addRule(CatreDevice cd, num priority, bool trigger) {
    CatreRule cr = CatreRule.create(catreUniverse, cd, priority, trigger);
    _insertRule(cr);
    return cr;
  }

  void _insertRule(CatreRule cr) {
    num priority = cr.getPriority();
    int index = 0;
    for (CatreRule cr in _theRules) {
      if (cr.getPriority() > priority) {
        break;
      }
      ++index;
    }
    _theRules.insert(index, cr);
  }

  void reorderRules() {
    _theRules.sort(_ruleSorter);
  }

  int _ruleSorter(CatreRule r1, CatreRule r2) {
    return r1.getPriority().compareTo(r2.getPriority());
  }

  void removeRule(CatreRule cr) {
    _theRules.remove(cr);
  }

  Map<String, CatreCondition> getSharedConditions() {
    return _sharedConditions;
  }

  void setRulePriority(CatreRule cr, num priority) {
    _theRules.remove(cr);
    cr.setPriority(priority);
    _insertRule(cr);
  }

  void shareCondition(CatreCondition cc) {
    CatreCondition? xc = _sharedConditions[cc.getName()];
    // if (xc != null && xc != cc) return false;
    xc?._setShared(false);
    cc._setShared(true);
    _sharedConditions[cc.getName()] = cc;
    cc.issueCommand("/universe/shareCondition", "CONDITION");
  }
} // end of CatreProgram

/// *****
///      CatreRule : description of a single rule
/// *****

class CatreRule extends CatreData {
  late List<CatreCondition> _conditions;
  late List<CatreAction> _actions;
  late CatreDevice _forDevice;
  bool _forceTrigger = false;

  CatreRule.build(CatreUniverse cu, dynamic data) : super(cu, data as Map<String, dynamic>) {
    setup();
  }

  CatreRule.create(CatreUniverse cu, CatreDevice cd, num priority, bool trigger)
      : super(cu, <String, dynamic>{
          "PRIORITY": priority,
          "LABEL": "Undefined",
          "DESCRIPTION": "Undefined",
          "USERDESC": false,
          "CONDITIONS": [],
          "ACTIONS": [],
          "TRIGGER": trigger,
        }) {
    _conditions = [CatreCondition.empty(cu, trigger)];
    _forceTrigger = trigger;
    _actions = [CatreAction.empty(cu, cd, trigger)];
    _forDevice = cd;
  }

  @override
  void setup() {
    _conditions = buildList("CONDITIONS", CatreCondition.build);
    _actions = buildList("ACTIONS", CatreAction.build);
    _forceTrigger = getBool("TRIGGER");
    for (CatreAction ca in _actions) {
      _forDevice = ca.getTransitionRef().getDevice();
    }
  }

  @override
  Map<String, dynamic> getCatreOutput() {
    setField("CONDITIONS", listCatreOutput(_conditions));
    setField("ACTIONS", listCatreOutput(_actions));
    setField("DEVICEID", _forDevice.getDeviceId());
    return super.getCatreOutput();
  }

  num getPriority() => getNum("PRIORITY");
  bool isTrigger() => _forceTrigger;
  CatreDevice getDevice() => _forDevice;

  List<CatreCondition> getConditions() => _conditions;

  CatreCondition addNewCondition() {
    bool trigger = false;
    if (_conditions.isEmpty && _forceTrigger) trigger = true;
    CatreCondition cc = CatreCondition.empty(catreUniverse, trigger);
    _conditions.add(cc);
    return cc;
  }

  void removeCondition(CatreCondition cc) {
    _conditions.remove(cc);
  }

  void setAndConditions(List<CatreCondition> ccs) {
    _conditions = ccs;
  }

  void setPriority(num p) {
    setField("PRIORITY", p);
  }

  List<CatreAction> getActions() => _actions;

  void setActions(List<CatreAction> acts) {
    if (setListField("ACTIONS", acts)) {
      _actions = acts;
    }
  }

  CatreAction addNewAction(CatreDevice cd) {
    CatreAction ca = CatreAction.empty(catreUniverse, cd, _forceTrigger);
    _actions.add(ca);
    return ca;
  }

  void removeAction(CatreAction ca) {
    _actions.remove(ca);
  }

  Future<void> addOrEditRule() async {
    String? rid = optString("RULEID");
    Map<String, dynamic>? data;
    if (rid == null) {
      data = await issueCommand("/rule/add", "RULE");
    } else {
      data = await issueCommand("/rule/$rid/edit", "RULE");
    }
    if (data != null && data["STATUS"] == "OK") {
      Map<String, dynamic>? rule = data["RULE"];
      if (rule != null) {
        rebuild(rule);
      }
    }
  }

  @override
  String buildDescription() {
    String desc = "WHEN\n";
    for (int i = 0; i < _conditions.length; ++i) {
      desc += "   ${_conditions[i].getDescription()}";
      if (i < _conditions.length - 1) desc += " AND\n";
    }
    desc += "\nDO\n";
    for (int i = 0; i < _actions.length; ++i) {
      desc += "   ${_actions[i].getDescription()}";
      if (i < _actions.length - 1) desc += " AND\n";
    }
    return desc;
  }
}

/// *****
///      CatreCondition : description of condition of a rule
/// *****

class CatreCondition extends CatreData {
  CatreParamRef? _paramRef;
  CatreCondition? _subCondition;
  CatreTimeSlot? _timeSlot;
  CatreTriggerTime? _triggerTime;
  List<CatreCalendarMatch>? _calendarFields;
  late CatreConditionType _conditionType;

  CatreCondition.build(CatreUniverse cu, dynamic data) : super(cu, data as Map<String, dynamic>) {
    setup();
  }

  CatreCondition.parameter(CatreUniverse cu, bool trig)
      : super(cu, <String, dynamic>{
          "TRIGGER": trig,
          "TYPE": "Parameter",
          "OPERATOR": "EQL",
          "STATE": "UNKNOWN",
          "SHARED": false,
        }) {
    setup();
    _paramRef = CatreParamRef.create(cu);
  }

  CatreCondition.empty(CatreUniverse cu, bool trig)
      : super(cu, <String, dynamic>{
          "TRIGGER": trig,
          "TYPE": "UNDEFINED",
          "LABEL": "Undefined",
          "NAME": "Undefined",
          "DESCRIPTION": "Undefined",
          "SHARED": false,
        }) {
    setup();
  }

  CatreCondition.clone(CatreCondition super.base) : super.clone() {
    setup();
  }

  CatreCondition cloneCondition() {
    CatreCondition base = this;
    while (base.getCatreType() == 'Reference') {
      base = base.getSubcondition();
    }
    return CatreCondition.clone(base);
  }

  @override
  void setup() {
    _paramRef = optItem("PARAMREF", CatreParamRef.build);
    _subCondition = optItem("CONDITION", CatreCondition.build);
    _timeSlot = optItem("EVENT", CatreTimeSlot.build);
    String? t = optString("TIME");
    if (t != null) _triggerTime = CatreTriggerTime(t);
    _calendarFields = optList("FIELDS", CatreCalendarMatch.build);

    String typ = getString("TYPE");
    bool trig = getBool("TRIGGER");
    List<CatreConditionType> ctyps = (trig ? triggerConditionTypes : ruleConditionTypes);
    _conditionType = ctyps[0];
    for (CatreConditionType ct in ctyps) {
      if (ct._catreType == typ && ct._isTrigger == trig) {
        _conditionType = ct;
        break;
      }
    }
  }

  @override
  Map<String, dynamic> getCatreOutput() {
    String typ = getCatreType();
    if (typ != "Parameter") {
      _paramRef = null;
    }
    if (typ != "Time" && typ != "TriggerTime") {
      _timeSlot = null;
    }
    if (typ != "CalendarEvent") {
      _calendarFields = null;
    }

    if (_timeSlot != null) {
      setField("EVENT", _timeSlot?.getCatreOutput());
    }
    if (_calendarFields != null) {
      setField("FIELDS", listCatreOutput(_calendarFields));
    }
    if (_paramRef != null) {
      setField("PARAMREF", _paramRef?.getCatreOutput());
    }
    return super.getCatreOutput();
  }

  CatreConditionType getConditionType() => _conditionType;
  String getCatreType() => getString("TYPE");
  bool isTrigger() => getBool("TRIGGER");
  CatreCondition getSubcondition() => _subCondition as CatreCondition;
  CatreCondition? optSubcondition() => _subCondition;
  bool isShared() => getBool("SHARED");

  void setConditionType(CatreConditionType ct) {
    _conditionType = ct;
    setField("TYPE", ct._catreType);
    _setDefaultFields();
  }

  void _setDefaultFields() {
    switch (getCatreType()) {
      case "Parameter":
        _paramRef ??= CatreParamRef.create(catreUniverse);
        defaultField("OPERATOR", isTrigger() ? "GEQ" : "EQL");
        defaultField("STATE", "UNKNOWN");
        break;
      case "Disabled":
        // defaultField("DEVICE", "Unknown");
        defaultField("ENABLED", true);
        break;
      case "Debounce":
        _subCondition ??= CatreCondition.parameter(catreUniverse, false);
        defaultField("ONTIME", 10);
        defaultField("OFFTIME", 10);
        break;
      case "Duration":
        _subCondition ??= CatreCondition.parameter(catreUniverse, false);
        defaultField("MINTIME", 0);
        defaultField("MAXTIME", 10);
        break;
      case "Latch":
        _subCondition ??= CatreCondition.parameter(catreUniverse, false);
        defaultField("OFFAFTER", 0);
        defaultField("RESETAFTER", 0);
        break;
      case "Reference":
        // String? refnm = optString("SHAREDNAME");
        // if (refnm == null) {
        //   for (CatreCondition cc in getUniverse().getSharedConditions().values) {
        //     _subCondition ??= cc;
        //     defaultField("SHAREDNAME", cc.getName());
        //     break;
        //   }
        // }
        break;
      case "Range":
        defaultField("LOW", 0);
        defaultField("HIGH", 100);
        break;
      case "Time":
        break;
      case "TriggerTime":
        break;
      case "CalendarEvent":
        break;
    }
  }

  @override
  String buildDescription() {
    String rslt = getDescription();
    switch (getCatreType()) {
      case "Parameter":
        CatreParamRef pmf = getParameterReference();
        rslt = "${pmf.getTitle()}\n\t${getOperator()}\n${getTargetValue()}";
        break;
      case "Disabled":
        CatreDevice cd = catreUniverse.findDevice(getDeviceId() as String);
        rslt = cd.getLabel();
        rslt += " is ";
        rslt += (isCheckForEnabled() ? "ENABLED" : "DISABLED");
        break;
      case "Debounce":
        rslt = getSubcondition().buildDescription();
        rslt += "\n\tIS STABLE";
        break;
      case "Duration":
        rslt = getSubcondition().buildDescription();
        num mt = getMinTime() / 1000 / 60;
        num xt = getMaxTime() / 1000 / 60;
        rslt += "\n\tON FOR $mt TO $xt MINUTES";
        break;
      case "Latch":
        rslt = getSubcondition().buildDescription();
        break;
      case "Reference":
        rslt = "Reference to ${getSharedName()}";
        break;
      case "Range":
        CatreParamRef pmf = getParameterReference();
        rslt = "${pmf.getTitle()} between ${getLowValue()} and ${getHighValue()}}";
        break;
      case "Time":
        break;
      case "TriggerTime":
        break;
      case "CalendarEvent":
        break;
    }
    return rslt;
  }

// Parameter conditions
//      isTrigger

  String getOperator() => getString("OPERATOR");
  String getTargetValue() => getString("STATE");
  CatreParamRef getParameterReference() => _paramRef as CatreParamRef;
  CatreParameter? getParameter() {
    return _paramRef?.getParameter();
  }

  void setParameter(CatreDevice cd, CatreParameter cp) {
    _paramRef = CatreParamRef.create(catreUniverse, cd, cp);
    if (_paramRef != null) {
      setField("PARAMREF", _paramRef?.getCatreOutput());
    }
  }

  void setOperator(String op) {
    setField("OPERATOR", op);
  }

  void setTargetValue(String val) {
    setField("STATE", val);
  }

  void _setShared(bool shared) {
    setField("SHARED", shared);
  }

// Disable conditions
  String? getDeviceId() => optString("DEVICE");
  void setDeviceId(CatreDevice cd) {
    setField("DEVICE", cd.getDeviceId());
  }

  bool isCheckForEnabled() => getBool("ENABLED");
  void setCheckForEnabled(bool fg) {
    setField("ENABLED", fg);
  }

// Debounce conditions
//        getSubCondition
  num getOnTime() => getNum("ONTIME");
  Duration getOnDuration() {
    int mt = getInt("ONTIME");
    Duration d = Duration(milliseconds: mt);
    return d;
  }

  void setOnTime(int ms) {
    setField("ONTIME", ms);
  }

  num getOffTime() => getNum("OFFTIME");
  Duration getOffDuration() {
    int mt = getInt("OFFTIME");
    Duration d = Duration(milliseconds: mt);
    return d;
  }

  void setOffTime(int ms) {
    setField("OFFTIME", ms);
  }

// Duration conditions
//    getSubcondition(), isTrigger
  num getMinTime() => getNum("MINTIME");
  void setMinTime(int millis) {
    setField("MINTIME", millis);
  }

  Duration getMinDuration() {
    int mt = getInt("MINTIME");
    Duration d = Duration(milliseconds: mt);
    return d;
  }

  num getMaxTime() => getNum("MAXTIME");
  void setMaxTime(int millis) {
    setField("MAXTIME", millis);
  }

  Duration getMaxDuration() {
    int mt = getInt("MAXTIME");
    Duration d = Duration(milliseconds: mt);
    return d;
  }

// Latch conditions
//    getSubcondition
  num? getResetTime() => optNum("RESETTIME");

  void setResetTime(int tod) {
    setField("RESETTIME", tod);
  }

  TimeOfDay? getResetTimeOfDay() {
    int tod = getInt("RESETTIME");
    if (tod < 0) return null;
    tod = tod ~/ (1000 * 60); // to minutes
    tod = tod.remainder(60 * 24); // remove days
    int hr = tod ~/ 60;
    int min = tod.remainder(60);
    return TimeOfDay(hour: hr, minute: min);
  }

  num getResetAfter() => getNum("RESETAFTER");
  num getOffAfter() => getNum("OFFAFTER");
  void setOffAfter(int millis) {
    setField("OFFAFTER", millis);
  }

  Duration getOffAfterDuration() {
    int mt = getInt("OFFAFTER");
    if (mt <= 0) return const Duration();
    return Duration(milliseconds: mt);
  }

// Range conditions
//    getParameterReference, isTrigger
  num? getLowValue() => optNum("LOW");

  void setLowValue(num v) {
    setField("LOW", v);
  }

  num? getHighValue() => optNum("HIGH");

  void setHighValue(num v) {
    setField("HIGH", v);
  }

// Time conditions
  CatreTimeSlot getTimeSlot() {
    _timeSlot ??= CatreTimeSlot.create(catreUniverse);
    return _timeSlot as CatreTimeSlot;
  }

// TriggerTime conditions
  CatreTriggerTime getTriggerTime() => _triggerTime as CatreTriggerTime;

// CalendarEvent conditions
//      getParameterReference
  List<CatreCalendarMatch> getCalendarFields() {
    _calendarFields ??= <CatreCalendarMatch>[];
    return _calendarFields as List<CatreCalendarMatch>;
  }

  void addCalendarFields(int idx) {
    getCalendarFields();
    while (_calendarFields!.length <= idx) {
      _calendarFields?.add(CatreCalendarMatch(catreUniverse));
    }
  }

  // Reference conditions
  // getSubcondition
  String? getSharedName() => optString("SHAREDNAME");

  void setSharedName(String name) {
    setField("SHAREDNAME", name);
    CatreCondition? cc = getUniverse().getProgram().getSharedConditions()[name];
    _subCondition = cc;
  }
}

class CatreConditionType {
  final String label;
  final String _catreType;
  final bool _isTrigger;

  const CatreConditionType(this.label, this._catreType, this._isTrigger);

  bool isTrigger() => _isTrigger;

  bool isEmpty() => _catreType == "UNKNOWN";

  bool isReference() => _catreType == "Reference";

  String get name {
    String s = _catreType;
    if (_isTrigger) s += "_T";
    return s;
  }
}

const List<CatreConditionType> ruleConditionTypes = [
  CatreConditionType("No Condition", "UNKNOWN", false),
  CatreConditionType("Shared Condtion", "Reference", false),
  CatreConditionType("Parameter", "Parameter", false),
  CatreConditionType("Time Period", "Time", false),
  CatreConditionType("Parameter for Duration", "Duration", false),
  CatreConditionType("Parameter Latched", "Latch", false),
  CatreConditionType("Parameter Range", "Range", false),
  CatreConditionType("Calendar Event", "CalendarEvent", false),
  CatreConditionType("Device Enabled/Disabled", "Disabled", false),
  CatreConditionType("Parameter Stable", "Debounce", false),
  CatreConditionType("Always", "Always", false),
];

const List<CatreConditionType> triggerConditionTypes = [
  CatreConditionType("No Trigger Condition", "UNKNOWN", true),
  CatreConditionType("Shared Trigger Condtion", "Reference", true),
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
  late CatreDevice _device;
  late Map<String, dynamic> _values;

  CatreAction.build(CatreUniverse cu, dynamic data) : super(cu, data as Map<String, dynamic>) {
    setup();
  }

  @override
  void setup() {
    _transition = buildItem("TRANSITION", CatreTransitionRef.build);
    _device = _transition.getDevice();
    _values = catreData["PARAMETERS"] as Map<String, dynamic>;
  }

  CatreAction.empty(CatreUniverse cu, this._device, bool trigger)
      : super(cu, <String, dynamic>{
          "TRANSITION": {
            "DEVICE": _device.getDeviceId(),
            "TRANSITION": _device.getDefaultTransition().getName(),
          },
          "PARAMETERS": <String, dynamic>{},
          "LABEL": "Undefined",
          "NAME": "Undefined",
          "DESCRIPTION": "Undefined",
          "USERDESC": false,
        }) {
    setup();
  }

  @override
  Map<String, dynamic> getCatreOutput() {
    setField("TRANSITION", _transition.getCatreOutput());
    return super.getCatreOutput();
  }

  CatreTransitionRef getTransitionRef() => _transition;
  CatreDevice? getDevice() => _device;
  CatreTransition getTransition() => _transition.getTransition() as CatreTransition;

  void setTransition(CatreTransition ct) {
    _transition.setTransition(ct);
  }

  bool isValid() => _transition.getTransition() != null;

  @override
  String buildDescription() {
    return getDescription();
  }

  Map<String, dynamic> getValues() => _values;
  dynamic getValue(CatreParameter cp) {
    return _values[cp.getName()];
  }

  void setValue(CatreParameter cp, dynamic val) {
    _values[cp.getName()] = val;
  }
}

/// *****
///      CatreParamRef : description of reference to a parameter
/// *****

class CatreParamRef extends CatreData {
  CatreParamRef.build(CatreUniverse cu, dynamic data) : super(cu, data as Map<String, dynamic>);

  CatreParamRef.create(CatreUniverse cu, [CatreDevice? cd, CatreParameter? param])
      : super(cu, {
          "DEVICE": cd?.getDeviceId() ?? "Unknown",
          "PARAMETER": param?.getName() ?? "Unknown",
        });

  String getDeviceId() => getString("DEVICE");
  String getParameterName() => getString("PARAMETER");

  CatreDevice? getDevice() {
    return catreUniverse.findDevice(getDeviceId());
  }

  CatreParameter? getParameter() {
    return getDevice()?.findParameter(getParameterName());
  }

  String getTitle() {
    String? ttl = optString("LABEL");
    if (ttl != null) return ttl;
    CatreDevice? dev = getDevice();
    ttl = dev?.getLabel();
    ttl ??= getDeviceId();
    ttl += ".${getParameterName()}";
    return ttl;
  }
}

/// *****
///      CatreTransitionRef : reference to a transition
/// *****

class CatreTransitionRef extends CatreData {
  CatreTransitionRef.build(CatreUniverse cu, dynamic data) : super(cu, data as Map<String, dynamic>);

  String getDeviceId() => getString("DEVICE");
  String getTransitionName() => getString("TRANSITION");

  CatreDevice getDevice() {
    return catreUniverse.findDevice(getDeviceId());
  }

  CatreTransition? getTransition() {
    CatreDevice? cd = getDevice();
    return cd.findTransition(getTransitionName());
  }

  void setTransition(CatreTransition ct) {
    setField("TRANSITION", ct.getName());
  }
}

/// *****
///      CatreTimeSlot :: desription of a time slot for a condition
/// *****

class CatreTimeSlot extends CatreData {
  late DateTime _fromDateTime;
  late DateTime _toDateTime;

  CatreTimeSlot.build(CatreUniverse cu, dynamic data) : super(cu, data as Map<String, dynamic>) {
    setup();
  }

  CatreTimeSlot.create(CatreUniverse cu)
      : super(cu, <String, dynamic>{
          "FROMDATETIME": DateTime.now().millisecondsSinceEpoch,
          "TODATETIME": DateTime.now().millisecondsSinceEpoch + 3600000,
          "ALLDAY": false,
        }) {
    setup();
  }

  @override
  void setup() {
    _fromDateTime = DateTime.fromMillisecondsSinceEpoch(getInt("FROMDATETIME"));
    _toDateTime = DateTime.fromMillisecondsSinceEpoch(getInt("TODATETIME"));
  }

  // bool operator ==(Object other) {
  //   return super == other;
  // }

  DateTime getFromDateTime() => _fromDateTime;
  DateTime getToDateTime() => _toDateTime;
  String? getDays() => optString("DAYS");
  num getRepeatInterval() => getNum("INTERVAL");
  bool getAllDay() => getBool("ALLDAY");
  List<num>? getExcludeDates() => optNumList("EXCLUDE");

  void setFromDate(DateTime date) {
    _fromDateTime = _merge(date, _fromDateTime);
    setField("FROMDATETIME", _fromDateTime.millisecondsSinceEpoch);
    _checkSetFrom();
  }

  void setToDate(DateTime date) {
    _toDateTime = _merge(date, _toDateTime);
    setField("TODATETIME", _toDateTime.millisecondsSinceEpoch);
    _checkSetTo();
  }

  void setFromTime(TimeOfDay time) {
    _fromDateTime = _mergeTime(_fromDateTime, time);
    setField("FROMDATETIME", _fromDateTime.millisecondsSinceEpoch);
    _checkSetFrom();
  }

  void setToTime(TimeOfDay time) {
    _toDateTime = _mergeTime(_toDateTime, time);
    setField("TODATETIME", _toDateTime.millisecondsSinceEpoch);
    _checkSetTo();
  }

  void _checkSetFrom() {
    if (_toDateTime.millisecondsSinceEpoch < _fromDateTime.millisecondsSinceEpoch) {
      _toDateTime = _fromDateTime.add(const Duration(hours: 1));
      setField("TODATETIME", _toDateTime.millisecondsSinceEpoch);
    }
  }

  void _checkSetTo() {
    if (_toDateTime.millisecondsSinceEpoch < _fromDateTime.millisecondsSinceEpoch) {
      _fromDateTime = _toDateTime.subtract(const Duration(hours: 1));
      setField("FROMDATETIME", _fromDateTime.millisecondsSinceEpoch);
    }
  }

  void setDays(List<String> days) {
    days = util.mapDays(days);
    String s = days.join(",");
    setField("DAYS", s);
  }

  void setRepeatInterval(num val) {
    setField("INTERVAL", val);
  }

  DateTime _merge(DateTime date, DateTime time) {
    return DateTime(date.year, date.month, date.day, time.hour, time.minute, time.second);
  }

  DateTime _mergeTime(DateTime date, TimeOfDay time) {
    return DateTime(date.year, date.month, date.day, time.hour, time.minute, 0);
  }
}

/// *****
///      CatreCalendarMatch -- description of a field match for calendar
/// *****

class CatreCalendarMatch extends CatreData {
  CatreCalendarMatch.build(CatreUniverse cu, dynamic data)
      : super(
          cu,
          data as Map<String, dynamic>,
        );

  CatreCalendarMatch(CatreUniverse cu)
      : super(
          cu,
          {"NAME": "TITLE", "MATCHOP": "IGNORE"},
        );

  String getFieldName() => getString("NAME");
  void setFieldName(String name) {
    setField("NAME", name);
  }

  String getOperator() => getString("MATCHOP");
  void setOperator(String op) {
    setField("MATCHOP", op);
  }

  String? getMatchValue() => optString("MATCHVALUE");
  void setMatchValue(String? val) {
    setField("MATCHVALUE", val);
  }
}
