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
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/levels.dart' as levels;
import 'package:sherpa/models/catremodel.dart';
import 'loginpage.dart' as login;
import 'rulesetpage.dart';

/// ******
///   Widget definitions
/// ******

class SherpaProgramWidget extends StatefulWidget {
  final CatreUniverse _theUniverse;

  const SherpaProgramWidget(this._theUniverse, {super.key});

  @override
  State<SherpaProgramWidget> createState() => _SherpaProgramWidgetState();
}

class _SherpaProgramWidgetState extends State<SherpaProgramWidget> {
  CatreDevice? _forDevice;
  late CatreUniverse _theUniverse;

  _SherpaProgramWidgetState();

  @override
  void initState() {
    _theUniverse = widget._theUniverse;
    // possibly save and recall _forDevice name
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    List<Widget> comps = util.skipNulls([
      _createPriorityView(levels.overrideLevel, true),
      _createPriorityView(levels.highLevel, false),
      _createPriorityView(levels.mediumLevel, false),
      _createPriorityView(levels.lowLevel, false),
      _createPriorityView(levels.defaultLevel, true),
    ]);

    return Scaffold(
      appBar: AppBar(
        title: const Text("SherPA Program"),
        actions: [
          widgets.topMenuAction([
            widgets.MenuAction(
              'Show Current Device States',
              _showDeviceStates,
            ),
            widgets.MenuAction(
              'Restore or Reload Program',
              _reloadProgram,
            ),
            widgets.MenuAction(
              'Create Virtual Condition',
              _createVirtualCondition,
            ),
            widgets.MenuAction('Log Off', _logOff),
          ]),
        ],
      ),
      body: widgets.sherpaPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  const Text(
                    "Rules for Device:   ",
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.brown),
                  ),
                  widgets.fieldSeparator(),
                  Expanded(child: _createDeviceSelector()),
                ],
              ),
              widgets.fieldSeparator(),
              Column(
                children: <Widget>[...comps],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showDeviceStates() {
    util.logD("Show device states");
  }

  void _reloadProgram() {
    setState(() async {
      CatreModel cm = CatreModel();
      await cm.loadUniverse();
    });
  }

  void _createVirtualCondition() {
    util.logD("Create Virtual condition");
  }

  void _logOff() {
    CatreModel cm = CatreModel();
    cm.removeUniverse();
    widgets.gotoReplace(context, const login.SherpaLoginWidget());
  }

  Widget _createDeviceSelector() {
    return widgets.dropDownWidget<CatreDevice>(
        _theUniverse.getOutputDevices().toList(), (CatreDevice d) => d.getLabel(),
        onChanged: _deviceSelected, value: _forDevice, nullValue: "All Devices");
  }

  Future<void> _deviceSelected(CatreDevice? value) async {
    setState(() => _forDevice = value);
  }

  void _handleSelect(levels.PriorityLevel lvl) {
    CatreProgram pgm = _theUniverse.getProgram();
    if (_forDevice != null) {
      List<CatreRule> all = pgm.getSelectedRules(null, _forDevice);
      if (all.length <= 5) lvl = levels.allLevel;
    }
    widgets.goto(context, SherpaRulesetWidget(_theUniverse, _forDevice, lvl));
  }

  Widget? _createPriorityView(levels.PriorityLevel lvl, bool optional) {
    CatreProgram pgm = _theUniverse.getProgram();
    int ct = 0;
    List<String> rules = [];
    for (CatreRule cr in pgm.getRules()) {
      if (cr.getPriority() < lvl.lowPriority || cr.getPriority() >= lvl.highPriority) continue;
      CatreDevice? cd = cr.getDevice();
      if (_forDevice != null && cd != _forDevice) continue;
      ++ct;
      if (ct <= globals.numRulesToDisplay) {
        rules.add(cr.getLabel());
      } else if (ct == globals.numRulesToDisplay + 1) {
        String s = rules[globals.numRulesToDisplay - 2];
        rules[globals.numRulesToDisplay - 2] = "$s ...";
        rules[globals.numRulesToDisplay - 1] = cr.getLabel();
      } else {
        rules[globals.numRulesToDisplay - 1] = cr.getLabel();
      }
    }
    if (ct == 0 && optional) return null;
    String nrul = "$ct Rule${(ct == 1) ? '' : 's'}";
    TextStyle lblstyle = const TextStyle(
      fontWeight: FontWeight.bold,
      color: globals.labelColor,
      fontSize: 20.0,
    );
    List<Text> rulew = rules.map((s) => Text(s)).toList();
    Text label = Text(
      "${lvl.name} Rules",
      textAlign: TextAlign.left,
      style: lblstyle,
    );
    return GestureDetector(
        onTap: () => _handleSelect(lvl),
        onDoubleTap: () => _handleSelect(lvl),
        child: Container(
            padding: const EdgeInsets.all(4.0),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              border: Border.all(width: 4, color: globals.borderColor),
            ),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, children: <Widget>[
              Row(
                children: <Widget>[
                  Expanded(child: label),
                  Text(nrul),
                ],
              ),
              ...rulew
            ])));
  }
}
