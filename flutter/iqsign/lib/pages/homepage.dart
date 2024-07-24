/*
 *        homepage.dart
 * 
 *    Home page for a user
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

import 'package:iqsign/pages/createsignpage.dart';

import '../util.dart' as util;
import '../widgets.dart' as widgets;
import '../signdata.dart';
import 'signpage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import '../globals.dart' as globals;
import 'loginpage.dart';

Future<List<SignData>> getSigns() async {
  var url = Uri.https(util.getServerURL(), '/rest/signs', {
    'session': globals.iqsignSession,
  });
  var resp = await http.get(url);
  var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
  var rslt = <SignData>[];
  if (js['status'] == 'OK') {
    var jsd = js['data'];
    for (final sd1 in jsd) {
      SignData sd = SignData(sd1);
      rslt.add(sd);
    }
  }
  return rslt;
}

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

  @override
  void initState() {
    _getSigns();
    super.initState();
  }

  Future _getSigns() async {
    List<SignData> rslt = await getSigns();
    setState(() {
      _signData = rslt;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        flexibleSpace: const Image(
          image: AssetImage('assets/images/iqsignstlogo.png'),
          fit: BoxFit.contain,
        ),
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
            : widgets.circularProgressIndicator(),
      ),
    );
  }

  ListTile _getTile(context, int i) {
    SignData sd = _signData[i];
    return ListTile(
      title: Text(
        sd.getName(),
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),
      subtitle: Text(
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
        child: Image.network(sd.getLocalImageUrl()),
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
        _goToCreateSign();
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
    var url = Uri.https(util.getServerURL(), "/rest/logout");
    await http.post(url);
  }

  dynamic _goToCreateSign() {
    Navigator.push(context,
        MaterialPageRoute(builder: (context) => const IQSignSignCreatePage()));
  }
}
