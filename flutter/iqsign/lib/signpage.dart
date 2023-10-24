/*
 *        signpage.dart
 * 
 *    Page for a single sign
 * 
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
///******************************************************************************

import 'signdata.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import 'globals.dart' as globals;
import 'widgets.dart' as widgets;
import 'loginpage.dart';
import 'editsignpage.dart';
import 'util.dart' as util;

class IQSignSignWidget extends StatelessWidget {
  final SignData _signData;

  const IQSignSignWidget(this._signData, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignSignPage(_signData);
  }
}

class IQSignSignPage extends StatefulWidget {
  final SignData _signData;

  const IQSignSignPage(this._signData, {super.key});

  @override
  State<IQSignSignPage> createState() => _IQSignSignPageState();
}

class _IQSignSignPageState extends State<IQSignSignPage> {
  SignData _signData = SignData.unknown();
  List<String> _signNames = [];
  late Future<List<String>> _signNamesFuture;
  _IQSignSignPageState();

  @override
  void initState() {
    _signData = widget._signData;
    _signNamesFuture = _getNames();
    super.initState();
  }

  Future<List<String>> _getNames() async {
    var url = Uri.https(util.getServerURL(), "/rest/namedsigns",
        {'session': globals.sessionId});
    var resp = await http.get(url);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    var jsd = js['data'];
    var rslt = <String>[];
    for (final sd1 in jsd) {
      rslt.add(sd1['name']);
    }
    setState(() {
      _signNames = rslt;
    });

    return rslt;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("iQsign Sign ${_signData.getName()}"),
        actions: [
          widgets.topMenu(_handleCommand, [
            {'EditSign': "Customize the Sign"},
            {'Logout': "Log Out"},
          ]),
        ],
      ),
      body: Center(
        child: FutureBuilder<List<String>>(
          future: _signNamesFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const CircularProgressIndicator();
            } else if (snapshot.hasError) {
              return Text('Error: ${snapshot.error}');
            } else {
              _signNames = snapshot.data!;
              return Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  widgets.fieldSeparator(),
                  Container(
                    padding: const EdgeInsets.all(24),
                    child: Image.network(_signData.getImageUrl()),
                  ),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        const Text("Set Sign to "),
                        _createNameSelector(),
                      ]),
                ],
              );
            }
          },
        ),
      ),
    );
  }

  dynamic _gotoLogin(dynamic) {
    Navigator.push(
        context, MaterialPageRoute(builder: (context) => const IQSignLogin()));
  }

  Future _handleLogout() async {
    var url = Uri.https(util.getServerURL(), "/rest/logout");
    await http.post(url);
  }

  dynamic _gotoEdit() {
    Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) => IQSignSignEditWidget(_signData, _signNames)));
  }

  void _handleCommand(String cmd) {
    switch (cmd) {
      case "EditSign":
        _gotoEdit();
        break;
      case "Logout":
        _handleLogout().then(_gotoLogin);
        break;
    }
  }

  void _setSignToSaved(String name) async {
    var url = Uri.https(
      util.getServerURL(),
      "/rest/sign/${_signData.getSignId()}/setTo",
    );
    var resp = await http.put(url, body: {
      'session': globals.sessionId,
      'value': name,
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] == "OK") {
      var jsd = js['data'];
      _setSignData(jsd);
    }
  }

  void _setSignData(data) {
    setState(() => {_signData.update(data)});
  }

  Widget _createNameSelector() {
    return DropdownButton<String>(
      items: _signNames.map<DropdownMenuItem<String>>((String value) {
        return DropdownMenuItem<String>(
          value: value,
          child: Text(value),
        );
      }).toList(),
      onChanged: (String? value) async {
        if (value != null) _setSignToSaved(value);
      },
      value: _signData.getDisplayName(),
    );
  }
}
