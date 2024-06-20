/*
 *      conditionpage.dart
 * 
 *   Overview page for the viewing and editing conditions
 */
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

import 'package:flutter/material.dart';
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/models/catremodel.dart';
import 'package:day_picker/day_picker.dart';
// import 'package:flutter_spinbox/material.dart';

/// ******
///   Widget definitions
/// ******

class SherpaConditionWidget extends StatefulWidget {
  // ignore: unused_field
  final CatreRule _forRule;
  final CatreCondition _forCondition;

  const SherpaConditionWidget(this._forRule, this._forCondition, {super.key});

  @override
  State<SherpaConditionWidget> createState() => _SherpaConditionWidgetState();
}

class _SherpaConditionWidgetState extends State<SherpaConditionWidget> {
  late CatreCondition _forCondition;

  late CatreConditionType _condType;
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _labelControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();
  bool _labelMatchesDescription = false;

  // CatreParameter? _parameter;
  // String? _operator;
  // dynamic _value;

  _SherpaConditionWidgetState();

  @override
  initState() {
    _forCondition = widget._forCondition;
    super.initState();
    _labelControl.text = _forCondition.getLabel();
    _descControl.text = _forCondition.getDescription();
    _condType = _forCondition.getConditionType();
    _labelMatchesDescription = _labelControl.text == _descControl.text;
    _labelControl.addListener(_labelListener);
    _descControl.addListener(_descriptionListener);
  }

  @override
  Widget build(BuildContext context) {
    String ttl = "Condition Editor";
    List<Widget> specwids = _createWidgets();

    return Scaffold(
      appBar: AppBar(title: Text(ttl), actions: [
        widgets.topMenuAction([
          widgets.MenuAction('Save Changes', _saveCondition),
          widgets.MenuAction('Revert condition', _revertCondition),
        ]),
      ]),
      body: widgets.sherpaPage(
        context,
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Form(
              key: _formKey,
              onPopInvoked: _popInvoked,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  widgets.fieldSeparator(),
                  _conditionLabel(),
                  widgets.fieldSeparator(),
                  _conditionDescription(),
                  widgets.fieldSeparator(),
                  _conditionType(),
                  widgets.boxWidgets(specwids),
                  widgets.fieldSeparator(),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: <Widget>[
                      widgets.submitButton(
                        "Accept",
                        _saveCondition,
                        enabled: _isConditionValid(),
                      ),
                      widgets.submitButton("Cancel", _revertCondition),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _conditionLabel() {
    return widgets.textFormField(
      hint: "Descriptive label for condition",
      label: "Condition Label",
      validator: _labelValidator,
      onChanged: _setLabel,
      controller: _labelControl,
    );
  }

  Widget _conditionDescription() {
    return widgets.textFormField(
        hint: "Detailed condition description",
        label: "Condition Description",
        controller: _descControl,
        onChanged: _setDescription,
        maxLines: 3);
  }

  Widget _conditionType() {
    bool trig = _forCondition.isTrigger();
    List<CatreConditionType> ctyps = (trig ? triggerConditionTypes : ruleConditionTypes);
    return widgets.dropDownWidget(ctyps,
        labeler: (CatreConditionType ct) => ct.label,
        value: _condType,
        onChanged: _setConditionType,
        label: "Condition Type");
  }

  String? _labelValidator(String? lbl) {
    if (lbl == null || lbl.isEmpty) {
      return "Rule must have a label";
    }
    return null;
  }

  void _setConditionType(CatreConditionType? ct) {
    if (ct == null) return;
    _forCondition.setConditionType(ct);
    setState(() {
      _condType = ct;
    });
  }

  List<Widget> _createWidgets() {
    List<Widget> rslt = [];
    switch (_condType.name) {
      case "Parameter":
      case "Parameter_T":
        rslt = _createParameterWidgets();
        break;
      case "Time":
        rslt = _createTimeWidgets();
        break;
      case "TriggerTime_T":
        break;
      case "Duration":
      case "Duration_T":
        rslt = _createDurationWidgets();
        break;
      case "Latch":
        rslt = _createLatchWidgets();
        break;
      case "Range":
      case "Range_T":
        rslt = _createRangeWidgets();
        break;
      case "CalendarEvent":
        rslt = _createCalendarWidgets();
        break;
      case "Disabled":
        rslt = _createEnabledWidgets();
        break;
      case "Debounce":
      case "Debounce_T":
        rslt = _createDebounceWidgets();
        break;
      default:
        break;
    }

    return rslt;
  }

  List<Widget> _createParameterWidgets() {
    List<Widget> rslt = [];
    CatreCondition cc = _getRefCondition();
    List<_SensorParameter> sensors = _getSensors();
    CatreParameter? cpq = cc.getParameter();
    _SensorParameter sp = sensors.firstWhere(
      (_SensorParameter sp) => sp.parameter == cpq,
      orElse: () => sensors[0],
    );
    Widget w1 = widgets.dropDownWidget(
      sensors,
      labeler: (_SensorParameter sp) => sp.name,
      value: sp,
      onChanged: _setParameter,
    );
    w1 = Row(
      children: <Widget>[
        const Spacer(),
        Flexible(
          flex: 10,
          child: w1,
        ),
      ],
    );
    List<String> ops = sp.getOperators(cc.isTrigger());
    String? op = cc.getOperator();
    if (!ops.contains(op)) op = null;
    op ??= ops[0];
    Widget w2 = widgets.dropDownWidget(
      ops,
      value: op,
      textAlign: TextAlign.center,
      onChanged: _setOperator,
    );
    w2 = Row(
      children: <Widget>[
        const Spacer(),
        Flexible(
          flex: 5,
          child: w2,
        ),
        // const Spacer(),
      ],
    );

    Widget? w3 = sp.getValueWidget(
      cc.getTargetValue(),
      textAlign: TextAlign.right,
      onChanged: _setTargetValue,
    );
    rslt.add(w1);
    rslt.add(w2);
    if (w3 != null) {
      rslt.add(widgets.fieldSeparator());
      w3 = Row(
        children: <Widget>[
          const Spacer(),
          Flexible(
            flex: 10,
            child: w3,
          ),
        ],
      );
      rslt.add(w3);
    }
    return rslt;
  }

  List<Widget> _createTimeWidgets() {
    List<Widget> rslt = [];
    DateTime starttime = _forCondition.getTimeSlot().getFromDateTime();
    DateTime endtime = _forCondition.getTimeSlot().getToDateTime();

    rslt.add(widgets.fieldSeparator());
    widgets.DateFormField dffs = widgets.DateFormField(
      context,
      hint: "Choose Start Date",
      initialDate: starttime,
      onChanged: _setStartDate,
    );
    rslt.add(dffs.widget);
    rslt.add(widgets.fieldSeparator());
    widgets.TimeFormField tffs = widgets.TimeFormField(
      context,
      hint: "Choose Start Time",
      current: starttime,
      onChanged: _setStartTime,
    );
    rslt.add(tffs.widget);
    rslt.add(widgets.fieldSeparator());

    widgets.DateFormField dffe = widgets.DateFormField(
      context,
      hint: "Choose End Date",
      startDate: starttime,
      initialDate: endtime,
      onChanged: _setEndDate,
    );
    rslt.add(dffe.widget);
    rslt.add(widgets.fieldSeparator());

    widgets.TimeFormField tffe = widgets.TimeFormField(
      context,
      hint: "Choose End Time",
      current: endtime,
      onChanged: _setEndTime,
    );
    rslt.add(tffe.widget);

    Widget w = SelectWeekDays(onSelect: _setDays, days: util.getDays());
    rslt.add(w);

    Widget r = Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        const Text("Repeat:   "),
        Expanded(
          child: _createRepeatSelector(),
        ),
      ],
    );
    rslt.add(r);

    return rslt;
  }

  Widget _createRepeatSelector() {
    List<util.RepeatOption> opts = util.getRepeatOptions();
    util.RepeatOption value = opts[0];
    int v0 = _forCondition.getTimeSlot().getRepeatInterval().toInt();
    for (util.RepeatOption opt in opts) {
      if (opt.value == v0) value = opt;
    }

    return widgets.dropDownWidget<util.RepeatOption>(
      opts,
      value: value,
      labeler: (util.RepeatOption ro) => ro.name,
      onChanged: _setRepeatOption,
    );
  }

  List<Widget> _createDurationWidgets() {
    List<Widget> rslt = [];

    Widget w1 = InputDecorator(
      decoration: InputDecoration(
        labelText: "After Condition",
        labelStyle: widgets.getLabelStyle(),
        border: const OutlineInputBorder(
          borderSide: BorderSide(
            width: 8,
            color: Color.fromARGB(128, 210, 180, 140),
          ),
        ),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: _createParameterWidgets(),
      ),
    );

    Duration mindur = _forCondition.getMinDuration();
    Duration maxdur = _forCondition.getMaxDuration();
    widgets.DurationFormField minff = widgets.DurationFormField(
      context,
      hint: "Choose minimum duration",
      initialDuration: mindur,
      onChanged: _setMinTime,
    );
    widgets.DurationFormField maxff = widgets.DurationFormField(
      context,
      hint: "Choose maximum duration",
      initialDuration: maxdur,
      onChanged: _setMaxTime,
    );

    rslt.add(widgets.fieldSeparator());
    rslt.add(w1);
    rslt.add(widgets.fieldSeparator());
    rslt.add(minff.widget);
    rslt.add(widgets.fieldSeparator());
    rslt.add(maxff.widget);

    return rslt;
  }

  List<Widget> _createLatchWidgets() {
    List<Widget> rslt = [];

    Widget w1 = InputDecorator(
      decoration: InputDecoration(
        labelText: "After Condition",
        labelStyle: widgets.getLabelStyle(),
        border: const OutlineInputBorder(
          borderSide: BorderSide(
            width: 8,
            color: Color.fromARGB(128, 210, 180, 140),
          ),
        ),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: _createParameterWidgets(),
      ),
    );

    Duration offdur = _forCondition.getOffAfterDuration();
    TimeOfDay? maxdur = _forCondition.getResetTimeOfDay();
    widgets.DurationFormField offff = widgets.DurationFormField(
      context,
      hint: "Choose off after interval",
      initialDuration: offdur,
      onChanged: _setOffAfter,
    );
    widgets.TimeFormField resetff = widgets.TimeFormField(
      context,
      hint: "Choose reset time of day",
      initialTime: maxdur,
      onChanged: _setResetTime,
    );

    rslt.add(widgets.fieldSeparator());
    rslt.add(w1);
    rslt.add(widgets.fieldSeparator());
    rslt.add(offff.widget);
    rslt.add(widgets.fieldSeparator());
    rslt.add(resetff.widget);

    return rslt;
  }

  List<Widget> _createRangeWidgets() {
    List<Widget> rslt = [];

    List<_SensorParameter> sensors = _getRangeSensors();
    if (sensors.isEmpty) return rslt;

    _SensorParameter sp = sensors.firstWhere(
      (_SensorParameter sp) => sp.parameter == _forCondition.getParameter(),
      orElse: () => sensors[0],
    );
    Widget w1 = widgets.dropDownWidget(
      sensors,
      labeler: (_SensorParameter sp) => sp.name,
      value: sp,
      onChanged: _setRangeParameter,
    );
    w1 = Row(
      children: <Widget>[
        const Spacer(),
        Flexible(
          flex: 10,
          child: w1,
        ),
      ],
    );

    Widget w2 = sp.getValueWidget(
      _forCondition.getLowValue(),
      onChanged: _setLowValue,
      label: "Low Value",
    ) as Widget;
    Widget w3 = sp.getValueWidget(
      _forCondition.getHighValue(),
      onChanged: _setHighValue,
      label: "High Value",
    ) as Widget;

    rslt.add(w1);
    rslt.add(widgets.fieldSeparator());
    rslt.add(w2);
    rslt.add(widgets.fieldSeparator());
    rslt.add(w3);

    return rslt;
  }

  List<Widget> _createCalendarWidgets() {
    List<Widget> rslt = [];

    List<CatreCalendarMatch> flds = _forCondition.getCalendarFields();
    int ct = flds.length;
    if (ct < 2) {
      ct = 2;
    } else if (ct < 5) {
      ct = ct + 1;
    }
    List<String> matchops = getMatchOps();
    List<String> calfields = getCalendarFields();

    for (int i = 0; i < ct; ++i) {
      CatreCalendarMatch? item = (i < flds.length ? flds[i] : null);
      String? match = item?.getMatchValue();
      String? field = item?.getFieldName();
      field ??= calfields[0];
      String? op = item?.getOperator();
      op ??= matchops[0];

      Widget w1 = widgets.dropDownWidget(
        calfields,
        value: field,
        onChanged: (String? v) {
          _setCalField(i, v);
        },
      );
      Widget w2 = widgets.dropDownWidget(
        matchops,
        value: op,
        onChanged: (String? v) {
          _setCalOp(i, v);
        },
      );

      TextEditingController ctrl = TextEditingController(text: match);
      Widget w3 = widgets.textField(
          hint: "match", controller: ctrl, onChanged: (String v) => {_setCalMatch(i, v)}, showCursor: true);
      Widget w1a = Row(children: <Widget>[const Spacer(), Flexible(flex: 10, child: w1)]);
      Widget w2a = Row(children: <Widget>[const Spacer(), Flexible(flex: 5, child: w2)]);
      Widget w3a = Row(children: <Widget>[const Spacer(), Flexible(flex: 10, child: w3)]);
      rslt.add(w1a);
      rslt.add(w2a);
      rslt.add(w3a);
    }

    return rslt;
  }

  List<String> getCalendarFields() {
    return ["TITLE", "WHERE", "WHO", "CONTENT", "CALENDAR", "TRANSPARENCY", "ALLDAY"];
  }

  List<String> getMatchOps() {
    return ["IGNORE", "MATCH", "NOMATCH", "ISNULL", "ISNONNULL"];
  }

  List<Widget> _createEnabledWidgets() {
    List<Widget> rslt = [];
    List<CatreDevice> devs = _getDevices();
    String? devid = _forCondition.getDeviceId();
    CatreDevice dev = devs[0];
    if (devid != null) dev = _forCondition.getUniverse().findDevice(devid);
    List<String> choices = ["IS ENABLED", "IS DISABLED"];

    Widget w1 = widgets.dropDownWidget(
      devs,
      labeler: (d) => d.getName(),
      value: dev,
      onChanged: _setDevice,
      label: "For Device",
    );
    Widget w2 = widgets.dropDownWidget(choices, onChanged: _setEnabled);
    rslt.add(w1);
    rslt.add(w2);

    return rslt;
  }

  List<Widget> _createDebounceWidgets() {
    List<Widget> rslt = [];

    Widget w1 = InputDecorator(
      decoration: InputDecoration(
        labelText: "After Condition",
        labelStyle: widgets.getLabelStyle(),
        border: const OutlineInputBorder(
          borderSide: BorderSide(
            width: 8,
            color: Color.fromARGB(128, 210, 180, 140),
          ),
        ),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: _createParameterWidgets(),
      ),
    );

    Duration ondur = _forCondition.getOnDuration();
    Duration offdur = _forCondition.getOffDuration();
    widgets.DurationFormField minff = widgets.DurationFormField(
      context,
      hint: "Choose stable on time",
      initialDuration: ondur,
      onChanged: _setOnTime,
    );
    widgets.DurationFormField maxff = widgets.DurationFormField(
      context,
      hint: "Choose stable off time",
      initialDuration: offdur,
      onChanged: _setOffTime,
    );

    rslt.add(widgets.fieldSeparator());
    rslt.add(w1);
    rslt.add(widgets.fieldSeparator());
    rslt.add(minff.widget);
    rslt.add(widgets.fieldSeparator());
    rslt.add(maxff.widget);

    return rslt;
  }

  List<_SensorParameter> _getSensors() {
    List<_SensorParameter> plst = [];
    bool trig = _forCondition.isTrigger();
    CatreUniverse cu = _forCondition.catreUniverse;
    for (CatreDevice cd in cu.getInputDevices()) {
      for (CatreParameter cp in cd.getParameters()) {
        // check that cp has operators and value widget
        if (cp.isValidSensor(trig)) {
          _SensorParameter sp = _SensorParameter(cd, cp);
          plst.add(sp);
        }
      }
    }
    return plst;
  }

  List<_SensorParameter> _getRangeSensors() {
    List<_SensorParameter> plst = [];
    bool trig = _forCondition.isTrigger();
    CatreUniverse cu = _forCondition.catreUniverse;
    for (CatreDevice cd in cu.getInputDevices()) {
      for (CatreParameter cp in cd.getParameters()) {
        if (cp.isValidSensor(trig)) {
          switch (cp.getParameterType()) {
            case "INTEGER":
            case "REAL":
              break;
            default:
              continue;
          }
          _SensorParameter sp = _SensorParameter(cd, cp);
          plst.add(sp);
        }
      }
    }
    return plst;
  }

  List<CatreDevice> _getDevices() {
    CatreUniverse cu = _forCondition.catreUniverse;
    List<CatreDevice> plst = [];
    for (CatreDevice cd in cu.getInputDevices()) {
      plst.add(cd);
    }
    return plst;
  }

  void _setLabel(String? lbl) {
    setState(() {
      _forCondition.setLabel(lbl);
      _forCondition.setDescription(_descControl.text);
    });
  }

  void _setDescription(String? d) {
    setState(() {
      _forCondition.setDescription(d);
      _forCondition.setLabel(_labelControl.text);
    });
  }

  void _setParameter(_SensorParameter? sp) {
    if (sp == null) return;
    setState(() {
      _getRefCondition().setParameter(sp.device, sp.parameter);
    });
  }

  void _setRangeParameter(_SensorParameter? sp) {
    if (sp == null) return;
    if (_getRefCondition().getParameter() == sp.parameter) return;
    _setParameter(sp);
    setState(() {
      _forCondition.setLowValue(sp.parameter.getMinValue());
      _forCondition.setHighValue(sp.parameter.getMaxValue());
    });
  }

  void _setOperator(String? op) {
    if (op == null) return;
    setState(() {
      _getRefCondition().setOperator(op);
    });
  }

  void _setTargetValue(dynamic val) {
    setState(() {
      _getRefCondition().setTargetValue(val.toString());
    });
  }

  CatreCondition _getRefCondition() {
    switch (_condType.name) {
      case "Parameter":
      case "Range":
        return _forCondition;
      case "Duration":
      case "Latch":
      case "Debounce":
        return _forCondition.getSubcondition();
      default:
        return _forCondition;
    }
  }

  void _setDevice(CatreDevice? dev) {
    if (dev != null) {
      setState(() {
        _forCondition.setDeviceId(dev);
      });
    }
  }

  void _setEnabled(String? what) {
    if (what == null) {
      return;
    } else {
      setState(() {
        if (what == "IS ENABLED") {
          _forCondition.setCheckForEnabled(true);
        } else {
          _forCondition.setCheckForEnabled(false);
        }
      });
    }
  }

  void _setStartTime(TimeOfDay time) {
    setState(() {
      _forCondition.getTimeSlot().setFromTime(time);
    });
  }

  void _setStartDate(DateTime date) {
    setState(() {
      _forCondition.getTimeSlot().setFromDate(date);
    });
  }

  void _setEndDate(DateTime date) {
    setState(() {
      _forCondition.getTimeSlot().setToDate(date);
    });
  }

  void _setEndTime(TimeOfDay time) {
    setState(() {
      _forCondition.getTimeSlot().setToTime(time);
    });
  }

  void _setDays(List<String> days) {
    setState(() {
      _forCondition.getTimeSlot().setDays(days);
    });
  }

  void _setRepeatOption(util.RepeatOption? opt) {
    if (opt != null) {
      setState(() {
        _forCondition.getTimeSlot().setRepeatInterval(opt.value);
      });
    }
  }

  void _setMinTime(Duration d) {
    setState(() {
      int min = d.inMilliseconds;
      int max = _forCondition.getMaxTime().toInt();
      _forCondition.setMinTime(min);
      if (max < min) {
        _forCondition.setMaxTime(min + 60000);
      }
    });
  }

  void _setMaxTime(Duration d) {
    setState(() {
      int min = _forCondition.getMinTime().toInt();
      int max = d.inMilliseconds;
      _forCondition.setMaxTime(max);
      if (min > max) {
        min = max - 60000;
        if (min < 0) min = 0;
        _forCondition.setMinTime(min);
      }
    });
  }

  void _setOffAfter(Duration d) {
    setState(() {
      _forCondition.setOffAfter(d.inMilliseconds);
      if (d.inMilliseconds > 0) _forCondition.setResetTime(-1);
    });
  }

  void _setResetTime(TimeOfDay td) {
    setState(() {
      _forCondition.setOffAfter(0);
      _forCondition.setResetTime((td.hour * 60 + td.minute * 60) * 60 * 1000);
    });
  }

  void _setOnTime(Duration d) {
    setState(() {
      _forCondition.setOnTime(d.inMilliseconds);
    });
  }

  void _setOffTime(Duration d) {
    setState(() {
      _forCondition.setOffTime(d.inMilliseconds);
    });
  }

  void _setLowValue(dynamic val) {
    if (val == null) return;
    num lv = val as num;
    setState(() {
      _forCondition.setLowValue(lv);
      num? hv = _forCondition.getHighValue();
      if (hv == null || hv.toDouble() < lv.toDouble()) {
        _forCondition.setHighValue(lv);
      }
    });
  }

  void _setHighValue(dynamic val) {
    if (val == null) return;
    num hv = val as num;
    setState(() {
      _forCondition.setHighValue(hv);
      num? lv = _forCondition.getLowValue();
      if (lv == null || lv.toDouble() > hv.toDouble()) {
        _forCondition.setLowValue(hv);
      }
    });
  }

  void _setCalField(int idx, String? val) {
    CatreCalendarMatch cm = getCalendarItem(idx);
    if (val != null) {
      setState(() {
        cm.setFieldName(val);
      });
    }
  }

  void _setCalOp(int idx, String? op) {
    CatreCalendarMatch cm = getCalendarItem(idx);
    if (op != null) {
      setState(() {
        cm.setOperator(op);
      });
    }
  }

  void _setCalMatch(int idx, String v) {
    CatreCalendarMatch cm = getCalendarItem(idx);
    setState(() {
      cm.setMatchValue(v);
    });
  }

  CatreCalendarMatch getCalendarItem(int idx) {
    List<CatreCalendarMatch> flds = _forCondition.getCalendarFields();
    _forCondition.addCalendarFields(idx);
    return flds[idx];
  }

  void _labelListener() {
    if (_labelMatchesDescription) {
      _descControl.text = _labelControl.text;
    }
  }

  void _descriptionListener() {
    if (_labelMatchesDescription) {
      if (_labelControl.text != _descControl.text) {
        _labelMatchesDescription = false;
      }
    }
  }

  bool _isConditionValid() {
    if (_labelControl.text.isEmpty) return false;
    if (_labelControl.text == 'Undefined') return false;
    if (_descControl.text.isEmpty) return false;
    if (_descControl.text == 'Undefined') return false;
    // might want other checks here if we don't ensure validity in the setXXX methods
    return true;
  }

  void _saveCondition() {
    setState(() {
      _forCondition.push();
      Navigator.pop(context);
    });
  }

  void _revertCondition() {
    setState(() {
      if (!_forCondition.pop()) _forCondition.revert();
    });
  }

  void _popInvoked(bool didpop) {}
} // end of class _SherpaConditionWidgetState

class _SensorParameter {
  final CatreDevice device;
  final CatreParameter parameter;
  const _SensorParameter(this.device, this.parameter);

  get name {
    return "${device.getLabel()} ${parameter.getLabel()}";
  }

  List<String> getOperators(bool trig) {
    if (trig) return parameter.getTriggerOperators();
    return parameter.getOperators();
  }

  Widget? getValueWidget(dynamic value, {textAlign = TextAlign.left, Function(dynamic)? onChanged, String? label}) {
    Widget? w;
    label ??= parameter.getName();
    onChanged ??= _dummySet;
    switch (parameter.getParameterType()) {
      case "BOOLEAN":
      case "ENUM":
      case "STRINGLIST":
      case "ENUMREF":
        List<String>? vals = parameter.getValues();
        if (vals != null) {
          value = vals[0];
          return widgets.dropDownWidget(
            vals,
            value: value,
            textAlign: textAlign,
            onChanged: onChanged,
          );
        }
        break;
      case "TIME":
      case "DATETIME":
      case "DATE":
        break;
      case "INTEGER":
        int vint = util.getIntValue(value, parameter.getMinValue());
        w = widgets.integerField(
          min: parameter.getMinValue().toInt(),
          max: parameter.getMaxValue().toInt(),
          value: vint,
          textAlign: textAlign,
          label: label,
          onChanged: onChanged,
        );
        break;
      case "REAL":
        double vdbl = util.getDoubleValue(value, parameter.getMinValue());
        w = widgets.doubleField(
          min: parameter.getMinValue().toDouble(),
          max: parameter.getMaxValue().toDouble(),
          value: vdbl,
          onChanged: onChanged,
          label: label,
        );
        break;
      case "STRING":
        TextEditingController ctrl = TextEditingController(text: value.toString());
        w = widgets.textField(
          hint: "Value for $label",
          controller: ctrl,
          onChanged: onChanged,
          showCursor: true,
        );
        break;
    }

    return w;
  }

  void _dummySet(dynamic) {}
}       // end of class _SensorParameter

