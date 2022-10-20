/*
 *        forgotpasswordpage.dart  
 * 
 *    Page to ask for password reset email
 * 
 */

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'globals.dart' as globals;
import 'util.dart' as util;
import 'widgets.dart' as widgets;
import 'loginpage.dart';

class IQSignPasswordWidget extends StatefulWidget {
  const IQSignPasswordWidget({super.key});

  @override
  State<IQSignPasswordWidget> createState() => _IQSignPasswordWidgetState();
}

class _IQSignPasswordWidgetState extends State<IQSignPasswordWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _emailGiven;

  _IQSignPasswordWidgetState();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("iQsign Forgot Password"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Image.asset("assets/images/iqsign01.png"),
                  widgets.textFormField("Email", "Email", _validateEmail),
                  widgets.submitButton(
                      "Request Password Email", _handleForgotPassword),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _handleForgotPassword() async {
    final form = _formKey.currentState;
    if (form!.validate()) {
      form.save();
      await _forgotPassword();
      _gotoLogin();
    }
  }

  String? _validateEmail(String? value) {
    _emailGiven = value;
    if (value == null || value.isEmpty) {
      return "Email must not be null";
    } else if (!util.validateEmail(value)) {
      return "Invalid email address";
    }
    return null;
  }

  void _gotoLogin() {
    Navigator.push(
        context, MaterialPageRoute(builder: (context) => const IQSignLogin()));
  }

  Future _forgotPassword() async {
    String em = (_emailGiven as String).toLowerCase();
    var body = {
      'session': globals.sessionId,
      'email': em,
    };
    var url = Uri.https("sherpa.cs.brown.edu:3336", "/rest/forgotpassword");
    await http.post(url, body: body);
  }
}
