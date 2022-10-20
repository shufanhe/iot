/*
 *        homepage.dart
 * 
 *    Home page for a user
 * 
 */

import 'util.dart' as util;
import 'widgets.dart' as widgets;
import 'signdata.dart';
import 'signpage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import 'globals.dart' as globals;
import 'loginpage.dart';

class IQSignHomeWidget extends StatelessWidget {
  const IQSignHomeWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'iQsign Home Page',
      theme: util.getTheme(),
      home: const IQSignHomePage(),
    );
  }
}

class IQSignHomePage extends StatefulWidget {
  const IQSignHomePage({super.key});

  @override
  State<IQSignHomePage> createState() => _IQSignHomePageState();
}

class _IQSignHomePageState extends State<IQSignHomePage> {
  List<SignData> _signData = [];

  _IQSignHomePageState() {
    _getSigns();
  }

  Future _getSigns() async {
    var url = Uri.https('sherpa.cs.brown.edu:3336', '/rest/signs',
        {'session': globals.sessionId});
    var resp = await http.get(url);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    var jsd = js['data'];
    var rslt = <SignData>[];
    for (final sd1 in jsd) {
      SignData sd = SignData(sd1);
      rslt.add(sd);
    }
    setState(() {
      _signData = rslt;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("iQsign Home Page"),
        actions: [
          widgets.topMenu(_handleCommand, [
            {'AddSign': 'Create New Sign'},
            {'Logout': "Log Out"},
          ]),
        ],
      ),
      body: Center(
        child: _signData.isNotEmpty
            ? ListView.builder(
                padding: const EdgeInsets.all(10.0),
                itemCount: _signData.length,
                itemBuilder: _getTile,
              )
            : const Text("Signs pending"),
      ),
    );
  }

  ListTile _getTile(context, int i) {
    SignData sd = _signData[i];
    return ListTile(
      leading: Text(
        sd.getName(),
        style: const TextStyle(
          fontSize: 20,
        ),
      ),
      title: Text(
        sd.getDisplayName(),
        style: const TextStyle(
          fontSize: 14,
        ),
      ),
      trailing: Container(
        decoration: BoxDecoration(
          border: Border.all(
            width: 5,
          ),
        ),
        child: Image.network(sd.getImageUrl()),
      ),
      onTap: () => {
        Navigator.of(context).push(
            MaterialPageRoute<void>(builder: (context) => IQSignSignWidget(sd)))
      },
    );
  }

  void _handleCommand(String cmd) {
    switch (cmd) {
      case "AddSign":
        break;
      case "Logout":
        _handleLogout().then(_gotoLogin);
        break;
    }
  }

  dynamic _gotoLogin(dynamic) {
    Navigator.push(
        context, MaterialPageRoute(builder: (context) => const IQSignLogin()));
  }

  Future _handleLogout() async {
    var url = Uri.https("sherpa.cs.brown.edu:3336", "/rest/logout");
    await http.post(url);
  }
}
