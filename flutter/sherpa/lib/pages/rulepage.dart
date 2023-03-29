/*
 *      programpage.dart
 * 
 *   Overview page for the user's program
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
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _labelControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();

  _SherpaRuleWidgetState();

  @override
  initState() {
    _forRule = widget._forRule;
    super.initState();
    _labelControl.text = _forRule.getLabel();
    _descControl.text = _forRule.getDescription();
  }

  @override
  Widget build(BuildContext context) {
    String ttl = _forRule.getLabel();
    return Scaffold(
      appBar: AppBar(title: Text(ttl), actions: [
        widgets.topMenuAction([
          widgets.MenuAction('Save Changes', _saveRule),
          widgets.MenuAction('Revert rule', _revertRule),
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
                widgets.fieldSeparator(),
                widgets.textFormField(
                  hint: "Descriptive label for rule",
                  label: "Rule Label",
                  validator: _labelValidator,
                  controller: _labelControl,
                  onSaved: (String? v) => _forRule.setLabel(v),
                ),
                widgets.fieldSeparator(),
                widgets.textFormField(
                    hint: "Detailed rule description",
                    label: "Rule Description",
                    controller: _descControl,
                    onSaved: (String? v) => _forRule.setDescription(v),
                    maxLines: 3),
                widgets.fieldSeparator(),
                Flexible(
                    child: widgets.listBox(
                        "Condition",
                        _forRule.getAndConditions(),
                        _conditionBuilder,
                        _conditionAdder)),
                Flexible(
                    child: widgets.listBox("Action", _forRule.getActions(),
                        _actionBuilder, _actionAdder)),
                widgets.fieldSeparator(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
                    widgets.submitButton("Validate", _validateRule),
                    const Spacer(),
                    widgets.submitButton("Accept", _saveRule),
                    const Spacer(),
                    widgets.submitButton("Cancel", _revertRule),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _saveRule() {
    if (_formKey.currentState!.validate()) {
      _forRule.setLabel(_labelControl.text);
      _forRule.setDescription(_descControl.text);
      _formKey.currentState!.save();
    }
    // validate rule to ensure it is useful -- ask user
    //    to approve the validation conditions if there are any
    // get updated conditions
    // get updated actions
    print("Handle save rule");
  }

  void _revertRule() {
    _forRule.revert();
    Navigator.of(context).pop();
  }

  void _validateRule() {
    print("Handle validation here");
  }

  Widget _conditionBuilder(CatreCondition cc) {
    List<widgets.MenuAction> acts = [];
    if (_forRule.getAndConditions().length > 1 && !cc.isTrigger()) {
      acts.add(widgets.MenuAction('Remove', () {
        _removeCondition(cc);
      }));
    }
    acts.add(widgets.MenuAction('Edit', () {
      _editCondition(cc);
    }));

    return widgets.itemWithMenu(
      cc.getLabel(),
      acts,
      onTap: () {
        _showCondition(cc);
      },
      onDoubleTap: () {
        _editCondition(cc);
      },
    );
  }

  String? _labelValidator(String? lbl) {
    if (lbl == null || lbl.isEmpty) {
      return "Rule must have a label";
    }
    return null;
  }

  void _conditionAdder() {
    print("Create new condition");
    // create new condition
  }

  void _removeCondition(CatreCondition cc) {
    print("Remove condition");
  }

  void _editCondition(CatreCondition cc) {
    widgets.goto(context, SherpaConditionWidget(_forRule, cc));
  }

  void _showCondition(CatreCondition cc) {
    print("Show condition");
  }

  Widget _actionBuilder(CatreAction ca) {
    List<widgets.MenuAction> acts = [];
    if (_forRule.getActions().length > 1) {
      acts.add(widgets.MenuAction('Remove', () {
        _removeAction(ca);
      }));
    }
    acts.add(widgets.MenuAction('Edit', () {
      _editAction(ca);
    }));

    return widgets.itemWithMenu(
      ca.getLabel(),
      acts,
      onTap: () {
        _showAction(ca);
      },
      onDoubleTap: () {
        _editAction(ca);
      },
    );
  }

  void _actionAdder() {
    print("Create new action");
    // create new Action
  }

  void _removeAction(CatreAction ca) {
    print("Remvoe action");
  }

  void _editAction(CatreAction ca) {
    CatreDevice? cd = _forRule.getDevice();
    if (cd != null) {
      widgets.goto(context, SherpaActionWidget(cd, _forRule, ca));
    }
  }

  void _showAction(CatreAction ca) {
    print("Show action");
  }
}
