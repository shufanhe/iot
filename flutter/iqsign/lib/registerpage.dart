/*
 * Registration Page
 */

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'globals.dart' as globals;
import 'util.dart' as util;
import 'widgets.dart' as widgets;
import 'loginpage.dart';

class IQSignRegister extends StatelessWidget {
  const IQSignRegister({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'iQSign Registration',
      theme: util.getTheme(),
      home: const IQSignRegisterWidget(),
    );
  }
}

class IQSignRegisterWidget extends StatefulWidget {
  const IQSignRegisterWidget({super.key});

  @override
  State<IQSignRegisterWidget> createState() => _IQSignRegisterWidgetState();
}

class _IQSignRegisterWidgetState extends State<IQSignRegisterWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _curUser;
  String? _curEmail;
  String? _curPassword;
  String? _signName;
  late String _registerError;

  _IQSignRegisterWidgetState() {
    _curUser = null;
    _curEmail = null;
    _curPassword = null;
    _signName = null;
    _registerError = '';
  }

  Future<String?> _registerUser() async {
    String pwd = (_curPassword as String);
    String usr = (_curUser as String).toLowerCase();
    String? em = _curEmail;
    if (em == null || em.isEmpty) em = usr;
    String email = em.toLowerCase();
    String sign = (_signName as String);
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + usr);
    String p3 = util.hasher(p1 + email);

    var body = {
      'session': globals.sessionId,
      'email': email,
      'username': usr,
      'password': p3,
      'altpassword': p2,
      'signname': sign,
    };
    var url = Uri.https("sherpa.cs.brown.edu:3336", "/rest/register");
    var resp = await http.post(url, body: body);
    var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (jresp['status'] == "OK") return null;
    return jresp['message'];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("iQsign Registration"),
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
                  widgets.textFormField(
                      "Valid Email Address", "Email", _validateEmail),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                      "Username", "Username", _validateUserName),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                      "Password", "Password", _validatePassword, null, true),
                  widgets.fieldSeparator(),
                  widgets.textFormField("Confirm Password", "Confirm Passwrd",
                      _validateConfirmPassword, null, true),
                  widgets.fieldSeparator(),
                  widgets.textFormField("Sign Name (e.g. Office)",
                      "Name of First Sign", _validateSignName),
                  widgets.errorField(_registerError),
                  widgets.submitButton("Submit", _handleRegister),
                ],
              ),
            ),
            widgets.textButton("Already a user, login", _gotoLogin),
          ],
        ),
      ),
    );
  }

  void _handleRegister() async {
    setState(() {
      _registerError = '';
    });
    if (_formKey.currentState!.validate()) {
      String? rslt = await _registerUser();
      if (rslt != null) {
        setState(() {
          _registerError = rslt;
        });
      }
      _gotoLogin();
    }
  }

  void _gotoLogin() {
    Navigator.push(
        context, MaterialPageRoute(builder: (context) => const IQSignLogin()));
  }

  String? _validateSignName(String? value) {
    _signName = value;
    if (value == null || value.isEmpty) {
      return "Must provide a name for initial sign";
    }
    return null;
  }

  String? _validateConfirmPassword(String? value) {
    if (value != _curPassword) {
      return "Passwords must match";
    }
    return null;
  }

  String? _validatePassword(String? value) {
    _curPassword = value;
    if (value == null || value.isEmpty) {
      return "Password must not be null";
    } else if (!util.validatePassword(value)) {
      return "Invalid password";
    }
    return null;
  }

  String? _validateUserName(String? value) {
    _curUser = value;
    return null;
  }

  String? _validateEmail(String? value) {
    _curEmail = value;
    if (value == null || value.isEmpty) {
      return "Email must not be null";
    } else if (!util.validateEmail(value)) {
      return "Invalid email address";
    }
    return null;
  }
}
