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
  final TextEditingController _controller = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  bool _replace = false;
  late TextField _nameField;
  bool _updateName = false;

  _IQSignSignEditPageState();

  @override
  void initState() {
    _signData = widget._signData;
    _knownNames = widget._signNames;
    _signNames = ['Current Sign', ...widget._signNames];
    _nameController.text = _signData.getDisplayName();
    _controller.text = _signData.getSignBody();
    _replace = _knownNames.contains(_signData.getDisplayName());
    _nameField = widgets.textField(
        label: "SignName",
        controller: _nameController,
        onSubmitted: _nameChanged);
    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void editSubmit(String v) {
    print("submitted");
    // _signData.setContents(v);
  }

  void editUpdate(String v) {
    // _signData.setContents(v);
  }

  void editComplete() {
    print("complete");
  }

  void focusChange(bool fg) async {
    if (!fg) {
      _signData.setContents(_controller.text);
      await _updateSign();
    }
    print("focus $fg");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("iQsign Sign ${_signData.getName()}",
            style: const TextStyle(
                fontWeight: FontWeight.bold, color: Colors.black)),
        actions: [
          widgets.topMenu(_handleCommand, [
            {'MyImages': "Browse My Images"},
            {'FAImages': "Browse Font Awesome Images"},
            {'SVGImages': "Browse Image Library"},
            {'EditSize': "Change Sign Size"},
            {'ChangeName': "Change Sign Name"},
            {'AddImage': "Add New Image to Image Library"},
          ]),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            Container(
              padding: const EdgeInsets.all(20.0),
              width: MediaQuery.of(context).size.width * 0.8,
              child: Focus(
                onFocusChange: focusChange,
                child: widgets.textField(
                    controller: _controller,
                    maxLines: 8,
                    showCursor: true,
                    onEditingComplete: editComplete,
                    onSubmitted: editSubmit,
                    onChanged: editUpdate),
              ),
            ),
            widgets.fieldSeparator(),
            Container(
              padding: const EdgeInsets.all(20.0),
              width: MediaQuery.of(context).size.width * 0.8,
              child: Focus(
                onFocusChange: focusChange,
                child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      const Text("Start with:         "),
                      Expanded(child: _createNameSelector(val: 'Current Sign')),
                    ]),
              ),
            ),
            widgets.fieldSeparator(),
            Container(
              padding: const EdgeInsets.all(20.0),
              width: MediaQuery.of(context).size.width * 0.8,
              child: Focus(
                onFocusChange: focusChange,
                child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Text(_replace
                          ? "Replace Sign:      "
                          : "Create Sign:      "),
                      Expanded(child: _nameField),
                    ]),
              ),
            ),
            Container(
              constraints: const BoxConstraints(minWidth: 150, maxWidth: 350),
              width: MediaQuery.of(context).size.width * 0.4,
              child: widgets.submitButton("Update", _handleUpdate),
            ),
            Image.network(_signData.getImageUrl()),
          ],
        ),
      ),
    );
  }

  void _handleCommand(String cmd) async {
    switch (cmd) {
      case "MyImages":
        var uri = Uri.https(util.getServerURL(), "/rest/savedimages",
            {'session': globals.sessionId});
        await _launchURI(uri);
        break;
      case "FAImages":
        await _launchURL("https://fontawesome.com/search?m=free&s=solid");
        break;
      case "SVGImages":
        var uri1 = Uri.https(util.getServerURL(), "/rest/svgimages",
            {'session': globals.sessionId});
        await _launchURI(uri1);
        break;
      case "AddImage":
        break;
      case "EditSize":
        final result = await setsize.showSizeDialog(context, _signData);
        if (result == "OK") updateDisplay(_signData);
        break;
      case "ChangeName":
        final result = await setname.setNameDialog(context, _signData);
        if (result == "OK") updateDisplay(_signData);
        break;
    }
  }

  Widget _createNameSelector({String? val}) {
    val ??= _signData.getDisplayName();
    return DropdownButton<String>(
      items: _signNames.map<DropdownMenuItem<String>>((String value) {
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

  Future _setSignToSaved(String name) async {
    if (name == "Current Sign") name = "*Current*";
    var url = Uri.https(
      util.getServerURL(),
      "/rest/loadsignimage",
    );
    var resp = await http.post(url, body: {
      'session': globals.sessionId,
      'signname': name,
      'signid': _signData.getSignId().toString(),
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] == "OK") {
      String cnts = js['contents'] as String;
      String sname = js['name'] as String;
      setState(() => {_updateText(sname, cnts)});
    }
  }

  void updateDisplay(SignData? sd) {
    setState(() {
      _signData != sd;
    });
  }

  void _updateText(String name, String cnts) {
    _nameController.text = name;
    _controller.text = cnts;
  }

  Future _saveSignImage(String name) async {
    var url = Uri.https(
      util.getServerURL(),
      "/rest/savesignimage",
    );
    var resp = await http.post(url, body: {
      'session': globals.sessionId,
      'name': name,
      'signid': _signData.getSignId().toString(),
      'signnamekey': _signData.getNameKey(),
      'signuser': _signData.getSignUserId().toString(),
    });
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] != "OK") {
      // handle errors here
    }
  }

  void _handleUpdate() async {
    _updateName = true;
    await _handleUpdateWork();
    setState(() => {
          () {
            _signData.getImageUrl();
          }
        });
  }

  Future _handleUpdateWork() async {
    String name = _nameController.text;
    String cnts = _controller.text;
    if (cnts == "") return;
    _signData.setContents(cnts);
    _signData.setDisplayName(name);
    if (name != '' && !_knownNames.contains(name)) {
      _knownNames.add(name);
    }
    await _updateSign();
    if (name != '') {
      await _saveSignImage(name);
    }
  }

  Future _updateSign() async {
    var url = Uri.https(
      util.getServerURL(),
      "/rest/sign/${_signData.getSignId()}/update",
    );
    var resp = await http.post(url, body: {
      'session': globals.sessionId,
      'signname': _signData.getName(),
      'signid': _signData.getSignId().toString(),
      'signkey': _signData.getNameKey(),
      'signuser': _signData.getSignUserId().toString(),
      'signwidth': _signData.getWidth().toString(),
      'signheight': _signData.getHeight().toString(),
      'signdim': _signData.getDimension(),
      'signdata': _signData.getSignBody(),
    });
    print(_signData.getSignBody());
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] != "OK") {}
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
    _updateName = true;
    bool kn = false;
    if (val != "") {
      kn = _knownNames.contains(val);
    }
    if (kn != _replace) {
      setState(() => _replace = kn);
    }
  }
}
