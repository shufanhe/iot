/*
 * Registration Page
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
        title: const Text("Sign Up"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  SizedBox(
                    width: MediaQuery.of(context).size.width * 0.4,
                    child: Center(
                      child: Image.asset(
                        "assets/images/iqsignstlogo.png",
                        fit: BoxFit.contain,
                      ),
                    ),
                  ),
                  const Padding(
                    padding: EdgeInsets.all(16.0),
                  ),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 100, maxWidth: 600),
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: widgets.textFormField(
                        hint: "Valid Email Address",
                        label: "Email",
                        validator: _validateEmail),
                  ),
                  widgets.fieldSeparator(),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 100, maxWidth: 600),
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: widgets.textFormField(
                        hint: "Username",
                        label: "Username",
                        validator: _validateUserName),
                  ),
                  widgets.fieldSeparator(),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 100, maxWidth: 600),
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: widgets.textFormField(
                        hint: "Password",
                        label: "Password",
                        validator: _validatePassword,
                        obscureText: true),
                  ),
                  widgets.fieldSeparator(),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 100, maxWidth: 600),
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: widgets.textFormField(
                        hint: "Confirm Password",
                        label: "Confirm Password",
                        validator: _validateConfirmPassword,
                        obscureText: true),
                  ),
                  widgets.fieldSeparator(),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 100, maxWidth: 600),
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: widgets.textFormField(
                        hint: "Sign Name (e.g. Office)",
                        label: "Name of First Sign",
                        validator: _validateSignName),
                  ),
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
