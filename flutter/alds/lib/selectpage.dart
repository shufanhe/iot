/*
 *      selectpage.dart 
 *    
 *    Main page for specifying/viewing/selecting room
 * 
 */

import 'package:flutter/material.dart';

import 'storage.dart' as storage;
import 'util.dart' as util;
import 'widgets.dart' as widgets;

class AldsSelect extends StatelessWidget {
  const AldsSelect({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ALDS Location Selector',
      theme: util.getTheme(),
      home: const AldsSelectWidget(),
    );
  }
}

class AldsSelectWidget extends StatefulWidget {
  const AldsSelectWidget({super.key});

  @override
  State<AldsSelectWidget> createState() => _AldsSelectWidgetState();
}

class _AldsSelectWidgetState extends State<AldsSelectWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _curController = TextEditingController();

  _AldsSelectWidgetState();

  @override
  void initState() {
    super.initState();
    _curController.text = "Other";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text("ALDS Location Selector"),
          actions: [
            widgets.topMenu(_handleCommand, [
              {'ShowLoginData': 'Show Login Data'},
              {'EditLocations': 'Edit Locations'}
            ]),
          ],
        ),
        body: Center(
            child: RefreshIndicator(
                onRefresh: _handleUpdate,
                child: ListView(children: <Widget>[
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      const Text("Current Location:   "),
                      Expanded(
                          child: widgets.textField(
                              controller: _curController, readOnly: true)),
                    ],
                  ),
                  widgets.fieldSeparator(),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      const Text('Alternatives:  '),
                      Expanded(child: _createLocationSelector())
                    ],
                  ),
                  widgets.submitButton("Validate", _handleValidate),
                ]))));
  }

  Widget _createLocationSelector() {
    return widgets.dropDown(storage.getLocations(),
        value: _curController.text, onChanged: _locationSelected);
  }

  Future<void> _locationSelected(String? value) async {
    if (value != null) {
      print("SET CURRENT TO $value");
      setState(() => {_curController.text = value});
    }
  }

  void _handleCommand(String cmd) async {
    switch (cmd) {
      case "ShowLoginData":
        break;
      case 'EditLocations':
        break;
    }
  }

  void _handleValidate() async {
    String txt = _curController.text;
    print("VALIDATE location as $txt");
  }

  Future<void> _handleUpdate() async {
    print("REFRESH REQUEST");
  }
}
