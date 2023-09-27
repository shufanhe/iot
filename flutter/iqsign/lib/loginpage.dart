/*
 * Login Page
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
import 'registerpage.dart';
import 'homepage.dart';
import 'forgotpasswordpage.dart';
import 'package:shared_preferences/shared_preferences.dart';

class IQSignLogin extends StatelessWidget {
  const IQSignLogin({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'iQSign Login',
      theme: util.getTheme(),
      home: const IQSignLoginWidget(),
    );
  }
}

class IQSignLoginWidget extends StatefulWidget {
  const IQSignLoginWidget({super.key});

  @override
  State<IQSignLoginWidget> createState() => _IQSignLoginWidgetState();
}

class _IQSignLoginWidgetState extends State<IQSignLoginWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _curUser;
  String? _curPassword;
  String? _curPadding;
  String? _curSession;
  Future? _setupDone;
  late String _loginError;
  final TextEditingController _userController = TextEditingController();
  final TextEditingController _pwdController = TextEditingController();
  bool _rememberMe = false;

  _IQSignLoginWidgetState() {
    _curUser = null;
    _curPassword = null;
    _curPadding = null;
    _curSession = null;
    _loginError = '';
    _setupDone = _prelogin();
  }

  @override
  void initState() {
    _loadUserAndPassword();
    super.initState();
  }

  Future _prelogin() async {
    var url = Uri.https('sherpa.cs.brown.edu:3336', '/rest/login');
    var resp = await http.get(url);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    _curPadding = js['code'];
    _curSession = js['session'];
    globals.sessionId = _curSession;
    // need to set global variable for session
  }

  Future<String?> _authUser() async {
    await _setupDone;
    String pwd = (_curPassword as String);
    String usr = (_curUser as String).toLowerCase();
    String pad = _curPadding as String;
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + usr);
    String p3 = util.hasher(p2 + pad);

    var body = {
      'session': _curSession,
      'username': usr,
      'padding': pad,
      'password': p3,
    };
    var url = Uri.https("sherpa.cs.brown.edu:3336", "/rest/login");
    var resp = await http.post(url, body: body);
    var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (jresp['status'] == "OK") return null;
    return jresp['message'];
  }

  void _gotoHome() {
    Navigator.push(context,
        MaterialPageRoute(builder: (context) => const IQSignHomeWidget()));
  }

  void _gotoRegister() {
    Navigator.push(context,
        MaterialPageRoute(builder: (context) => const IQSignRegister()));
  }

  void _gotoForgotPassword() {
    Navigator.push(context,
        MaterialPageRoute(builder: (context) => const IQSignPasswordWidget()));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("iQsign Login"),
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
                    // height: MediaQuery.of(context).size.height * 0.3,
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
                        hint: "Username or email",
                        label: "Username or email",
                        validator: _validateUserName,
                        controller: _userController),
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
                        controller: _pwdController,
                        obscureText: true),
                  ),
                  widgets.errorField(_loginError),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 100, maxWidth: 300),
                    width: MediaQuery.of(context).size.width * 0.4,
                    child: widgets.submitButton("Login", _handleLogin),
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: <Widget>[
                      Checkbox(
                        value: _rememberMe,
                        onChanged: _handleRememberMe,
                      ),
                      const Text("Remember Me"),
                    ],
                  ),
                ],
              ),
            ),
            const Padding(
              padding: EdgeInsets.all(16.0),
            ),
            widgets.textButton("Not a user? Register Here.", _gotoRegister),
            widgets.textButton("Forgot Password?", _gotoForgotPassword),
          ],
        ),
      ),
    );
  }

  void _handleLogin() async {
    setState(() {
      _loginError = '';
    });
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();
      String? rslt = await _authUser();
      if (rslt != null) {
        setState(() {
          _loginError = rslt;
        });
      }
      _gotoHome();
    }
  }

  String? _validatePassword(String? value) {
    _curPassword = value;
    if (value == null || value.isEmpty) {
      return "Password must not be null";
    }
    return null;
  }

  String? _validateUserName(String? value) {
    _curUser = value;
    if (value == null || value.isEmpty) {
      return "Username must not be null";
    }
    return null;
  }

  void _handleRememberMe(bool? fg) async {
    if (fg == null) return;
    _rememberMe = fg;
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('remember_me', fg);
      prefs.setString('uid', _userController.text);
      prefs.setString('pwd', _pwdController.text);
    });
    setState(() {
      _rememberMe = fg;
    });
  }

  void _loadUserAndPassword() async {
    try {
      SharedPreferences prefs = await SharedPreferences.getInstance();
      var uid = prefs.getString('uid');
      var pwd = prefs.getString('pwd');
      var rem = prefs.getBool('remember_me');
      if (rem != null && rem) {
        setState(() {
          _rememberMe = true;
        });
        _userController.text = uid ?? "";
        _pwdController.text = pwd ?? "";
      }
    } catch (e) {
      _rememberMe = false;
      _userController.text = "";
      _pwdController.text = "";
    }
  }
}
