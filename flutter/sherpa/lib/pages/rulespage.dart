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
import 'package:sherpa/levels.dart';
import 'package:sherpa/models/catreuniverse.dart';
import 'package:sherpa/models/catredevice.dart';
import 'package:sherpa/models/catreprogram.dart';

// ******
///   Globals
/// ******

CatreUniverse? _rulesUniverse;
CatreDevice? _rulesDevice;
PriorityLevel? _priorityArg;

/// ******
///   Widget definitions
/// ******

class SherpaRulesWidget extends StatefulWidget {
  SherpaRulesWidget(CatreUniverse u, CatreDevice? dev, PriorityLevel lvl,
      {super.key}) {
    _rulesUniverse = u;
    _rulesDevice = dev;
    _priorityArg = lvl;
  }

  @override
  State<SherpaRulesWidget> createState() => _SherpaRulesWidgetState();
}

class _SherpaRulesWidgetState extends State<SherpaRulesWidget> {
  CatreDevice? _forDevice;
  late CatreUniverse _forUniverse;
  late PriorityLevel _priority;
  late bool _deviceKnown;

  _SherpaRulesWidgetState() {
    _forUniverse = _rulesUniverse as CatreUniverse;
    _forDevice = _rulesDevice;
    _priority = _priorityArg as PriorityLevel;
    _deviceKnown = _forDevice != null;
  }

  @override
  void initState() {
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

    var chng = (_deviceKnown ? null : _deviceSelected);
    return widgets.dropDownWidget<CatreDevice>(
        _forUniverse.getOutputDevices().toList(),
        (CatreDevice cd) => cd.getLabel(),
        value: _forDevice,
        nullvalue: "All Devices",
        onChanged: chng);
  }

  Future<void> _deviceSelected(CatreDevice? dev) async {
    setState(() => {_forDevice = dev});
  }

  Widget _buildRuleWidget(CatreRule cr) {
    List<String> labels = [
      "Edit Rule",
      "Remove Rule",
      "Add Rule Before",
      "Add Rule After",
    ];
    PriorityLevel? pl = _priority.getLowerLevel();
    if (pl != null) labels.add("Move to ${pl.name} Priority");
    pl = _priority.getHigherLevel();
    if (pl != null) labels.add("Move to ${pl.name} Priority");

    Widget w = GestureDetector(
      onTap: (() => _editRule(cr)),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          PopupMenuButton(
            icon: const Icon(Icons.menu_sharp),
            itemBuilder: (context) =>
                labels.map<PopupMenuItem<String>>(widgets.menuItem).toList(),
          ),
          Expanded(
            child: Text(cr.getLabel()),
          ),
        ],
      ),
    );
    return w;
  }

  void _editRule(CatreRule cr) {
    print("Edit rule ${cr.getLabel()}");
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
    if (!_deviceKnown) devsell.add(devsel);
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
          widgets.topMenu(_handleCommand, [
            {'AddRule', "Add a New Rule"},
            {'ShowDeviceStates', 'Show Current Device States'},
            {'Restore', 'Restore or Reload Program'},
            {'Logout': "Log Off"},
          ]),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            ...devsell,
            rulew,
          ],
        ),
      ),
    );
  }

  void _handleCommand(String cmd) async {
    switch (cmd) {
      case 'AddRule':
        break;
      case 'ShowDeviceStates':
        break;
      case 'Logout':
        break;
      case 'Restore':
        break;
    }
  }
}
