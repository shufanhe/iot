/*
 *        forgotpasswordpage.dart  
 * 
 *    Page to ask for password reset email
 * 
 */

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/widgets.dart' as widgets;
import 'loginpage.dart';

class SherpaPasswordWidget extends StatefulWidget {
  const SherpaPasswordWidget({super.key});

  @override
  State<SherpaPasswordWidget> createState() => _SherpaPasswordWidgetState();
}

class _SherpaPasswordWidgetState extends State<SherpaPasswordWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _emailGiven;

  _SherpaPasswordWidgetState();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Sherpa Forgot Password"),
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
                  Image.asset("assets/images/sherpaimage.png"),
                  widgets.textFormField(
                      hint: "Email", label: "Email", validator: _validateEmail),
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
    widgets.goto(context, const SherpaLogin());
  }

  Future _forgotPassword() async {
    String em = (_emailGiven as String).toLowerCase();
    var body = {
      globals.catreSession: globals.sessionId,
      'email': em,
    };
    var url = Uri.https(globals.catreURL, "/forgotpassword");
    await http.post(url, body: body);
  }
}
