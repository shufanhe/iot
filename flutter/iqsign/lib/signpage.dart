/*
 *        signpage.dart
 * 
 *    Page for a single sign
 * 
 */

import 'signdata.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import 'globals.dart' as globals;
import 'widgets.dart' as widgets;
import 'loginpage.dart';
import 'editsignpage.dart';

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

  _IQSignSignPageState();

  @override
  void initState() {
    _signData = widget._signData;
    _getNames();
    super.initState();
  }

  Future _getNames() async {
    var url = Uri.https('sherpa.cs.brown.edu:3336', "/rest/namedsigns",
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
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Image.network(_signData.getImageUrl()),
            Row(mainAxisAlignment: MainAxisAlignment.center, children: <Widget>[
              const Text("Set Sign to "),
              _createNameSelector(),
            ]),
          ],
        ),
      ),
    );
  }

  dynamic _gotoLogin(dynamic) {
    Navigator.push(
        context, MaterialPageRoute(builder: (context) => const IQSignLogin()));
  }

  Future _handleLogout() async {
    var url = Uri.https("sherpa.cs.brown.edu:3336", "/rest/logout");
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
      'sherpa.cs.brown.edu:3336',
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
