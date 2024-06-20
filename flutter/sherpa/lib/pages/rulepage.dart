/*
 *      rulepage.dart
 * 
 *   Page letting the user view and edit a single rule
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
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/models/catremodel.dart';
import 'package:sherpa/globals.dart' as globals;
import 'conditionpage.dart';
import 'actionpage.dart';

/// ******
///   Widget definitions
/// ******

class SherpaRuleWidget extends StatefulWidget {
  final CatreRule _forRule;

  const SherpaRuleWidget(this._forRule, {super.key});

  @override
  State<SherpaRuleWidget> createState() => _SherpaRuleWidgetState();
}

class _SherpaRuleWidgetState extends State<SherpaRuleWidget> {
  late CatreRule _forRule;
  late CatreDevice _forDevice;
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _labelControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();
  bool _labelMatchesDescription = false;

  _SherpaRuleWidgetState();

  @override
  initState() {
    _forRule = widget._forRule;
    _forDevice = _forRule.getDevice();
    super.initState();
    _labelControl.text = _forRule.getLabel();
    _descControl.text = _forRule.getDescription();
    _labelMatchesDescription = _labelControl.text == _descControl.text;
    if (_labelMatchesDescription) {
      _labelControl.addListener(_labelListener);
      _descControl.addListener(_descriptionListener);
    }
  }

  @override
  Widget build(BuildContext context) {
    String ttl = "Rule Editor for ${_forDevice.getName()}";
    return Scaffold(
      appBar: AppBar(title: Text(ttl), actions: [
        widgets.topMenuAction([
          widgets.MenuAction('Save Changes', _saveRule),
          widgets.MenuAction('Revert rule', _revertRule),
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
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  widgets.fieldSeparator(),
                  widgets.fieldSeparator(),
                  _ruleLabel(),
                  widgets.fieldSeparator(),
                  _ruleDescription(),
                  widgets.fieldSeparator(),
                  _ruleConditions(),
                  widgets.fieldSeparator(),
                  _ruleActions(),
                  widgets.fieldSeparator(),
                  _ruleBottomButtons(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ruleLabel() {
    return widgets.textFormField(
      hint: "Descriptive label for rule",
      label: "Rule Label",
      validator: _labelValidator,
      controller: _labelControl,
      onSaved: (String? v) => _forRule.setLabel(v),
    );
  }

  Widget _ruleDescription() {
    return widgets.textFormField(
        hint: "Detailed rule description",
        label: "Rule Description",
        controller: _descControl,
        onSaved: (String? v) => _forRule.setDescription(v),
        maxLines: 3);
  }

  Widget _ruleConditions() {
    return Flexible(
      child: widgets.listBox(
        "Condition",
        _forRule.getConditions(),
        _conditionBuilder,
        _addCondition,
      ),
    );
  }

  Widget _ruleActions() {
    return Flexible(
      child: widgets.listBox(
        "Action",
        _forRule.getActions(),
        _actionBuilder,
        _actionAdder,
      ),
    );
  }

  Widget _ruleBottomButtons() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: <Widget>[
        widgets.submitButton("Validate", _validateRule),
        widgets.submitButton(
          "Accept",
          _saveRule,
          enabled: _isRuleAcceptable(),
        ),
        widgets.submitButton("Cancel", _revertRule),
      ],
    );
  }

  Widget _conditionBuilder(CatreCondition cc) {
    List<widgets.MenuAction> acts = [];
    acts.add(widgets.MenuAction('Edit', () {
      _editCondition(cc);
    }));

    if (_forRule.getConditions().length > 1 && !cc.isTrigger()) {
      acts.add(widgets.MenuAction('Remove', () {
        _removeCondition(cc);
      }));
    }

    return widgets.itemWithMenu(
      cc.getLabel(),
      acts,
      onTap: () {
        _showCondition(cc);
      },
      onLongPress: () {
        _editCondition(cc);
      },
      onDoubleTap: () {
        _editCondition(cc);
      },
    );
  }

  Widget _actionBuilder(CatreAction ca) {
    List<widgets.MenuAction> acts = [];

    acts.add(widgets.MenuAction('Edit', () {
      _editAction(ca);
    }));

    if (_forRule.getActions().length > 1) {
      acts.add(widgets.MenuAction('Remove', () {
        _removeAction(ca);
      }));
    }

    return widgets.itemWithMenu(ca.getLabel(), acts, onTap: () {
      _showAction(ca);
    }, onDoubleTap: () {
      _editAction(ca);
    }, onLongPress: () {
      _editAction(ca);
    });
  }

  void _saveRule() {
    if (_formKey.currentState!.validate()) {
      _forRule.setLabel(_labelControl.text);
      _forRule.setDescription(_descControl.text);
      _formKey.currentState!.save();
    }
    // TODO: Run validator to ensure rule is okay,
    // Pop up validation check window for user,
    // Actually save rule
    setState(() {
      _forRule.push();
      Navigator.pop(context);
    });
  }

  void _revertRule() {
    setState(() {
      if (!_forRule.pop()) _forRule.revert();
    });
    // Navigator.of(context).pop();
  }

  void _validateRule() async {
    // TODO: create validator; create validation output page
    util.logD("Handle validation here");

    var url = Uri.https(util.getServerURL(), "/rule/validate");
    var body = {
      globals.catreSession: globals.sessionId,
      "RULE": convert.jsonEncode(_forRule.getCatreOutput()),
    };
    var resp = await http.post(url, body: body);
    if (resp.statusCode >= 400) throw Exception("Bad response from CATRE");
    var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    util.logD("Validate response $jresp");
  }

  String? _labelValidator(String? lbl) {
    if (lbl == null || lbl.isEmpty) {
      return "Rule must have a label";
    }
    return null;
  }

  void _addCondition() {
    setState(() {
      _forRule.addNewCondition();
    });
  }

  void _removeCondition(CatreCondition cc) {
    setState(() {
      _forRule.removeCondition(cc);
    });
  }

  void _editCondition(CatreCondition cc) async {
    await widgets.gotoThen(context, SherpaConditionWidget(_forRule, cc));
    setState(() {});
  }

  void _showCondition(CatreCondition cc) {
    widgets.displayDialog(context, "Condition Description", cc.getDescription());
  }

  void _actionAdder() {
    setState(() {
      _forRule.addNewAction(_forDevice);
    });
  }

  void _removeAction(CatreAction ca) {
    setState(() {
      _forRule.removeAction(ca);
    });
  }

  void _editAction(CatreAction ca) async {
    CatreDevice cd = _forRule.getDevice();
    await widgets.gotoThen(context, SherpaActionWidget(cd, _forRule, ca));
    setState(() {});
  }

  void _showAction(CatreAction ca) {
    widgets.displayDialog(context, "Action Description", ca.getDescription());
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

  bool _isRuleAcceptable() {
    if (_labelControl.text.isEmpty) return false;
    if (_labelControl.text == 'Undefined') return false;
    if (_descControl.text.isEmpty) return false;
    if (_descControl.text == 'Undefined') return false;
    bool havecond = false;
    for (CatreCondition cc in _forRule.getConditions()) {
      if (!cc.getConditionType().isEmpty()) havecond = true;
    }
    if (!havecond) return false;
    // might want to check other items
    return true;
  }
}
