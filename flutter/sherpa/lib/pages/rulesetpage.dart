/*
 *      rulesetpage.dart
 * 
 *   Description of a set of rules for a device at a given priority level
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

import 'package:flutter/material.dart';
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/levels.dart';
import 'package:sherpa/pages/rulepage.dart';
import 'package:sherpa/pages/authorizationpage.dart';
import 'package:sherpa/models/catremodel.dart';

/// ******
///   Widget definitions
/// ******

class SherpaRulesetWidget extends StatefulWidget {
  final CatreUniverse _theUniverse;
  final CatreDevice? _theDevice;
  final PriorityLevel _priority;

  const SherpaRulesetWidget(this._theUniverse, this._theDevice, this._priority, {super.key});

  @override
  State<SherpaRulesetWidget> createState() => _SherpaRulesetWidgetState();
}

class _SherpaRulesetWidgetState extends State<SherpaRulesetWidget> {
  CatreDevice? _forDevice;
  late final CatreUniverse _forUniverse;
  late final PriorityLevel _priority;
  List<CatreRule> _ruleSet = [];

  _SherpaRulesetWidgetState();

  @override
  void initState() {
    _forUniverse = widget._theUniverse;
    _forDevice = widget._theDevice;
    _priority = widget._priority;
    // possibly save and recall _forDevice name
    super.initState();
  }

  Widget _createDeviceSelector() {
    List<CatreDevice> devs = _forUniverse.getOutputDevices().toList();
    devs.sort(_deviceSorter);
    return widgets.dropDownWidget<CatreDevice>(
      devs,
      labeler: (CatreDevice cd) => cd.getLabel(),
      value: _forDevice,
      nullValue: "All Devices",
      onChanged: _deviceSelected,
    );
  }

  int _deviceSorter(CatreDevice cd1, CatreDevice cd2) {
    String s1 = cd1.getLabel().toLowerCase();
    String s2 = cd2.getLabel().toLowerCase();
    return s1.compareTo(s2);
  }

  Future<void> _deviceSelected(CatreDevice? dev) async {
    if (dev != null && dev != _forDevice) {
      await dev.updateValues();
    }
    setState(() => _forDevice = dev);
  }

  Widget _buildRuleWidget(CatreRule cr) {
    List<widgets.MenuAction> acts = [
      widgets.MenuAction('Edit Rule', () => _editRule(cr)),
      widgets.MenuAction('Remove Rule', () => _removeRule(cr)),
    ];
    if (_forDevice != null) {
      acts.addAll([
        widgets.MenuAction('Add New Rule Before', () => _newRule(cr, false, true)),
        widgets.MenuAction('Add New Rule After', () => _newRule(cr, false, false)),
        widgets.MenuAction('Add New Trigger Before', () => _newRule(cr, true, true)),
        widgets.MenuAction('Add New Trigger After', () => _newRule(cr, true, false)),
      ]);
    }

    PriorityLevel? pl1 = _priority.getLowerLevel();
    if (pl1 != null) {
      PriorityLevel pl1a = pl1;
      acts.add(widgets.MenuAction("Move to ${pl1.name}", () => _findRulePriority(pl1a.highPriority - 1, true, pl1a)));
    }
    pl1 = _priority.getHigherLevel();
    if (pl1 != null) {
      PriorityLevel pl1a = pl1;
      num h = pl1a.lowPriority;
      acts.add(widgets.MenuAction("Move to ${pl1.name}", () => _findRulePriority(h, false, pl1a)));
    }
    return widgets.itemWithMenu(
      cr.getLabel(),
      acts,
      onTap: () => _conditionalEdit(cr),
      onDoubleTap: () => _editRule(cr),
    );
  }

  void _conditionalEdit(CatreRule cr) {
    if (cr.getLabel().startsWith("Undefined")) {
      _editRule(cr);
    } else {
      _describeRule(cr);
    }
  }

  void _handleReorder(int o, int n) async {
    if (o == n) return;
    CatreRule cr0 = _ruleSet[o];
    bool below = false;
    if (o < n) n -= 1;
    CatreRule? cr;
    if (n >= _ruleSet.length) {
      below = true;
      cr = _ruleSet.last;
    } else {
      cr = _ruleSet[n];
    }

    num p = cr.getPriority();
    num priority = _findRulePriority(p, below, _priority);
    _forUniverse.getProgram().setRulePriority(cr0, priority);

    await cr0.addOrEditRule();
    setState(() {
      _forUniverse.getProgram().reorderRules();
    });
  }

  @override
  Widget build(BuildContext context) {
    String ttl = "${_priority.name} Rules";
    Widget devsel = Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        const Text("Rules for Device:   ",
            style: TextStyle(
              fontWeight: FontWeight.bold,
            )),
        Expanded(
          child: _createDeviceSelector(),
        ),
      ],
    );
    _ruleSet = _forUniverse.getProgram().getSelectedRules(
          _priority,
          _forDevice,
        );

    List<Widget> rulewl = _ruleSet.map(_buildRuleWidget).toList();
    Widget rulew = ReorderableListView(
      onReorder: _handleReorder,
      children: rulewl,
    );

    return Scaffold(
      appBar: AppBar(
        title: Text(ttl),
        actions: [
          widgets.topMenuAction([
            if (_forDevice != null)
              widgets.MenuAction(
                'Add a New Rule',
                _addRule,
              ),
            if (_forDevice != null)
              widgets.MenuAction(
                'Add a New Trigger',
                _addTrigger,
              ),
            if (_forDevice != null)
              widgets.MenuAction(
                'Show Current Device States',
                _showStates,
              ),
            widgets.MenuAction(
              'Add or Modify Authorizations',
              _updateBridges,
            )
          ]),
        ],
      ),
      body: widgets.sherpaNSPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              if (widget._theDevice == null) devsel,
              if (widget._theDevice == null) widgets.fieldSeparator(),
              Expanded(child: rulew),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[
                  IconButton(
                    icon: const Icon(Icons.add_alert_outlined),
                    tooltip: "Add a new trigger rule",
                    onPressed: _addTrigger,
                  ),
                  IconButton(
                    icon: const Icon(Icons.add_task_outlined),
                    tooltip: 'Add new rule',
                    onPressed: _addRule,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _describeRule(CatreRule cr) async {
    String desc = cr.getDescription();
    return widgets.displayDialog(context, "Rule Description", desc);
  }

  void _editRule(CatreRule cr) async {
    await widgets.gotoThen(context, SherpaRuleWidget(cr));
    setState(() {});
  }

  Future<CatreRule?> _newRule(CatreRule? cr, bool trig, bool after) async {
    if (_forDevice == null) {
      await widgets.displayDialog(
        context,
        "Can't Create Rule",
        "Please select a device in order to create a rule",
      );
      return null;
    }
    num basepriority = _priority.lowPriority;
    bool below = false;

    if (cr == null) {
      List<CatreRule> rules = _forUniverse.getProgram().getSelectedRules(
            _priority,
            _forDevice,
          );
      if (rules.isNotEmpty) {
        if (after) {
          cr = rules.last;
        } else {
          cr = rules.first;
        }
      }
    }

    if (cr != null) {
      basepriority = cr.getPriority();
      if (!after) below = true;
    }

    num p = _findRulePriority(basepriority, below, _priority);
    CatreDevice cd = _forDevice as CatreDevice;
    CatreRule? rslt;
    setState(() {
      rslt = _forUniverse.getProgram().addRule(cd, p, trig);
    });

    return rslt;
  }

  num _findRulePriority(num p, bool below, PriorityLevel lvl) {
    num prior = (below ? p : lvl.highPriority);
    num next = (below ? lvl.lowPriority : p);
    for (CatreRule xr in _forUniverse.getProgram().getSelectedRules(lvl, _forDevice)) {
      if (xr.getPriority() > p) {
        prior = xr.getPriority();
      } else if (xr.getPriority() == p) {
        if (!below) break;
        prior = p;
      } else {
        next = xr.getPriority();
        break;
      }
    }
    num p0 = (next + prior) / 2.0;
    return p0;
  }

  Future<void> _removeRule(CatreRule cr) async {
    bool sts = true;
    if (!cr.getLabel().startsWith("Undefined")) {
      sts = await widgets.getValidation(
        context,
        "Remove Rule ${cr.getLabel()}",
      );
    }
    if (sts) {
      setState(() {
        _forUniverse.getProgram().removeRule(cr);
      });
    }
  }

  void _addRule() async {
    CatreRule? cr = await _newRule(null, false, true);
    if (cr != null) {
      _editRule(cr);
    }
  }

  void _addTrigger() async {
    await _newRule(null, true, true);
  }

  void _showStates() async {
    Map<String, dynamic>? states = await _forDevice?.issueCommandWithArgs(
      "/universe/deviceStates",
      {"DEVICEID": _forDevice?.getDeviceId()},
    );
    util.logD("Show device states for ${_forDevice?.getName()}");
    util.logD("Result: $states");
    // TODO: Show device states in dialog
  }

  void _updateBridges() {
    widgets.goto(context, SherpaAuthorizeWidget(_forUniverse));
  }
}
