/*
 *        widgets.dart  
 * 
 *    Common code for creating widgets
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
///*******************************************************************************/

import 'package:flutter/material.dart';

ThemeData getTheme() {
  return ThemeData(
    primarySwatch: Colors.brown,
  );
}

Widget textFormField({
  String? hint,
  String? label,
  TextEditingController? controller,
  ValueChanged<String>? onChanged,
  VoidCallback? onEditingComplete,
  ValueChanged<String>? onSubmitted,
  String? Function(String?)? validator,
  bool? showCursor,
  int? maxLines,
  TextInputType? keyboardType,
  bool obscureText = false,
}) {
  label ??= hint;
  hint ??= label;
  if (obscureText) maxLines = 1;
  return TextFormField(
    decoration: InputDecoration(
      hintText: hint,
      labelText: label,
      border: const OutlineInputBorder(),
    ),
    validator: validator,
    controller: controller,
    onChanged: onChanged,
    onEditingComplete: onEditingComplete,
    onFieldSubmitted: onSubmitted,
    showCursor: showCursor,
    maxLines: maxLines,
    obscureText: obscureText,
  );
}

TextField textField({
  String? hint,
  String? label,
  TextEditingController? controller,
  ValueChanged<String>? onChanged,
  VoidCallback? onEditingComplete,
  ValueChanged<String>? onSubmitted,
  bool? showCursor,
  int? maxLines,
  TextInputType? keyboardType,
  TextInputAction? textInputAction,
}) {
  label ??= hint;
  hint ??= label;
  maxLines ??= 1;
  keyboardType ??=
      (maxLines == 1 ? TextInputType.text : TextInputType.multiline);

  return TextField(
    controller: controller,
    onChanged: onChanged,
    onEditingComplete: onEditingComplete,
    onSubmitted: onSubmitted,
    showCursor: showCursor,
    maxLines: maxLines,
    keyboardType: keyboardType,
    textInputAction: textInputAction,
    decoration: InputDecoration(
      hintText: hint,
      labelText: label,
      border: const OutlineInputBorder(),
    ),
  );
}

Widget errorField(String text) {
  return Text(
    text,
    style: const TextStyle(color: Colors.red, fontSize: 16),
  );
}

Widget submitButton(String name, void Function()? action) {
  return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16.0),
      child: ElevatedButton(
        onPressed: action,
        child: Text(name),
      ));
}

Widget textButton(String label, void Function()? action) {
  return TextButton(
    style: TextButton.styleFrom(
      textStyle: const TextStyle(fontSize: 14),
    ),
    onPressed: action,
    child: Text(label),
  );
}

Widget topMenu(void Function(String)? handler, List labels) {
  return PopupMenuButton(
    icon: const Icon(Icons.menu_sharp),
    itemBuilder: (context) =>
        labels.map<PopupMenuItem<String>>(menuItem).toList(),
    onSelected: handler,
  );
}

PopupMenuItem<String> menuItem(dynamic val) {
  String value = 'Unknown';
  String label = 'Unknown';
  if (val is String) {
    value = val;
    label = val;
  } else if (val is Map<String, String>) {
    for (String k in val.keys) {
      value = k;
      label = val[k] as String;
    }
  }
  return PopupMenuItem<String>(
    value: value,
    child: Text(label),
  );
}

Widget fieldSeparator() {
  return const SizedBox(height: 8);
}

Widget dropDown(List<String> items,
    {String? value, Function(String?)? onChanged}) {
  value ??= items[0];
  return DropdownButton<String>(
    value: value,
    onChanged: onChanged,
    items: items.map<DropdownMenuItem<String>>((String value) {
      return DropdownMenuItem<String>(
        value: value,
        child: Text(value),
      );
    }).toList(),
  );
}

Widget dropDownWidget<T>(List<T> items, String Function(T)? labeler,
    {T? value, Function(T?)? onChanged, String? nullvalue}) {
  String Function(T) lbl = (x) => x.toString();
  if (labeler != null) lbl = labeler;
  if (nullvalue == null) value ??= items[0];
  List<DropdownMenuItem<T?>> itmlst = [];
  if (nullvalue != null) {
    itmlst.add(DropdownMenuItem<T?>(value: null, child: Text(nullvalue)));
  } else {
    value ??= items[0];
  }
  itmlst.addAll(items.map<DropdownMenuItem<T>>((T v) {
    return DropdownMenuItem<T>(
      value: v,
      child: Text(lbl(v)),
    );
  }).toList());

  return DropdownButton<T?>(
    value: value,
    onChanged: onChanged,
    items: itmlst,
  );
}

void goto(BuildContext context, Widget w) {
  // if (!context.mounted) return;
  Navigator.of(context).push(MaterialPageRoute(builder: (context) => w));
}

void gotoDirect(BuildContext context, Widget w) {
  MaterialPageRoute route = MaterialPageRoute(builder: (context) => w);
  Navigator.of(context).pushReplacement(route);
}
