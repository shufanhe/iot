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
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/levels.dart';
import 'package:sherpa/pages/rulepage.dart';
import 'package:sherpa/models/catremodel.dart';

/// ******
///   Widget definitions
/// ******

class SherpaRulesetWidget extends StatefulWidget {
  final CatreUniverse _forUniverse;
  final CatreDevice? _forDevice;
  final PriorityLevel _priority;

  const SherpaRulesetWidget(this._forUniverse, this._forDevice, this._priority,
      {super.key});

  @override
  State<SherpaRulesetWidget> createState() => _SherpaRulesetWidgetState();
}

class _SherpaRulesetWidgetState extends State<SherpaRulesetWidget> {
  CatreDevice? _forDevice;
  late final CatreUniverse _forUniverse;
  late final PriorityLevel _priority;

  _SherpaRulesetWidgetState();

  @override
  void initState() {
    _forUniverse = widget._forUniverse;
    _forDevice = widget._forDevice;
    _priority = widget._priority;
    // possibly save and recall _forDevice name
    super.initState();
  }

  Widget _createDeviceSelector() {
    String cur = "All Devices";
    List<String> names = [cur];
    for (CatreDevice d in _forUniverse.getOutputDevices()) {
      if (_forDevice == d) cur = d.getLabel();
      names.add(d.getLabel());
    }

    return widgets.dropDownWidget<CatreDevice>(
        _forUniverse.getOutputDevices().toList(),
        (CatreDevice cd) => cd.getLabel(),
        value: _forDevice,
        nullValue: "All Devices",
        onChanged: _deviceSelected);
  }

  Future<void> _deviceSelected(CatreDevice? dev) async {
    setState(() => {_forDevice = dev});
  }

  Widget _buildRuleWidget(CatreRule cr) {
    List<widgets.MenuAction> acts = [
      widgets.MenuAction('Edit Rule', () => _editRule(cr)),
      widgets.MenuAction('Remove Rule', () => _removeRule(cr)),
      widgets.MenuAction('Add Rule Before', () => _newRule(cr, false, false)),
      widgets.MenuAction('Add Rule After', () => _newRule(cr, false, true)),
      widgets.MenuAction('Add Trigger Before', () => _newRule(cr, true, false)),
      widgets.MenuAction('Add Trigger After', () => _newRule(cr, true, true)),
    ];
    PriorityLevel? pl1 = _priority.getLowerLevel();
    if (pl1 != null) {
      num h = pl1.highPriority;
      acts.add(widgets.MenuAction("Move to ${pl1.name}",
          () => _findRulePriority(cr, h - 1, true, pl1)));
    }
    pl1 = _priority.getHigherLevel();
    if (pl1 != null) {
      num h = pl1.lowPriority;
      acts.add(widgets.MenuAction(
          "Move to ${pl1.name}", () => _findRulePriority(cr, h, false, pl1)));
    }
    return widgets.itemWithMenu(cr.getLabel(), acts,
        onTap: () => _editRule(cr));
  }

  void _editRule(CatreRule cr) {
    widgets.goto(context, SherpaRuleWidget(cr));
  }

  void _removeRule(CatreRule cr) {
    // double check with user
    print("Edit rule ${cr.getLabel()}");
  }

  void _newRule(CatreRule cr, bool trig, bool after) {
    print("Add rule $trig $after ${cr.getLabel()}");
  }

  void _findRulePriority(CatreRule cr, num p, bool below, PriorityLevel? lvl) {
    num p1 = 0;
    num prior = globals.minPriority;
    num top = globals.maxPriority;
    for (CatreRule xr in _forUniverse.getProgram().getRules()) {
      if (xr.getPriority() > p) {
        p1 = (prior + xr.getPriority()) / 2.0;
        top = xr.getPriority();
        break;
      } else if (xr.getPriority() == p && below) {
        p1 = (prior + xr.getPriority()) / 2.0;
        top = xr.getPriority();
        break;
      } else {
        prior = xr.getPriority();
      }
    }
    if (p1 == 0) {
      p1 = (prior + top) / 2.0;
    }
    if (lvl != null) {
      if (p1 < lvl.lowPriority) {
        p1 = (lvl.lowPriority + top) / 2.0;
        prior = lvl.lowPriority;
      }
      if (p1 >= lvl.highPriority) {
        p1 = (prior + lvl.highPriority) / 2.0;
      }
    }
    print("Set Rule Priority $p1 ${cr.getLabel()}");
  }

  void _handleReorder(List<CatreRule> rules, int o, int n) {}

  @override
  Widget build(BuildContext context) {
    String ttl = "SherPA ${_priority.name} Rules";
    Widget devsel = Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        const Text("Rules for Device:   "),
        Expanded(
          child: _createDeviceSelector(),
        ),
      ],
    );
    List<Widget> devsell = [];
    if (widget._forDevice == null) devsell.add(devsel);
    List<CatreRule> rules =
        _forUniverse.getProgram().getSelectedRules(_priority, _forDevice);

    List<Widget> rulewl = rules.map(_buildRuleWidget).toList();
    Widget rulew = ReorderableListView(
      onReorder: (int o, int n) => {_handleReorder(rules, o, n)},
      children: rulewl,
    );

    return Scaffold(
      appBar: AppBar(
        title: Text(ttl),
        actions: [
          widgets.topMenuAction([
            if (_forDevice != null)
              widgets.MenuAction('Add a New Rule', _addRule),
            if (_forDevice != null)
              widgets.MenuAction('Add a New Trigger', _addTrigger),
            if (_forDevice != null)
              widgets.MenuAction('Show Current Device States', _showStates),
            widgets.MenuAction('Restore or Reload Program', _reload),
          ]),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            ...devsell,
            Expanded(child: rulew),
          ],
        ),
      ),
    );
  }

  void _addRule() {
    print("Add a new rule for ${_forDevice?.getName()}");
  }

  void _addTrigger() {
    print("Add a new trigger for ${_forDevice?.getName()}");
  }

  void _showStates() {
    print("Show device states for ${_forDevice?.getName()}");
  }

  void _reload() {
    print("Reload program");
  }
}
