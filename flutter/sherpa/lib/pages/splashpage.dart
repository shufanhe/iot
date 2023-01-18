/*
 *        Splash page
 */

import 'package:sherpa/widgets.dart' as widgets;
import 'package:flutter/material.dart';
import 'loginpage.dart' as login;

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
    setStep("Checking for saved login");
    login.testLogin().then((bool value) {
      if (!value) {
        widgets.goto(context, const login.SherpaLoginWidget());
        return;
      }
      setStep("Loading current universe");
      print("WE ARE LOGGED IN");
    });
    // now build model
    // go to home page
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("SherPA start up"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Image.asset("assets/images/sherpaimage.png"),
            widgets.fieldSeparator(),
            Text(_curStep, textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}
