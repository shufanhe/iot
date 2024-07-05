/*
 *        autorizationpage.dart
 * 
 *    top-level interface to Catre models
 * 
 **/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/ /// *******************************************************************************
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
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/models/catremodel.dart';

class SherpaAuthroizeWidget extends StatefulWidget {
  final CatreUniverse _theUniverse;

  const SherpaAuthroizeWidget(this._theUniverse, {super.key});

  @override
  State<SherpaAuthroizeWidget> createState() => _SherpaAuthroizeWidgetState();
}

class _SherpaAuthroizeWidgetState extends State<SherpaAuthroizeWidget> {
  late CatreUniverse _theUniverse;
  CatreBridge? _bridgeData;
  String? _curBridge;

  _SherpaAuthroizeWidgetState();

  @override
  void initState() {
    _theUniverse = widget._theUniverse;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Configure Device Bridges"),
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
                    "Configure Bridge:   ",
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.brown),
                  ),
                  widgets.fieldSeparator(),
                  Expanded(child: _createBridgeSelector()),
                ],
              ),
              widgets.fieldSeparator(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _createBridgeSelector() {
    List<String> names = _theUniverse.getBridgeNames();
    _curBridge ??= names[0];
    _bridgeData = _theUniverse.getBridge(_curBridge as String);
    return widgets.dropDownWidget<String>(
      _theUniverse.getBridgeNames(),
      onChanged: _bridgeSelected,
      value: _curBridge,
    );
  }

  Future<void> _bridgeSelected(String? value) async {
    if (value == null) return;
    setState(() {
      _curBridge = value;
      _bridgeData = _theUniverse.getBridge(value);
    });
  }
}


/* end of module authorizationpage.dart */


