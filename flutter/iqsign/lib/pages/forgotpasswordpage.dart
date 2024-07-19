/*
 *        forgotpasswordpage.dart  
 * 
 *    Page to ask for password reset email
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

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../globals.dart' as globals;
import '../util.dart' as util;
import '../widgets.dart' as widgets;
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
      appBar: widgets.appBar("Forgot Password"),
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
                        hint: "Email",
                        label: "Email",
                        validator: _validateEmail),
                  ),
                  const Padding(
                    padding: EdgeInsets.all(16.0),
                  ),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 200, maxWidth: 350),
                    width: MediaQuery.of(context).size.width * 0.4,
                    child: widgets.submitButton(
                        "Request Password Email", _handleForgotPassword),
                  ),
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
    var url = Uri.https(util.getServerURL(), "/rest/forgotpassword");
    await http.post(url, body: body);
  }
}
