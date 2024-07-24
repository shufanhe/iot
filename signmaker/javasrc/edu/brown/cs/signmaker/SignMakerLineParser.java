/********************************************************************************/
/*										*/
/*		SignMakerLineParser.java					*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/




package edu.brown.cs.signmaker;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.ivy.swing.SwingColorSet;
import edu.brown.cs.signmaker.SignMakerConstants.SignMakerParser;

class SignMakerLineParser implements SignMakerConstants, SignMakerParser
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SignMakerSign	result_sign;
private int		current_text;
private int		current_image;
private int		text_level;
private int		user_id;
private Color           txt_color;
private String          txt_font;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

SignMakerLineParser(int uid,boolean counts)
{
   result_sign = new SignMakerSign(uid,counts);
   current_text = 1;
   current_image = 1;
   text_level = 1;
   user_id = uid;
   txt_color = null;
   txt_font = null;
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public SignMakerSign parse(InputStream ins) throws SignMakerException
{
   boolean isloadsign = false;

   BufferedReader br = new BufferedReader(new InputStreamReader(ins));
   try {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 ln = ln.trim();
	 while (ln.endsWith("\\")) {
	    ln = ln.substring(0,ln.length()-1);
	    String ln1 = br.readLine();
	    if (ln1 == null) break;
	    ln = ln + " " + ln1.trim();
	  }
	 if (ln.length() == 0) continue;
	 if (!isIndicator(ln)) ln = "# " + ln;
	 char typ = ln.charAt(0);
	 List<String> cnts = splitLine(ln);
	 switch (typ) {
	    case '@' :
	       if (!isloadsign) parseImageLine(cnts);
	       break;
	    case '#' :
	       if (!isloadsign) parseTextLine(cnts);
	       break;
	    case '%' :
	       parseGlobalLine(cnts);
	       break;
	    case '=' :
	       isloadsign |= parseLoadLine(cnts);
	       break;
	  }
	
       }
    }
   catch (IOException e) {
      System.err.println("signmaker: Problem reading input: " + e);
      System.exit(1);
    }

   return result_sign;
}



/********************************************************************************/
/*										*/
/*	Parse global line							*/
/*										*/
/********************************************************************************/

void parseGlobalLine(List<String> cnts) throws SignMakerException
{
   for (int i = 0; i < cnts.size(); ++i) {
      String s = cnts.get(i);
      if (s.equals("%")) break;                 // comment
      if (s.startsWith("%")) {
	 String cmd = s.substring(1).toLowerCase();
	 switch (cmd) {
	    case "dialog" :
               txt_font = cmd;
	       result_sign.setFontFamily(Font.DIALOG);
	       break;
	    case "dialoginput" :
               txt_font = cmd;
	       result_sign.setFontFamily(Font.DIALOG_INPUT);
	       break;
	    case "monospaced" :
               txt_font = cmd;
	       result_sign.setFontFamily(Font.MONOSPACED);
	       break;
	    case "sansserif" :
	    case "sans" :
               txt_font = cmd;
               result_sign.setFontFamily(Font.SANS_SERIF);
               break;
	    case "serif" :
               txt_font = cmd;
	       result_sign.setFontFamily(Font.SERIF);
	       break;
	    case "border" :
	    case "Border" :
	       // handle borders here
	       break;
	    case "bg" :
	    case "background" :
               if (cnts.size() > i+1) {
                  Color bg = parseColor(cnts.get(++i));
                  if (bg != null) result_sign.setBackground(bg);
                }
	       break;
	    case "fg" :
	    case "foreground" :
               if (cnts.size() > i+1) {
                  Color fg = parseColor(cnts.get(++i));
                  if (fg != null) {
                     txt_color = fg;
                     result_sign.setForeground(fg);
                   }
                }
	       break;
	    default :
	       if (result_sign.setFontFamily(cmd)) break;
               else {
                  Color bg = parseColor(cmd);
                  if (bg != null) result_sign.setBackground(bg);
                }
	       break;
	  }
       }
      else {
	 // ignore arbitrary text not in a command
       }
    }
}



/********************************************************************************/
/*										*/
/*	Parse image line							*/
/*										*/
/********************************************************************************/

void parseImageLine(List<String> cnts) throws SignMakerException
{
   Color fg = null;
   Color bg = null;
   String image = null;
   boolean isqr = false;
   int lvl = current_image;
   int size = -1;

   for (int i = 0; i < cnts.size(); ++i) {
      String s = cnts.get(i);
      if (s.startsWith("%")) break;
      else if (s.equals("@")) continue;
      if (s.startsWith("@")) {
	 String cmd = s.substring(1).toLowerCase();
	 if (i+1 < cnts.size()) {
	    switch (cmd) {
	       case "1" :
	       case "2" :
	       case "3" :
	       case "4" :
	       case "5" :
		  lvl = Integer.parseInt(cmd);
		  break;
               case "size" :
                  size = Integer.parseInt(cnts.get(++i));
                  break;
	       case "bg" :
	       case "background" :
		  bg = parseColor(cnts.get(++i));
		  break;
	       case "fg" :
	       case "foreground" :
		  fg = parseColor(cnts.get(++i));
		  break;
	       case "qr" :
		  isqr = true;
		  break;
	     }
	  }
       }
      else if (image == null) image = s;
    }

   if (image != null) {
      SignMakerImage img = new SignMakerImage(user_id);
      if (bg != null) img.setBackgroundColor(bg);
      if (fg != null) img.setForegroundColor(fg);
      if (size > 0) img.setSizeLevel(size);
      if (isqr) img.setQR(image);
      else img.setImage(image);
      result_sign.setImageRegion(lvl,img);
    }
   current_image = lvl+1;
}


private Color parseColor(String x)
{
   x = x.trim().toLowerCase();
   Color c = SwingColorSet.getColorByName(x,false);
   if (c != null) return c;
   c = SwingColorSet.getColorByName("#" + x,false);
   if (c != null) return c;
   return null;
}



/********************************************************************************/
/*										*/
/*	Parse text line 							*/
/*										*/
/********************************************************************************/

void parseTextLine(List<String> cnts)
{
   SignMakerText txt = new SignMakerText();
   if (txt_color != null || txt_font != null) {
      txt.setFont(txt_color,txt_font);
    } 
   
   for (int i = 0; i < cnts.size(); ++i) {
      String s = cnts.get(i);
      if (s.equals("#")) {
	 txt.popAll();
         if (txt_color != null || txt_font != null) {
            txt.setFont(txt_color,txt_font);
          } 
       }
      else if (s.startsWith("#")) {
	 String cmd = s.substring(1).toLowerCase();
	 switch (cmd) {
	    case "1" :
	    case "2" :
	    case "3" :
	    case "4" :
	    case "5" :
	       text_level = Integer.parseInt(cmd);
	       break;
	    case "bold" :
	    case "b" :
	       txt.setBold();
	       break;
	    case "italic" :
	    case "i" :
	       txt.setItalic();
	       break;
	    case "normal" :
	    case "n" :
	       txt.setNormal();
	       break;
	    case "underline" :
	    case "u" :
	       txt.setUnderline();
	       break;
	    case "fg" :
	    case "color" :
	       if (i+1 < cnts.size()) {
		  Color c = parseColor(cnts.get(i+1));
		  if (c != null) {
		     txt.setFont(c,null);
		   }
		}
	       break;
	    case "bg" :
	    case "background" :
	       if (i+1 < cnts.size()) {
		  Color c = parseColor(cnts.get(i+1));
		  if (c != null) {
		     // set background color in text
		   }
		}
	       break;
	    default :
	       if (txt.setFont(null,cmd)) break;
	       Color c = parseColor(cmd);
	       if (c != null) txt.setFont(c,null);
	       break;
	  }
       }
      else {
	 txt.addText(s);
       }
    }
   
   txt.popAll();
   txt.setSizeLevel(text_level);
   if (text_level < 5) ++text_level;

   result_sign.setTextRegion(current_text++,txt);
}



/********************************************************************************/
/*										*/
/*	Handle loading saved signs						*/
/*										*/
/********************************************************************************/

boolean parseLoadLine(List<String> cnts) throws SignMakerException
{
   boolean loaded = false;

   for (int i = 0; i < cnts.size(); ++i) {
      String what = cnts.get(i).trim();
      if (what.equals("=")) {
	 if (i+1 < cnts.size()) {
	    String name = cnts.get(i+1);
	    if (name.startsWith("=") || name.contains("=")) continue;
	    ++i;
	    useSavedImage(name);
	  }
       }
      else if (what.startsWith("=")) {
	 String name = what.substring(1);
	 useSavedImage(name);
       }
      else if (what.contains("=")) {
	 int idx = what.indexOf("=");
	 String key = what.substring(0,idx).trim();
	 String value = what.substring(idx+1).trim();
	 result_sign.setProperty(key,value);
       }
    }
   return loaded;
}



private void useSavedImage(String name) throws SignMakerException
{
   String usecnts = result_sign.useSavedImage(name);
   if (usecnts != null) {
      result_sign.clearContents();
      ByteArrayInputStream bas = new ByteArrayInputStream(usecnts.getBytes());
      parse(bas);
    }
}


/********************************************************************************/
/*										*/
/*	Break Line into logical units						*/
/*										*/
/********************************************************************************/

List<String> splitLine(String ln)
{
   List<String> rslt = new ArrayList<>();
   char linetype = 0;

   for (int i = 0; i < ln.length(); ++i) {
      char c = ln.charAt(i);
      if (isIndicator(c) && (linetype == 0 || linetype == c)) {
	 if (linetype == 0) {
            linetype = c;
          }         
         if (linetype == '=') i = scanLoadWord(i,ln,rslt);
         else i = scanStartWord(i,ln,rslt);
       }
      else if (Character.isWhitespace(c)) ;
      else if (c == '%') break;
      else if (linetype == '=') {
         i = scanLoadWord(i,ln,rslt);
       }
      else if (isIndicator(c)) {
         break;
       }
      else {
         i = scanWord(i,ln,rslt);
       }
    }

   return rslt;
}



private int scanWord(int i,String ln,List<String> rslt)
{
   int i0 = i;
   StringBuffer wd = new StringBuffer();
   for ( ; i < ln.length(); ++i) {
      char c = ln.charAt(i);
      if (isIndicator(c) && i != i0) {
         --i;
         break;
       }
      else if (c == '\\' && i+1 < ln.length()) {
         c = ln.charAt(++i);
         wd.append(c);
       }
      else wd.append(c);
    }
   rslt.add(wd.toString());
   return i;
}



private int scanStartWord(int i,String ln,List<String> rslt)
{
   StringBuffer wd = new StringBuffer();
   for ( ; i < ln.length(); ++i) {
      char c = ln.charAt(i);
      if (Character.isWhitespace(c)) {
         break;
       }
      else if (c == '\\' && i+1 < ln.length()) {
         c = ln.charAt(++i);
         wd.append(c);
       }
      else wd.append(c);
    }
   rslt.add(wd.toString());
   
   return i;
}


private int scanLoadWord(int i,String ln,List<String> rslt)
{
   StringBuffer wd = new StringBuffer();
   String lasttok = null;
   int laststart = -1;
   boolean lastwhite = false;
   boolean usewd = true;
   for ( ; i < ln.length(); ++i) {
      char c = ln.charAt(i);
      if (c == '%') {
         --i;
         break;
       }
      else if (Character.isWhitespace(c)) {
         if (!lastwhite) {
            lasttok = wd.toString();
          }
         lastwhite = true;
         wd.append(c);
       }
      else {
         if (lastwhite) {
            lastwhite = false;
            laststart = i;
          }
         if (c == '=' && lasttok != null && laststart > 0) {
            rslt.add(lasttok.trim());
            i = laststart-1;
            usewd = false;
            break;
          }
         if (c == '\\' && i+1 < ln.length()) {
            c = ln.charAt(++i);
            wd.append(c);
          }
         else wd.append(c);
       }
    }
   if (usewd) rslt.add(wd.toString().trim());
   
   return i;
}




private boolean isIndicator(String s)
{
   if (s.length() == 0) return false;
   return isIndicator(s.charAt(0));
}

private boolean isIndicator(char c)
{
   return c == '@' || c == '#' || c == '%' || c == '=';
}



}	// end of class SignMakerLineParser




/* end of SignMakerLineParser.java */

