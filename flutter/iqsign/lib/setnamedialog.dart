/*  
 *       setnamedialog.dart 
 * 
 *    Dialog for changing sign name
 * 
 */
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/


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
