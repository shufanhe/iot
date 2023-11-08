/*
 *        createsignpage.dart
 * 
 *    Page for creating a sign
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

import 'package:shared_preferences/shared_preferences.dart';

import 'signdata.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import 'globals.dart' as globals;
import 'widgets.dart' as widgets;
import 'package:url_launcher/url_launcher.dart';
import 'setnamedialog.dart' as setname;
import 'setsizedialog.dart' as setsize;
import 'util.dart' as util;

class IQSignSignCreateWidget extends StatelessWidget {
  const IQSignSignCreateWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const IQSignSignCreatePage();
  }
}

class IQSignSignCreatePage extends StatefulWidget {
  const IQSignSignCreatePage({super.key});

  @override
  State<IQSignSignCreatePage> createState() => _IQSignSignCreatePageState();
}

class _IQSignSignCreatePageState extends State<IQSignSignCreatePage> {
  SignData _signData = SignData.unknown();
  List<String> _signNames = [];
  List<String> _knownNames = [];
  final TextEditingController _controller = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  bool _replace = false;
  late TextField _nameField;
  bool _updateName = false;

  _IQSignSignCreatePageState();

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future handleCreate() async {
    var url = Uri.https(
      util.getServerURL(),
      "/rest/addsign",
    );
    SharedPreferences prefs = await SharedPreferences.getInstance();
    var resp = await http.post(url, body: {
      'session': globals.sessionId,
      'name': prefs.getString('uid'),
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] != "OK") {
      // handle errors here
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("iQsign Create Sign Page",
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black)),
      ),
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Container(
              padding: const EdgeInsets.all(20.0),
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Expanded(
                      child: widgets.textField(
                        label: "Sign Name",
                        controller: _nameController,
                      ),
                    ),
                  ]),
            ),
            widgets.fieldSeparator(),
            Container(
              constraints: const BoxConstraints(minWidth: 150, maxWidth: 350),
              width: MediaQuery.of(context).size.width * 0.4,
              child: widgets.submitButton("Create", handleCreate),
            ),
          ],
        ),
      ),
    );
  }
}
