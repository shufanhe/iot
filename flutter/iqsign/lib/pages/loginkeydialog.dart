/*  
 *       setnamedialog.dart 
 * 
 *    Dialog for changing sign name
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

import 'dart:async';

import '../widgets.dart' as widgets;
import 'package:flutter/material.dart';
import '../signdata.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import '../util.dart' as util;
import 'package:flutter/services.dart';

Future loginKeyDialog(BuildContext context, SignData sd) async {
  Uri url = Uri.https(
    util.getServerURL(),
    "/rest/createcode",
  );
  var body = {
    'signuser': sd.getSignUserId().toString(),
    'signid': sd.getSignId().toString(),
    'signkey': sd.getNameKey(),
  };
  http.post(url, body: body).then((resp) {
    var js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (js['status'] != 'OK') {
      Navigator.of(context).pop("CANCEL");
    }
    String code = js['code'];
    return loginKeyDialog1(context, sd, code);
  });
}

Future loginKeyDialog1(BuildContext context, SignData sd, String code) async {
  void handleOk() {
    Navigator.of(context).pop("OK");
  }

  void accept() {
    Clipboard.setData(ClipboardData(text: code))
        .then(handleOk as FutureOr Function(void value));
  }

  Widget acceptBtn = widgets.submitButton("OK", accept);

  Dialog dlg = Dialog(
    child: Padding(
      padding: const EdgeInsets.all(20.0),
      child: SizedBox(
        width: MediaQuery.of(context).size.width * 0.8,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text(
              "Login Code: ",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 15),
            Text(
              code,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 15),
            const Text("Code will be posted to clipboard"),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [acceptBtn],
            )
          ],
        ),
      ),
    ),
  );

  return showDialog(
      context: context,
      builder: (context) {
        return dlg;
      });
}
