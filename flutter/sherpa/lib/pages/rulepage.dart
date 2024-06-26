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
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/models/catremodel.dart';
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
    );
  }

  Widget _ruleDescription() {
    return widgets.textFormField(
        hint: "Detailed rule description", label: "Rule Description", controller: _descControl, maxLines: 3);
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
        _addNewAction,
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

  void _saveRule() async {
    if (_formKey.currentState!.validate()) {
      _forRule.setLabel(_labelControl.text);
      _forRule.setName(_labelControl.text);
      _forRule.setDescription(_descControl.text);
      await _forRule.addOrEditRule();
      // ensure validation has been run, run it if not
      // ensure validation status is ok
      setState(() {});
    }

    setState(() {
      _forRule.push();
      Navigator.pop(context);
    });
  }

  void _revertRule() {
    setState(() {
      if (!_forRule.pop()) {
        _forRule.revert();
        Navigator.of(context).pop();
      }
    });
  }

  _validateRule() async {
    _forRule.setLabel(_labelControl.text);
    _forRule.setName(_labelControl.text);
    _forRule.setDescription(_descControl.text);
    Map<String, dynamic>? jresp = await _forRule.issueCommand(
      "/rule/validate",
      "RULE",
    );

    // TODO: create validate output page if needed

    util.logD("Validate response $jresp");
  }

  String? _labelValidator(String? lbl) {
    if (lbl == null || lbl.isEmpty) {
      return "Rule must have a label";
    }
    return null;
  }

  void _addCondition() async {
    CatreCondition? cond;
    setState(() {
      cond = _forRule.addNewCondition();
    });
    if (cond != null) {
      _editCondition(cond!);
    }
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

  void _addNewAction() {
    CatreAction? act;
    setState(() {
      act = _forRule.addNewAction(_forDevice);
    });
    if (act != null) {
      _editAction(act!);
    }
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
