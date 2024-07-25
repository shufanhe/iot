/*  
 *       setsizedialog.dart 
 * 
 *    Dialog for changing sign size
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

import '../widgets.dart' as widgets;
import 'package:flutter/material.dart';
import '../signdata.dart';

const List<String> _dims = <String>['16by9', '4by3', '16by10', 'other'];

class _IQSignSetSizeDialog extends AlertDialog {
  final BuildContext context;

  _IQSignSetSizeDialog(
      this.context, signData, widthController, heightController, void Function(int, String?) changeCallback)
      : super(
            title: const Text("Set Sign Size"),
            content: Padding(
              padding: const EdgeInsets.all(20.0),
              child: SizedBox(
                width: MediaQuery.of(context).size.width * 0.8,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    widgets.textField(
                        hint: "Width",
                        controller: widthController,
                        keyboardType: TextInputType.number,
                        onEditingComplete: () {
                          changeCallback(0, widthController.text);
                        }),
                    widgets.fieldSeparator(),
                    widgets.textField(
                      hint: "Height",
                      controller: heightController,
                      keyboardType: TextInputType.number,
                      onEditingComplete: () {
                        changeCallback(1, heightController.text);
                      },
                    ),
                    widgets.fieldSeparator(),
                    widgets.dropDownMenu(_dims, value: signData.getDimension(), onChanged: (String? v) {
                      changeCallback(2, v);
                    }),
                  ],
                ),
              ),
            ),
            actions: <Widget>[
              widgets.submitButton("Cancel", () {
                Navigator.of(context).pop("CANCEL");
              }),
              widgets.submitButton("OK", () {
                Navigator.of(context).pop("OK");
              }),
            ]);
}

Future<String?> showSizeDialog(BuildContext context, SignData sd) async {
  TextEditingController widthController = TextEditingController(text: sd.getWidth().toString());
  TextEditingController heightController = TextEditingController(text: sd.getHeight().toString());
  String dim = sd.getDimension();
  int wd = sd.getWidth();
  int ht = sd.getHeight();
  int lastType = 0;

  void changeCallback(int type, String? v) {
    if (type == 2) {
      dim != v;
      double f = fract(dim);
      if (f != 0) {
        if (lastType == 0) {
          ht = (wd * f) as int;
          heightController.text = ht.toString();
        } else {
          wd = (ht / f) as int;
          widthController.text = wd.toString();
        }
      }
    } else {
      double f = fract(dim);
      if (type == 0) {
        wd != v as int;
        if (f != 0) {
          ht = (wd * f) as int;
          heightController.text = ht.toString();
        }
      } else {
        ht != v as int;
        if (f != 0) {
          wd = (ht / f) as int;
          widthController.text = wd.toString();
        }
      }
      lastType = type;
    }
  }

  String? rslt = await showDialog(
    context: context,
    builder: (context) {
      return _IQSignSetSizeDialog(context, sd, widthController, heightController, changeCallback);
    },
  );
  if (rslt == "OK") {
    sd.setSize(wd, ht, dim);
  }
  return rslt;
}

double fract(String dim) {
  switch (dim) {
    case "16by9":
      return 9 / 16;
    case "4by3":
      return 3 / 4;
    case "16by10":
      return 10 / 16;
    default:
      return 0;
  }
}
