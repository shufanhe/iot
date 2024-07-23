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

import '../signdata.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import '../globals.dart' as globals;
import '../widgets.dart' as widgets;
import 'loginpage.dart';
import 'editsignpage.dart';
import '../util.dart' as util;

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
  final TextEditingController _extraControl = TextEditingController();
  bool _preview = false;
  String? _baseSign;

  @override
  void initState() {
    _signData = widget._signData;
    _signNamesFuture = _getNames();
    _analyzeSign();

    super.initState();
  }

  Future<List<String>> _getNames() async {
    var url = Uri.https(util.getServerURL(), "/rest/namedsigns",
        {'session': globals.iqsignSession});
    var resp = await http.get(url);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    var jsd = js['data'];
    List<String> rslt = <String>[];
    for (final sd1 in jsd) {
      String s = sd1['name'];
      if (!rslt.contains(s)) rslt.add(s);
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
        title: Text(_signData.getName(),
            style: const TextStyle(
                fontWeight: FontWeight.bold, color: Colors.black)),
        actions: [
          widgets.topMenu(_handleCommand, [
            {'EditSign': "Create New Saved Sign"},
            {'GenerateKey': "Generate Login Key"},
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
              String url = _signData.getLocalImageUrl(_preview);
              return Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  widgets.fieldSeparator(),
                  Container(
                    padding: const EdgeInsets.all(24),
                    child: Image.network(
                      url,
                      width: MediaQuery.of(context).size.width * 0.4,
                      height: MediaQuery.of(context).size.height * 0.4,
                    ),
                  ),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        const Text("Set Sign to "),
                        _createNameSelector(),
                      ]),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Additional text for the sign",
                    label: "Additional Text",
                    controller: _extraControl,
                    maxLines: 3,
                    onChanged: _handleOtherText,
                    enabled: _canHaveOtherText(),
                  ),
                  widgets.fieldSeparator(),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: <Widget>[
                        widgets.submitButton(
                          "Preview",
                          _previewAction,
                          enabled: _isSignValid(),
                        ),
                        widgets.submitButton(
                          "Update",
                          _updateAction,
                          enabled: _isSignValid(),
                        )
                      ])
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

  void _analyzeSign() {
    List<String> lines = _signData.getSignBody().split("\n");
    _baseSign = null;
    _extraControl.text = "";
    for (String line in lines) {
      line = line.trim();
      if (_baseSign == null && line.startsWith(RegExp(r'=\w+'))) {
        int idx = line.indexOf(' ');
        if (idx > 0) line = line.substring(0, idx);
        line = line.substring(1);
        _baseSign = line;
      } else if (_baseSign != null) {
        if (_extraControl.text.isEmpty) {
          _extraControl.text = line;
        } else {
          _extraControl.text += "\n$line";
        }
      }
    }
  }

  Widget _createNameSelector() {
    String? val = _signData.getDisplayName();
    if (!_signNames.contains(val)) val = null;
    return DropdownButton<String>(
      items: _signNames.map<DropdownMenuItem<String>>((String value) {
        return DropdownMenuItem<String>(
          value: value,
          child: Text(value),
        );
      }).toList(),
      onChanged: _handleChangeBaseSign,
      value: val,
    );
  }

  void _handleChangeBaseSign(String? name) async {
    if (name == null) return;
    setState(() {
      _signData.setDisplayName(name);
    });
    String cnts = "=$name\n${_extraControl.text}";
    _signData.setContents(cnts);
    _baseSign = name;
    await _previewAction();
  }

  void _handleOtherText(String txt) async {
    if (_baseSign == null) return;
    String cnts = "=$_baseSign\n${_extraControl.text}";
    _signData.setContents(cnts);
    await _previewAction();
  }

  bool _canHaveOtherText() {
    return _baseSign != null;
  }

  Future _previewAction() async {
    Uri url = Uri.https(
      util.getServerURL(),
      "/rest/sign/preview",
    );
    var body = {
      'session': globals.iqsignSession,
      'signdata': _signData.getSignBody(),
      'signuser': _signData.getSignUserId(),
      'signid': _signData.getSignId(),
      'signkey': _signData.getNameKey(),
    };
    var resp = await http.put(url, body: body);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] == 'OK') {
      setState(() {
        _preview = true;
      });
    }
  }

  void _updateAction() async {
    var url = Uri.https(
      util.getServerURL(),
      "/rest/sign/update",
    );
    var body = {
      'session': globals.iqsignSession,
      'signdata': _signData.getSignBody(),
      'signuser': _signData.getSignUserId(),
      'signid': _signData.getSignId(),
      'signkey': _signData.getNameKey(),
    };
    var resp = await http.put(
      url,
      body: body,
    );
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] == "OK") {
      setState(() {
        _preview = false;
      });
    }
  }

  bool _isSignValid() {
    if (_baseSign == null) return false;
    return true;
  }
}
