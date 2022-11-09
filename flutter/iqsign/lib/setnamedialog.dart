/*  
 *       setnamedialog.dart 
 * 
 *    Dialog for changing sign name
 * 
 */

import 'widgets.dart' as widgets;
import 'package:flutter/material.dart';
import 'signdata.dart';

Future setNameDialog(BuildContext context, SignData sd) async {
  String name = sd.getName();
  TextEditingController controller = TextEditingController(text: name);
  BuildContext dcontext = context;

  void cancel() {
    Navigator.of(dcontext).pop("CANCEL");
  }

  void accept() {
    sd.setName(name);
    Navigator.of(dcontext).pop("OK");
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget acceptBtn = widgets.submitButton("OK", accept);

  AlertDialog dlg = AlertDialog(
      title: const Text("Set Sign Name"),
      content: widgets.textField(label: "Sign Name", controller: controller),
      actions: <Widget>[cancelBtn, acceptBtn]);

  return showDialog(
      context: context,
      builder: (context) {
        dcontext = context;
        return dlg;
      });
}
