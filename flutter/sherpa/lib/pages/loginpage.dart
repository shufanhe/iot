/*
 * Login Page
 *
 *      Page to let the user login to sherpa
 */
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.                           *
///                                                                              *
///                       All Rights Reserved                                    *
///                                                                              *
///  Permission to use, copy, modify, and distribute this software and its       *
///  documentation for any purpose other than its incorporation into a           *
///  commercial product is hereby granted without fee, provided that the         *
///  above copyright notice appear in all copies and that both that              *
///  copyright notice and this permission notice appear in supporting            *
///  documentation, and that the name of Brown University not be used in         *
///  advertising or publicity pertaining to distribution of the software         *
///  without specific, written prior permission.                                 *
///                                                                              *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS               *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND           *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY     *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY         *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,             *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS              *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE         *
///  OF THIS SOFTWARE.                                                           *
///                                                                              *
///*******************************************************************************/

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/widgets.dart' as widgets;
import 'package:shared_preferences/shared_preferences.dart';
import 'registerpage.dart';
import 'forgotpasswordpage.dart';
import 'splashpage.dart';
import 'changepasswordpage.dart';
import 'package:sherpa/models/catremodel.dart';

//
//    Private Variables
//
bool _loginValid = false;

//
//    Check login using preferences or prior login
//

Future<bool> testLogin() async {
  if (_loginValid) return true;
  SharedPreferences prefs = await SharedPreferences.getInstance();
  String? uid = prefs.getString('uid');
  String? pwd = prefs.getString('pwd');
  if (uid != null && pwd != null) {
    _HandleLogin login = _HandleLogin(uid, pwd);
    String? rslt = await login.authUser();
    if (rslt == null) {
      _loginValid = true;
      return true;
    }
  }
  return false;
}

//
//    Login widget
//

class SherpaLogin extends StatelessWidget {
  const SherpaLogin({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SherPA Login',
      theme: widgets.getTheme(),
      home: const SherpaLoginWidget(),
    );
  }
}

class SherpaLoginWidget extends StatefulWidget {
  const SherpaLoginWidget({super.key});

  @override
  State<SherpaLoginWidget> createState() => _SherpaLoginWidgetState();
}

class _SherpaLoginWidgetState extends State<SherpaLoginWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _curUser;
  String? _curPassword;
  String _loginError = '';
  final TextEditingController _userController = TextEditingController();
  final TextEditingController _pwdController = TextEditingController();
  bool _rememberMe = false;

  _SherpaLoginWidgetState() {
    _loginValid = false;
  }

  @override
  void initState() {
    _loadUserAndPassword();
    super.initState();
  }

  void _gotoHome() {
    widgets.goto(context, const SplashPage());
  }

  void _gotoRegister() {
    widgets.goto(context, const SherpaRegister());
  }

  void _gotoForgotPassword() {
    widgets.goto(context, const SherpaPasswordWidget());
  }

  void _gotoChangePassword(CatreUniverse cu) {
    widgets.goto(context, SherpaChangePasswordWidget(cu));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Login"),
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
                  widgets.fieldSeparator(),
                  widgets.fieldSeparator(),
                  Image.asset(
                    "assets/images/sherpaimage.png",
                    width: MediaQuery.of(context).size.width * 0.5,
                    height: MediaQuery.of(context).size.width * 0.2,
                  ),
                  widgets.fieldSeparator(),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                      hint: "Username",
                      label: "Username",
                      validator: _validateUserName,
                      controller: _userController,
                      fraction: 0.8,
                      context: context),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Password",
                    label: "Password",
                    validator: _validatePassword,
                    controller: _pwdController,
                    obscureText: true,
                    context: context,
                    fraction: 0.8,
                  ),
                  widgets.errorField(_loginError),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 150, maxWidth: 350),
                    width: MediaQuery.of(context).size.width * 0.4,
                    child: widgets.submitButton("Login", _handleLogin),
                  ),
                ],
              ),
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
    _loginValid = false;
    setState(() {
      _loginError = '';
    });
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();
      _HandleLogin login =
          _HandleLogin(_curUser as String, _curPassword as String);
      String? rslt = await login.authUser();
      if (rslt == "TEMPORARY") {
        _loginValid = true;
        CatreModel().loadUniverse().then(_gotoChangePassword);
      } else if (rslt != null) {
        setState(() {
          _loginError = rslt;
        });
      } else {
        _loginValid = true;
        _saveData();
        _gotoHome();
      }
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
    _saveData();
    setState(() {
      _rememberMe = fg;
    });
  }

  void _saveData() {
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('remember_me', _rememberMe);
      prefs.setString('uid', _rememberMe ? _userController.text : "");
      prefs.setString('pwd', _rememberMe ? _pwdController.text : "");
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

//
//    Class to actually handle loggin in
//

class _HandleLogin {
  String? _curPadding;
  String? _curSession;
  final String _curPassword;
  final String _curUser;

  _HandleLogin(this._curUser, this._curPassword);

  Future _prelogin() async {
    var url = Uri.https(util.getServerURL(), '/login');
    var resp = await http.get(url);
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    _curPadding = js['SALT'];
    _curSession = js[globals.catreSession];
    globals.sessionId = _curSession;
  }

  Future<String?> authUser() async {
    if (_curPadding == null) {
      await _prelogin();
    }
    String pwd = _curPassword;
    String usr = _curUser.toLowerCase();
    String pad = _curPadding as String;
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + usr);
    String p3 = util.hasher(p2 + pad);

    var body = {
      globals.catreSession: _curSession,
      'username': usr,
      'SALT': pad,
      'password': p3,
    };
    var url = Uri.https(util.getServerURL(), "/login");
    var resp = await http.post(url, body: body);
    var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (jresp['STATUS'] == "OK") {
      globals.sessionId = _curSession;
      var temp = jresp['TEMPORARY'];
      if (temp) return "TEMPORARY";
      return null;
    }
    return jresp['MESSAGE'];
  }
}

