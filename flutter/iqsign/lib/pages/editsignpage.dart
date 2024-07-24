/*
 *        editsignpage.dart
 * 
 *    Page for editing a sign
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
import 'package:url_launcher/url_launcher.dart';
import '../util.dart' as util;

class IQSignSignEditWidget extends StatelessWidget {
  final SignData _signData;
  final List<String> _signNames;

  const IQSignSignEditWidget(this._signData, this._signNames, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignSignEditPage(_signData, _signNames);
  }
}

class IQSignSignEditPage extends StatefulWidget {
  final SignData _signData;
  final List<String> _signNames;

  const IQSignSignEditPage(this._signData, this._signNames, {super.key});

  @override
  State<IQSignSignEditPage> createState() => _IQSignSignEditPageState();
}

class _IQSignSignEditPageState extends State<IQSignSignEditPage> {
  SignData _signData = SignData.unknown();
  List<String> _signNames = [];
  List<String> _knownNames = [];
  List<String> _refNames = [];
  final TextEditingController _controller = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  bool _preview = false;

  _IQSignSignEditPageState();

  @override
  void initState() {
    _signData = widget._signData;
    _knownNames = widget._signNames;
    _signNames = ['Current Sign', ...widget._signNames];
    _refNames = ['< NONE', ...widget._signNames];
    _nameController.text = _signData.getDisplayName();
    _controller.text = _signData.getSignBody();

    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    bool repl = _knownNames.contains(_nameController.text);
    String accept = repl ? "Update" : "Create";
    String btnname = "$accept Saved Image: ${_nameController.text}";
    TextField namefield = widgets.textField(
      label: "SignName",
      controller: _nameController,
      onChanged: _nameChanged,
    );
    TextField cntsfield = widgets.textField(
      controller: _controller,
      maxLines: 8,
      showCursor: true,
      onChanged: _signUpdated,
    );

    String imageurl = _signData.getLocalImageUrl(_preview);
    return Scaffold(
      appBar: AppBar(
        title: Text("Customize ${_signData.getName()}",
            style: const TextStyle(
                fontWeight: FontWeight.bold, color: Colors.black)),
        actions: [
          widgets.topMenu(_handleCommand, [
            { 'Help': "Sign Instructions"}
            {'MyImages': "Browse My Images"},
            {'FAImages': "Browse Font Awesome Images"},
            {'SVGImages': "Browse Image Library"},
            {'AddImage': "Add New Image to Image Library"},
          ]),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            Image.network(
              imageurl,
              width: MediaQuery.of(context).size.width * 0.4,
              height: MediaQuery.of(context).size.height * 0.3,
            ),
            widgets.fieldSeparator(),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    const Text("Start with:         "),
                    Expanded(child: _createNameSelector()),
                  ]),
            ),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    const Text("Refer to:            "),
                    Expanded(child: _createReferenceSelector()),
                  ]),
            ),
            widgets.fieldSeparator(),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: cntsfield,
            ),
            widgets.fieldSeparator(),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    const Text("Saved Name:    "),
                    Expanded(child: namefield),
                  ]),
            ),
            Container(
              constraints: const BoxConstraints(minWidth: 150, maxWidth: 350),
              width: MediaQuery.of(context).size.width * 0.4,
              child: widgets.submitButton(btnname, _handleUpdate),
            ),
          ],
        ),
      ),
    );
  }

  void _handleCommand(String cmd) async {
    switch (cmd) {
      case "Help" :
        await _launchURL("http://sherpa.cs.brown.edu/iqsign/instructions.html");
        break;
      case "MyImages":
        var uri = Uri.https(util.getServerURL(), "/rest/savedimages",
            {'session': globals.iqsignSession});
        await _launchURI(uri);
        break;
      case "FAImages":
        await _launchURL("https://fontawesome.com/search?m=free&s=solid");
        break;
      case "SVGImages":
        var uri1 = Uri.https(util.getServerURL(), "/rest/svgimages",
            {'session': globals.iqsignSession});
        await _launchURI(uri1);
        break;
      case "AddImage":
        break;
    }
  }

  Widget _createNameSelector({String? val}) {
    List<String> base = _signNames;
    val ??= base.first;
    return DropdownButton<String>(
      items: base.map<DropdownMenuItem<String>>((String value) {
        return DropdownMenuItem<String>(
          value: value,
          child: Text(value),
        );
      }).toList(),
      onChanged: (String? value) async {
        if (value != null) await _setSignToSaved(value);
      },
      value: val,
    );
  }

  Future _setSignToSaved(String? name) async {
    if (name == null) return;
    if (name == "Current Sign") name = "*Current*";
    var url = Uri.https(
      util.getServerURL(),
      "/rest/loadsignimage",
    );
    var resp = await http.post(url, body: {
      'session': globals.iqsignSession,
      'signname': name,
      'signid': _signData.getSignId().toString(),
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] == "OK") {
      String cnts = js['contents'] as String;
      String sname = js['name'] as String;
      setState(() {
        _nameController.text = sname;
        _controller.text = cnts;
      });
      await _previewAction();
    }
  }

  Widget _createReferenceSelector({String? val}) {
    List<String> base = _refNames;
    val ??= base.first;
    return DropdownButton<String>(
      items: base.map<DropdownMenuItem<String>>((String value) {
        return DropdownMenuItem<String>(
          value: value,
          child: Text(value),
        );
      }).toList(),
      onChanged: _setSignToReference,
      value: val,
    );
  }

  Future _setSignToReference(String? name) async {
    if (name == null) return;
    setState(() {
      _controller.text = "=$name";
      _nameController.text = name;
    });

    await _previewAction();
  }

  void _signUpdated(String txt) async {
    await _previewAction();
  }

  Future _saveSignImage(String name, String cnts) async {
    var url = Uri.https(
      util.getServerURL(),
      "/rest/savesignimage",
    );
    var resp = await http.post(url, body: {
      'session': globals.iqsignSession,
      'name': name,
      'signid': _signData.getSignId().toString(),
      'signnamekey': _signData.getNameKey(),
      'signuser': _signData.getSignUserId().toString(),
      'signbody': cnts,
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] != "OK") {
      // handle errors here
    }
  }

  Future _handleUpdate() async {
    String name = _nameController.text;
    String cnts = _controller.text;
    if (cnts.isEmpty) return;

    if (name.isNotEmpty && !_knownNames.contains(name)) {
      _knownNames.add(name);
    }
    if (name.isNotEmpty && cnts.isNotEmpty) {
      await _saveSignImage(name, cnts);
    }
    setState(() => () {});
  }

  Future _launchURL(String url) async {
    Uri uri = Uri.parse(url);
    await _launchURI(uri);
  }

  Future _launchURI(Uri uri) async {
    if (!await launchUrl(uri)) {
      throw "Could not launch $uri";
    }
  }

  void _nameChanged(String val) {
    setState(() {});
  }

  Future _previewAction() async {
    Uri url = Uri.https(
      util.getServerURL(),
      "/rest/sign/preview",
    );
    var body = {
      'session': globals.iqsignSession,
      'signdata': _nameController.text,
      'signuser': _signData.getSignUserId().toString(),
      'signid': _signData.getSignId().toString(),
      'signkey': _signData.getNameKey(),
    };
    var resp = await http.post(url, body: body);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] == 'OK') {
      setState(() {
        _preview = true;
      });
    }
  }
}
