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

import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flutter/material.dart';
import '../globals.dart' as globals;
import '../widgets.dart' as widgets;
import '../util.dart' as util;

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
  final TextEditingController _controller = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  List<String> _signNames = [];
  late Future<List<String>> _signNamesFuture;
  String _selectedSignName = '';

  _IQSignSignCreatePageState();

  @override
  void initState() {
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
      _selectedSignName = _signNames[0];
    });

    return rslt;
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
    var resp = await http.post(url, body: {
      'session': globals.sessionId,
      'name': _nameController.text,
      'signname': _selectedSignName,
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] != "OK") {
      // handle errors here
    }
    Navigator.pop(context, true);
  }

  Widget _createNameSelector() {
    return FutureBuilder<List<String>>(
      future: _signNamesFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return widgets.circularProgressIndicator();
        } else if (snapshot.hasError) {
          return Text('Error: ${snapshot.error}');
        } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
          return const Text('No data available');
        } else {
          return DropdownButton<String>(
            items: snapshot.data!.map<DropdownMenuItem<String>>((String value) {
              return DropdownMenuItem<String>(
                value: value,
                child: Text(value),
              );
            }).toList(),
            onChanged: (String? value) async {
              if (value != null) {
                setState(() {
                  _selectedSignName = value;
                });
              }
            },
            value: _selectedSignName,
          );
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Create New Sign",
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
            Row(mainAxisAlignment: MainAxisAlignment.center, children: <Widget>[
              const Text("Set Sign to "),
              _createNameSelector(),
            ]),
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
