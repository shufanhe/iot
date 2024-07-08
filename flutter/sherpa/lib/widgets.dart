/*
 *	  widgets.dart
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
///  commercial product is hereby granted without fee, provided that the	 *
///  above copyright notice appear in all copies and that both that		 *
///  copyright notice and this permission notice appear in supporting		 *
///  documentation, and that the name of Brown University not be used in	 *
///  advertising or publicity pertaining to distribution of the software	 *
///  without specific, written prior permission.				 *
///										 *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY	 *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE	 *
///  OF THIS SOFTWARE.								 *
///										 *
///*******************************************************************************/

import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as intl;
import 'package:duration_picker/duration_picker.dart';
import 'globals.dart' as globals;
import 'package:flutter_spinbox/material.dart';

///******************************************************************************/
///										*/
///	Text widgets								*/
///										*/
///******************************************************************************/

Widget textFormField({
  String? hint,
  String? label,
  TextEditingController? controller,
  ValueChanged<String>? onChanged,
  VoidCallback? onEditingComplete,
  ValueChanged<String>? onSubmitted,
  String? Function(String?)? onSaved,
  GestureTapCallback? onTap,
  String? Function(String?)? validator,
  bool? showCursor,
  int? maxLines,
  TextInputType? keyboardType,
  bool obscureText = false,
  double fraction = 0,
  BuildContext? context,
}) {
  label ??= hint;
  hint ??= label;
  if (obscureText) maxLines = 1;
  double vpadding = 0.0;
  if (maxLines != null && maxLines > 1) {
    vpadding = 8.0;
  }

  Widget w = TextFormField(
    decoration: getDecoration(
      hint: hint,
      label: label,
      vPadding: vpadding,
    ),
    validator: validator,
    controller: controller,
    onChanged: onChanged,
    onEditingComplete: onEditingComplete,
    onFieldSubmitted: onSubmitted,
    onSaved: onSaved,
    onTap: onTap,
    showCursor: showCursor,
    maxLines: maxLines,
    obscureText: obscureText,
  );
  if (fraction != 0 && context != null) {
    double minw = 100;
    double maxw = 600;
    if (fraction <= 0.4) {
      minw = 150;
      maxw = 350;
    }
    w = Container(
        constraints: BoxConstraints(minWidth: minw, maxWidth: maxw),
        width: MediaQuery.of(context).size.width * fraction,
        child: w);
  }
  return w;
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
    decoration: getDecoration(
      hint: hint,
      label: label,
    ),
  );
}

Widget errorField(String text) {
  return Text(
    text,
    style: const TextStyle(color: Colors.red, fontSize: 16),
  );
}

Widget itemWithMenu<T>(String lbl, List<MenuAction> acts,
    {void Function()? onTap,
    void Function()? onDoubleTap,
    void Function()? onLongPress}) {
  Widget btn = PopupMenuButton(
    icon: const Icon(Icons.menu_sharp),
    itemBuilder: (context) =>
        acts.map<PopupMenuItem<MenuAction>>(menuItemAction).toList(),
    onSelected: (MenuAction act) => act.action(),
  );
  Widget w = Row(
    mainAxisAlignment: MainAxisAlignment.center,
    children: <Widget>[
      btn,
      Expanded(
        child: Text(lbl),
      ),
    ],
  );
  if (onTap == null && onDoubleTap == null) return w;
  Widget w1 = GestureDetector(
    key: Key(lbl),
    onTap: onTap,
    onDoubleTap: onDoubleTap,
    onLongPress: onLongPress,
    child: w,
  );
  return w1;
}

///******************************************************************************/
///										*/
///	Buttons 								*/
///										*/
///******************************************************************************/

Widget submitButton(String name, void Function()? action, {enabled = true}) {
  ButtonStyle style = ElevatedButton.styleFrom(
    backgroundColor: Colors.yellow,
    foregroundColor: Colors.black,
//  overlayColor: Colors.brown,
  );
  if (!enabled) action = null;
  ElevatedButton eb = ElevatedButton(
    onPressed: action,
    style: style,
    child: Text(name),
  );
  return Padding(
    padding: const EdgeInsets.symmetric(
      vertical: 16.0,
      horizontal: 6.0,
    ),
    child: eb,
  );
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

///******************************************************************************/
///										*/
///	Top menus								*/
///										*/
///******************************************************************************/

Widget topMenu(void Function(String)? handler, List labels) {
  return PopupMenuButton(
    icon: const Icon(Icons.menu_sharp),
    itemBuilder: (context) =>
        labels.map<PopupMenuItem<String>>(menuItem).toList(),
    onSelected: handler,
  );
}

Widget topMenuAction(List labels) {
  return PopupMenuButton(
      icon: const Icon(Icons.menu_sharp),
      itemBuilder: (context) =>
          labels.map<PopupMenuItem<MenuAction>>(menuItemAction).toList(),
      onSelected: (dynamic act) => act.action());
}

PopupMenuItem<MenuAction> menuItemAction(dynamic val) {
  return PopupMenuItem<MenuAction>(
    value: val,
    child: Text(val.label),
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

class MenuAction {
  String label;
  void Function() action;
  MenuAction(this.label, this.action);
}

///******************************************************************************/
///										*/
///	Field separator 							*/
///										*/
///******************************************************************************/

Widget fieldSeparator() {
  return const SizedBox(height: 8);
}

///******************************************************************************/
///										*/
///	Drop down selectors							*/
///										*/
///******************************************************************************/

Widget dropDown(List<String> items,
    {String? value, Function(String?)? onChanged, textAlign = TextAlign.left}) {
  value ??= items[0];
  return DropdownButton<String>(
    value: value,
    onChanged: onChanged,
    items: items.map<DropdownMenuItem<String>>((String value) {
      return DropdownMenuItem<String>(
        value: value,
        child: Text(value, textAlign: textAlign),
      );
    }).toList(),
  );
}

Widget dropDownWidget<T>(List<T> items,
    {String Function(T)? labeler,
    T? value,
    void Function(T?)? onChanged,
    textAlign = TextAlign.left,
    String? nullValue,
    String? label,
    String? hint}) {
  String Function(T) lbl = (x) => x.toString();
  if (labeler != null) lbl = labeler;
  List<DropdownMenuItem<T?>> itmlst = [];
  if (nullValue != null) {
    itmlst.add(DropdownMenuItem<T?>(
      value: null,
      child: Text(
        nullValue,
        textAlign: textAlign,
      ),
    ));
  } else {
    value ??= items[0];
  }

  itmlst.addAll(items.map<DropdownMenuItem<T>>((T v) {
    return DropdownMenuItem<T>(
      value: v,
      enabled: true,
      child: Text(lbl(v)),
    );
  }).toList());

  DropdownButtonFormField<T?> fld = DropdownButtonFormField<T?>(
    value: value,
    onChanged: onChanged,
    items: itmlst,
    decoration: getDecoration(label: label, hint: hint),
  );
  return fld;
}

///******************************************************************************/
///										*/
///	Page navigation assistance						*/
///										*/
///******************************************************************************/

void goto(BuildContext context, Widget w) {
  // if (!context.mounted) return;
  Navigator.of(context).push(MaterialPageRoute(builder: (context) => w));
}

Future<void> gotoThen(BuildContext context, Widget w) async {
  await Navigator.of(context).push(MaterialPageRoute(builder: (context) => w));
}

void gotoDirect(BuildContext context, Widget w) {
  MaterialPageRoute route = MaterialPageRoute(builder: (context) => w);
  Navigator.of(context).pushReplacement(route);
}

void gotoReplace(BuildContext context, Widget w) {
  Navigator.of(context).popUntil((route) => false);
  MaterialPageRoute route = MaterialPageRoute(builder: (context) => w);
  Navigator.of(context).push(route);
}

///******************************************************************************/
///										*/
///	Lists and list boxes							*/
///										*/
///******************************************************************************/

Widget listBox<T>(
  String what,
  List<T> data,
  Widget Function(T) itemBuilder,
  void Function() add,
) {
  List<Widget> widgets = data.map(itemBuilder).toList();
  Widget view = ListBody(children: widgets);
  // ListView view = ListView.builder(
  //	 padding: const EdgeInsets.all(2),
  //	 itemCount: data.length,
  //	 itemBuilder: (BuildContext context, int idx) {
  //	   return itemBuilder(data[idx]);
  //	 });
  String label = "${what}s";
  return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Row(mainAxisAlignment: MainAxisAlignment.start, children: <Widget>[
          Text(label, style: getLabelStyle()),
        ]),
        view,
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: <Widget>[
            IconButton(
              icon: const Icon(Icons.add_box_outlined),
              tooltip: 'Add New $what',
              onPressed: add,
            ),
          ],
        ),
      ]);
}

///******************************************************************************/
///										*/
///	Date and time fields							*/
///										*/
///******************************************************************************/

class DateFormField {
  late final BuildContext context;
  late final TextEditingController _editControl;
  late TextFormField _textField;
  final void Function(DateTime)? onChanged;
  late final DateTime _startDate;
  late final DateTime _endDate;
  late final String? _helpText;

  DateFormField(
    this.context, {
    String? hint,
    String? label,
    DateTime? startDate,
    DateTime? endDate,
    DateTime? initialDate,
    this.onChanged,
  }) {
    _editControl = TextEditingController();
    label ??= hint;
    hint ??= label;
    _helpText = label;
    initialDate ??= DateTime.now();
    startDate ??= DateTime(2020);
    endDate ??= DateTime(2030);
    _startDate = startDate;
    _endDate = endDate;
    _editControl.text = _formatDate(initialDate);
    _textField = TextFormField(
      controller: _editControl,
      decoration: getDecoration(hint: hint, label: label),
      keyboardType: TextInputType.datetime,
      onTap: _handleTap,
      onChanged: _handleChange,
    );
  }

  Widget get widget => _textField;

  void _handleTap() async {
    DateTime? newd = DateTime.tryParse(_editControl.text);
    newd ??= DateTime.now();
    DateTime? nextd = await showDatePicker(
      context: context,
      initialDate: newd,
      firstDate: _startDate,
      lastDate: _endDate,
      helpText: _helpText,
    );
    if (nextd != null) {
      _editControl.text = _formatDate(nextd);
      onChanged!(nextd);
    }
    // bring up date picker here
  }

  void _handleChange(String s) {
    DateTime? newd = DateTime.tryParse(_editControl.text);
    if (newd != null) {
      onChanged!(newd);
    }
  }

  static String _formatDate(DateTime t) {
    t = t.toLocal();
    intl.DateFormat dfmt = intl.DateFormat("EEE MMM d, yyyy");
    return dfmt.format(t);
  }
}

class TimeFormField {
  late final BuildContext context;
  late final TextEditingController _editControl;
  late TextFormField _textField;
  final void Function(TimeOfDay)? onChanged;
  late final String? _helpText;

  TimeFormField(this.context,
      {String? hint,
      String? label,
      TimeOfDay? initialTime,
      DateTime? current,
      this.onChanged}) {
    _editControl = TextEditingController();
    label ??= hint;
    hint ??= label;
    _helpText = label;
    if (current != null) initialTime ??= TimeOfDay.fromDateTime(current);
    initialTime ??= TimeOfDay.now();
    _editControl.text = _formatTime(initialTime);

    _textField = TextFormField(
      controller: _editControl,
      decoration: getDecoration(hint: hint, label: label),
      keyboardType: TextInputType.datetime,
      onTap: _handleTap,
      onChanged: _handleChange,
    );
  }

  Widget get widget => _textField;

  void _handleTap() async {
    TimeOfDay? newd = parseTime(_editControl.text);
    newd ??= TimeOfDay.now();
    TimeOfDay? nextd = await showTimePicker(
      context: context,
      initialTime: newd,
      helpText: _helpText,
    );
    if (nextd != null) {
      _editControl.text = _formatTime(nextd);
      onChanged!(nextd);
    }
    // bring up date picker here
  }

  void _handleChange(String s) {
    TimeOfDay? newd = parseTime(_editControl.text);
    if (newd != null) {
      onChanged!(newd);
    }
  }

  String _formatTime(TimeOfDay tod) {
    return tod.format(context);
  }

  TimeOfDay? parseTime(String t) {
    DateTime dt = DateTime.now();
    String txt = intl.DateFormat.yMd().format(dt);
    txt += " $t";
    DateTime? dt1 = DateTime.tryParse(txt);
    if (dt1 == null) return null;
    return TimeOfDay.fromDateTime(dt1);
  }
}

class DurationFormField {
  late final BuildContext context;
  late final TextEditingController _editControl;
  late TextFormField _textField;
  final void Function(Duration)? onChanged;

  DurationFormField(this.context,
      {String? hint,
      String? label,
      Duration? initialDuration,
      this.onChanged}) {
    _editControl = TextEditingController();
    label ??= hint;
    hint ??= label;
    initialDuration ??= const Duration(minutes: 5);
    _editControl.text = _formatDuration(initialDuration);

    _textField = TextFormField(
      controller: _editControl,
      decoration: getDecoration(hint: hint, label: label),
      keyboardType: TextInputType.datetime,
      onTap: _handleTap,
      onChanged: _handleChange,
    );
  }

  Widget get widget => _textField;

  void _handleTap() async {
    Duration? newd = parseDuration(_editControl.text);
    newd ??= const Duration(minutes: 5);
    Duration? nextd = await showDurationPicker(
      context: context,
      initialTime: newd,
      baseUnit: BaseUnit.minute,
      upperBound: const Duration(hours: 12),
      lowerBound: const Duration(minutes: 1),
    );
    if (nextd != null) {
      _editControl.text = _formatDuration(nextd);
      // onChanged!(nextd);
    }
    // bring up date picker here
  }

  void _handleChange(String s) {
    Duration? newd = parseDuration(_editControl.text);
    if (newd != null) {
      onChanged!(newd);
    }
  }

  String _formatDuration(Duration tod) {
    int hrs = tod.inHours;
    int mins = tod.inMinutes.remainder(60);
    String shrs = "${twoDigits(hrs)}:";
    String smins = twoDigits(mins);
    int sec = tod.inSeconds.remainder(60);
    String ssec = ":${twoDigits(sec)}";

    return shrs + smins + ssec;
  }

  Duration? parseDuration(String t) {
    List<String> timeparts = t.split(":");
    int hrs = 0;
    int pt = 0;
    if (timeparts.length == 3) hrs = int.parse(timeparts[pt++]);
    int mins = int.parse(timeparts[pt++]);
    int secs = int.parse(timeparts[pt++]);
    return Duration(hours: hrs, minutes: mins, seconds: secs);
  }

  String twoDigits(int n) {
    if (n >= 10) {
      return "$n";
    } else {
      return "0$n";
    }
  }
} // end of DurationFormField

///******************************************************************************/
///										*/
///	Numeric fields								*/
///										*/
///******************************************************************************/

Widget integerField({
  required int min,
  required int max,
  required int value,
  TextAlign textAlign = TextAlign.left,
  required String label,
  Function(dynamic)? onChanged,
}) {
  return numberField(
    min: min.toDouble(),
    max: max.toDouble(),
    value: value.toDouble(),
    textAlign: textAlign,
    label: label,
    onChanged: onChanged,
    decimals: 0,
    isInt: true,
  );
}

Widget doubleField({
  required double min,
  required double max,
  required double value,
  TextAlign textAlign = TextAlign.left,
  required String label,
  Function(dynamic)? onChanged,
  int decimals = 1,
}) {
  return numberField(
      min: min,
      max: max,
      value: value,
      textAlign: textAlign,
      label: label,
      onChanged: onChanged,
      decimals: decimals,
      isInt: false);
}

Widget numberField({
  required double min,
  required double max,
  required double value,
  TextAlign textAlign = TextAlign.left,
  required String label,
  Function(dynamic)? onChanged,
  int decimals = 1,
  bool isInt = false,
}) {
  InputDecoration d = getDecoration(label: label);
  Widget w1 = SpinBox(
    min: min,
    max: max,
    value: value,
    textAlign: textAlign,
    decoration: d,
    decimals: decimals,
    onChanged: onChanged,
  );
  Widget w2 = Slider.adaptive(
    min: min,
    max: max,
    value: value,
    onChanged: onChanged,
    label: (isInt ? value.round().toString() : value.toString()),
  );
  SliderThemeData sd = SliderThemeData.fromPrimaryColors(
    primaryColor: const Color.fromARGB(185, 121, 85, 72),
    primaryColorDark: Colors.brown,
    primaryColorLight: const Color.fromARGB(73, 121, 85, 72),
    valueIndicatorTextStyle: const TextStyle(
      color: Colors.white,
    ),
  );
  sd = sd.copyWith(showValueIndicator: ShowValueIndicator.always);
  Widget w3 = SliderTheme(
    data: sd,
    child: w2,
  );
  Widget w4 = Row(
    children: <Widget>[
      Expanded(flex: 3, child: w1),
      Expanded(flex: 10, child: w3),
    ],
  );

  return w4;
}

///******************************************************************************/
///										*/
///	Dialog setup								*/
///										*/
///******************************************************************************/

Future<void> displayDialog(
    BuildContext context, String title, String description) async {
  return showDialog<void>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(description, maxLines: 10),
          actions: <Widget>[
            TextButton(
                child: const Text("OK"),
                onPressed: () {
                  Navigator.of(context).pop();
                }),
          ],
        );
      });
}

Future<bool> getValidation(BuildContext context, String title) async {
  bool? sts = await showDialog<bool>(
    context: context,
    builder: (BuildContext context) {
      return SimpleDialog(
        title: Text(title),
        children: <Widget>[
          SimpleDialogOption(
            onPressed: () {
              Navigator.pop(context, true);
            },
            child: const Text("Yes"),
          ),
          SimpleDialogOption(
            onPressed: () {
              Navigator.pop(context, false);
            },
            child: const Text("No"),
          ),
        ],
      );
    },
  );
  if (sts != null) return sts;
  return false;
}

///******************************************************************************/
///										*/
///	Top level pages 							*/
///										*/
///******************************************************************************/

Widget sherpaPage(BuildContext context, Widget child) {
  return sherpaPage1(context, child);
}

// Widget sherpaPage0(BuildContext context, Widget child) {
//   return Container(
//     decoration: BoxDecoration(
//	 border: Border.all(
//	   width: 8,
//	   color: const Color.fromARGB(128, 210, 180, 140),
//	 ),
//	 image: const DecorationImage(
//	   image: AssetImage("assets/images/sherpaimage.png"),
//	   fit: BoxFit.fitWidth,
//	   opacity: 0.05,
//	 ),
//     ),
//     child: Column(
//	 children: <Widget>[
//	   SingleChildScrollView(
//	     child: child,
//	   ),
//	 ],
//     ),
//   );
// }

Widget sherpaPage1(BuildContext context, Widget child) {
  return LayoutBuilder(
    builder: (BuildContext context, BoxConstraints cnst) =>
        _sherpaPageBuilder(context, cnst, child),
  );
}

Widget _sherpaPageBuilder(
    BuildContext context, BoxConstraints constraints, Widget child) {
  return Container(
    decoration: BoxDecoration(
      border: Border.all(
        width: 8,
        color: const Color.fromARGB(128, 210, 180, 140),
      ),
      image: const DecorationImage(
        image: AssetImage("assets/images/sherpaimage.png"),
        fit: BoxFit.fitWidth,
        opacity: 0.05,
      ),
    ),
    child: SingleChildScrollView(
      child: ConstrainedBox(
        constraints: const BoxConstraints(
            // minHeight: constraints.maxHeight,
            ),
        child: child,
      ),
    ),
  );
}

Widget boxWidgets(List<Widget> wlist, {double width = 8}) {
  return Container(
    decoration: BoxDecoration(
      border: Border.all(
        width: width,
        color: const Color.fromARGB(128, 210, 180, 140),
      ),
    ),
    child: Column(
      mainAxisAlignment: MainAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: wlist,
    ),
  );
}

Widget sherpaNSPage(BuildContext context, Widget child) {
  return Container(
    decoration: BoxDecoration(
      border: Border.all(
        width: 8,
        color: const Color.fromARGB(128, 210, 180, 140),
      ),
      image: const DecorationImage(
        image: AssetImage("assets/images/sherpaimage.png"),
        fit: BoxFit.fitWidth,
        opacity: 0.05,
      ),
    ),
    child: child,
  );
}

///******************************************************************************/
///										*/
///	Utility methods 							*/
///										*/
///******************************************************************************/

ThemeData getTheme() {
  return ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color.fromRGBO(121, 85, 72, 1),
    ),
  );
}

InputDecoration getDecoration({
  String? hint,
  String? label,
  double vPadding = 0,
  double hPadding = 4.0,
  String? error,
}) {
  hint ??= label;
  label ??= hint;
  return InputDecoration(
    hintText: hint,
    labelText: label,
    labelStyle: getLabelStyle(),
    hoverColor: Colors.amber,
    focusedBorder: const OutlineInputBorder(
      borderSide: BorderSide(
        width: 2,
        color: Colors.yellow,
      ),
    ),
    border: const OutlineInputBorder(
      borderSide: BorderSide(
        width: 2,
        color: Colors.white,
      ),
    ),
    contentPadding: EdgeInsets.symmetric(
      horizontal: hPadding,
      vertical: vPadding,
    ),
  );
}

TextStyle getLabelStyle() {
  return const TextStyle(
      color: globals.labelColor, fontWeight: FontWeight.bold);
}

