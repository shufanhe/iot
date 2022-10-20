/*  
 *       setnamedialog.dart 
 * 
 *    Dialog for changing sign name
 * 
 */

import 'widgets.dart' as widgets;
import 'package:flutter/material.dart';
import 'signdata.dart';

class _IQSignSetNameDialog extends AlertDialog {
  final BuildContext context;
  final SignData _signData;
  final TextEditingController _nameController;

  _IQSignSetNameDialog(this.context, this._signData, this._nameController,
      Function() cancelCallback, Function() okCallback)
      : super(
            title: const Text("Set Sign Name"),
            content: widgets.textField(
                label: "Sign Name", controller: _nameController),
            actions: <Widget>[
              widgets.submitButton("Cancel", cancelCallback),
              widgets.submitButton("OK", okCallback),
            ]) {
    _nameController.text = _signData.getName();
  }
}

Future<String> setNameDialog(BuildContext context, SignData sd) async {
  TextEditingController controller = TextEditingController();
  String name = sd.getName();
  await showDialog(
    context: context,
    builder: (context) {
      return _IQSignSetNameDialog(context, sd, controller, () {
        Navigator.pop(context);
      }, () {
        name = controller.text;
        Navigator.pop(context);
      });
    },
  );
  return name;
}
