/*
 *        Splash page
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
///*******************************************************************************/

import 'dart:async';

import 'package:sherpa/widgets.dart' as widgets;
import 'package:flutter/material.dart';
import 'loginpage.dart' as login;
import 'programpage.dart' as programpage;
import 'package:sherpa/models/catremodel.dart';

String _curStep = "";

class SplashPage extends StatelessWidget {
  const SplashPage({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SHERPA Start...',
      theme: widgets.getTheme(),
      home: const SplashWidget(),
    );
  }
}

class SplashWidget extends StatefulWidget {
  const SplashWidget({super.key});

  @override
  State<SplashWidget> createState() => _SplashWidgetState();
}

class _SplashWidgetState extends State<SplashWidget> {
  _SplashWidgetState();

  @override
  void initState() {
    super.initState();
    splashTasks();
  }

  void setStep(String step) {
    setState(() {
      _curStep = step;
    });
  }

  void splashTasks() {
    setStep("Checking for saved login...");
    login.testLogin().then((bool value) async {
      if (!value) {
        widgets.gotoDirect(context, const login.SherpaLoginWidget());
        return;
      }
      setStep("Loading current universe...");
      CatreModel().loadUniverse().then(_gotoProgram);
    });
    // now build model
    // go to home page
  }

  Future<void> _gotoProgram(CatreUniverse cu) async {
    // if number of rules is small, go to program page directly
    widgets.gotoDirect(context, programpage.SherpaProgramWidget(cu));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(""),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Image.asset(
              "assets/images/sherpaimage.png",
              width: MediaQuery.of(context).size.width * 0.9,
              height: null,
            ),
            widgets.fieldSeparator(),
            Text(
              _curStep,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
