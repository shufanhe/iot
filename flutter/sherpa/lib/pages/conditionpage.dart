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
    _SensorParameter sp = sensors.firstWhere(
        (_SensorParameter sp) => sp.parameter == _forCondition.getParameter(),
        orElse: () => sensors[0]);
    Widget w1 = widgets.dropDownWidget(
      sensors,
      (_SensorParameter sp) => sp.name,
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
    List<String> ops = sp.getOperators(_forCondition.isTrigger());
    String? op = _forCondition.getOperator();
    if (!ops.contains(op)) op = null;
    op ??= ops[0];
    Widget w2 = widgets.dropDownWidget(
      ops,
      (String s) => s,
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
    Widget? w3 = sp.getValueWidget(_forCondition.getTargetValue(),
        textAlign: TextAlign.right, onChanged: _setTargetValue);
    rslt.add(w1);
    rslt.add(w2);
    if (w3 != null) {
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

  List<_SensorParameter> getSensors() {
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

  void _setParameter(_SensorParameter? sp) {
    if (sp == null) return;
    _forCondition.setParameter(sp.device, sp.parameter);
  }

  void _setOperator(String? op) {
    if (op == null) return;
    _forCondition.setOperator(op);
  }

  void _setTargetValue(dynamic val) {
    _forCondition.setTargetValue(val.toString());
    util.log("Set value $val");
  }

  List<Widget> _createTimeWidgets() {
    List<Widget> rslt = [];
    DateTime starttime = _forCondition.getTimeSlot().getFromDateTime();
    DateTime endtime = _forCondition.getTimeSlot().getToDateTime();

    widgets.DateFormField dffs = widgets.DateFormField(context,
        hint: "Choose Start Date",
        initialDate: starttime,
        onChanged: _setStartDate);
    rslt.add(dffs.widget);
    rslt.add(widgets.fieldSeparator());
    widgets.TimeFormField tffs = widgets.TimeFormField(context,
        hint: "Choose Start Time",
        current: starttime,
        onChanged: _setStartTime);
    rslt.add(tffs.widget);
    rslt.add(widgets.fieldSeparator());

    widgets.DateFormField dffe = widgets.DateFormField(context,
        hint: "Choose End Date",
        startDate: starttime,
        initialDate: endtime,
        onChanged: _setEndDate);
    rslt.add(dffe.widget);
    rslt.add(widgets.fieldSeparator());

    widgets.TimeFormField tffe = widgets.TimeFormField(context,
        hint: "Choose End Time", current: endtime, onChanged: _setEndTime);
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
    _forCondition.getTimeSlot().setFromTime(time);
  }

  void _setStartDate(DateTime date) {
    _forCondition.getTimeSlot().setFromDate(date);
  }

  void _setEndDate(DateTime date) {
    _forCondition.getTimeSlot().setToDate(date);
  }

  void _setEndTime(TimeOfDay time) {
    _forCondition.getTimeSlot().setToTime(time);
  }

  void _setDays(List<String> days) {
    _forCondition.getTimeSlot().setDays(days);
  }

  void _setRepeatOption(util.RepeatOption? opt) {
    if (opt != null) {
      _forCondition.getTimeSlot().setRepeatInterval(opt.value);
    }
  }

  void _saveCondition() {
    // TODO: validate condition, then save it if okay, dialog if not
    util.log("Save condition");
  }

  void _revertCondition() {
    setState(() {
      _forCondition.revert();
    });
  }
}

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

  Widget? getValueWidget(dynamic value,
      {textAlign = TextAlign.left, Function(dynamic)? onChanged}) {
    Widget? w;
    onChanged ??= _dummySet;
    switch (parameter.getParameterType()) {
      case "BOOLEAN":
      case "ENUM":
      case "STRINGLIST":
      case "ENUMREF":
        List<String>? vals = parameter.getValues();
        if (vals != null) {
          value ??= vals[0];
          return widgets.dropDownWidget(
            vals,
            null,
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
        value ??= parameter.getMinValue();
        w = SpinBox(
          min: parameter.getMinValue() as double,
          max: parameter.getMaxValue() as double,
          value: value,
          textAlign: textAlign,
          onChanged: onChanged,
        );
        break;
      case "REAL":
        value ??= parameter.getMaxValue();
        w = SpinBox(
          min: parameter.getMinValue() as double,
          max: parameter.getMaxValue() as double,
          value: value,
          textAlign: textAlign,
          decimals: 1,
          onChanged: onChanged,
        );
        break;
      case "STRING":
        break;
    }

    return w;
  }

  void _dummySet(dynamic) {}
}
