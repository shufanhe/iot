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
import 'package:flutter_spinbox/material.dart';

/// ******
///   Widget definitions
/// ******

class SherpaConditionWidget extends StatefulWidget {
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

  _SensorParameter? _parameter;
  String? _operator;
  dynamic _value;

  DateTime? _startTime;
  DateTime? _endTime;

  _SherpaConditionWidgetState();

  @override
  initState() {
    _forCondition = widget._forCondition;
    super.initState();
    _labelControl.text = _forCondition.getLabel();
    _descControl.text = _forCondition.getDescription();
    _condType = _forCondition.getConditionType();
  }

  @override
  Widget build(BuildContext context) {
    String ttl = _forCondition.getLabel();
    bool trig = _forCondition.isTrigger();
    List<CatreConditionType> ctyps =
        (trig ? triggerConditionTypes : ruleConditionTypes);
    Widget typwid = widgets.dropDownWidget(
        ctyps, (CatreConditionType ct) => ct.label,
        value: _condType, onChanged: _setConditionType);
    List<Widget> specwids = _createWidgets();

    return Scaffold(
      appBar: AppBar(title: Text(ttl), actions: [
        widgets.topMenuAction([
          widgets.MenuAction('Save Changes', _saveCondition),
          widgets.MenuAction('Revert condition', _revertCondition),
        ]),
      ]),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                widgets.fieldSeparator(),
                widgets.textFormField(
                  hint: "Descriptive label for condition",
                  label: "Condition Label",
                  validator: _labelValidator,
                  onSaved: (String? v) => _forCondition.setLabel(v),
                  controller: _labelControl,
                ),
                widgets.fieldSeparator(),
                widgets.textFormField(
                    hint: "Detailed condition description",
                    label: "Condition Description",
                    controller: _descControl,
                    onSaved: (String? v) => _forCondition.setDescription(v),
                    maxLines: 3),
                widgets.fieldSeparator(),
                typwid,
                ...specwids,
                widgets.fieldSeparator(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
                    widgets.submitButton("Accept", _saveCondition),
                    widgets.submitButton("Cancel", _revertCondition),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String? _labelValidator(String? lbl) {
    if (lbl == null || lbl.isEmpty) {
      return "Rule must have a label";
    }
    return null;
  }

  void _setConditionType(CatreConditionType? ct) {
    setState(() {
      if (ct != null) _condType = ct;
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
      case "Duration":
      case "Duration_T":
      case "Latch":
      case "Range":
      case "Range_T":
      case "CalendarEvent":
      case "Disabled":
      case "Debounce":
      case "Debounce_T":
        break;
      default:
        break;
    }

    return rslt;
  }

  List<Widget> _createParameterWidgets() {
    List<Widget> rslt = [];
    List<_SensorParameter> sensors = getSensors();
    _parameter ??= sensors[0];
    Widget w1 = widgets.dropDownWidget(
        sensors, (_SensorParameter sp) => sp.name,
        value: _parameter);
    List<String> ops = _parameter!.getOperators(_forCondition.isTrigger());
    if (!ops.contains(_operator)) _operator = null;
    _operator ??= ops[0];
    Widget w2 = widgets.dropDownWidget(ops, null, value: _operator);
    Widget? w3 = _parameter!.getValueWidget(_value);
    rslt.add(Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        Expanded(child: w1),
        const Spacer(),
      ],
    ));
    rslt.add(
      Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[const Spacer(), Expanded(child: w2), const Spacer()],
      ),
    );
    if (w3 != null) {
      rslt.add(Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: <Widget>[const Spacer(), Expanded(child: w3)],
      ));
    }
    return rslt;
  }

  List<_SensorParameter> getSensors() {
    List<_SensorParameter> plst = [];
    bool trig = _forCondition.isTrigger();
    CatreUniverse cu = _forCondition.catreUniverse;
    for (CatreDevice cd in cu.getInputDevices()) {
      for (CatreParameter cp in cd.getParameters()) {
        // check that cp has operators and value widget
        if (cp.isValidSensor(trig)) plst.add(_SensorParameter(cd, cp));
      }
    }
    return plst;
  }

  List<Widget> _createTimeWidgets() {
    List<Widget> rslt = [];
    if (_startTime != null) {
      _endTime ??= _startTime?.add(const Duration(hours: 1));
    }

    widgets.DateFormField dffs = widgets.DateFormField(context,
        hint: "Choose Start Date",
        initialDate: _startTime,
        onChanged: _setStartDate);
    rslt.add(dffs.widget);
    widgets.TimeFormField tffs = widgets.TimeFormField(context,
        hint: "Choose Start Time", when: _startTime, onChanged: _setStartTime);
    rslt.add(tffs.widget);
    widgets.DateFormField dffe = widgets.DateFormField(context,
        hint: "Choose End Date",
        startDate: _startTime,
        initialDate: _endTime,
        onChanged: _setEndDate);
    rslt.add(dffe.widget);
    widgets.TimeFormField tffe = widgets.TimeFormField(context,
        hint: "Choose End Time", when: _endTime, onChanged: _setEndTime);
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
    return widgets.dropDownWidget<util.RepeatOption>(
        util.getRepeatOptions(), (util.RepeatOption ro) => ro.name,
        onChanged: _setRepeatOption);
  }

  void _setStartTime(TimeOfDay time) {
    print("Set start time");
  }

  void _setStartDate(DateTime date) {
    print("Set start time");
  }

  void _setEndDate(DateTime date) {
    print("Set start time");
  }

  void _setEndTime(TimeOfDay time) {
    print("Set start time");
  }

  void _setDays(List<String> days) {
    print("Set Days $days");
  }

  void _setRepeatOption(util.RepeatOption? opt) {
    print("Set Repeat $opt");
  }

  void _saveCondition() {}
  void _revertCondition() {}
}

class _SensorParameter {
  final CatreDevice _device;
  final CatreParameter _parameter;
  const _SensorParameter(this._device, this._parameter);
  get name {
    return "${_device.getLabel()} ${_parameter.getLabel()}";
  }

  List<String> getOperators(bool trig) {
    if (trig) return _parameter.getTriggerOperators();
    return _parameter.getOperators();
  }

  Widget? getValueWidget(dynamic value) {
    Widget? w;
    switch (_parameter.getParameterType()) {
      case "BOOLEAN":
      case "ENUM":
      case "STRINGLIST":
      case "ENUMREF":
        List<String>? vals = _parameter.getValues();
        if (vals != null) {
          value ??= vals[0];
          return widgets.dropDownWidget(vals, null, value: value);
        }
        break;
      case "TIME":
      case "DATETIME":
      case "DATE":
        break;
      case "INTEGER":
        value ??= _parameter.getMinValue();
        w = SpinBox(
            min: _parameter.getMinValue() as double,
            max: _parameter.getMaxValue() as double,
            value: value,
            onChanged: (value) => {});
        break;
      case "REAL":
        value ??= _parameter.getMaxValue();
        w = SpinBox(
            min: _parameter.getMinValue() as double,
            max: _parameter.getMaxValue() as double,
            value: value,
            decimals: 1,
            onChanged: (value) => {});
        break;
      case "STRING":
        break;
    }

    return w;
  }
}
