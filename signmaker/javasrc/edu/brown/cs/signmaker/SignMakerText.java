/********************************************************************************/
/*                                                                              */
/*              SignMakerText.java                                              */
/*                                                                              */
/*      Container for contents of a text block                                  */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.signmaker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.JLabel;

import edu.brown.cs.ivy.swing.SwingColorSet;

class SignMakerText extends SignMakerComponent implements SignMakerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private StringBuffer            current_text;
private boolean                 is_empty;
private Stack<String>           nest_items;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SignMakerText()
{
   current_text = new StringBuffer();
   current_text.append("<html>");
   is_empty = true;
   nest_items = new Stack<>();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override JComponent setupComponent(Rectangle2D r,JComponent par,SignMakerSign sgn)
{
   String text = sgn.mapString(current_text.toString());
   JLabel lbl = new JLabel(text,JLabel.CENTER);
   lbl.setFont(par.getFont());
   lbl.setVerticalAlignment(JLabel.CENTER);
   Dimension sz = lbl.getPreferredSize();
   
   Font lastfont = lbl.getFont();
   double szw  = r.getWidth()/sz.getWidth();
   double szh = r.getHeight()/sz.getHeight();
   double scale = Math.min(szw,szh);
   float s = lastfont.getSize2D();
   s *= scale * 0.9;
   Font newfont = lastfont.deriveFont(s);
   lbl.setFont(newfont);
   
   return lbl;
}



@Override boolean isEmpty()
{
   return is_empty;
}


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void addText(char c)
{
   current_text.append(c);
   is_empty = false;
}


void addText(String s)
{
   // check for parameters ?
   
   current_text.append(s);
   is_empty = false;
}


boolean setFont(Color c,String family)
{
   if (family != null) {
      family = SignMakerSign.getFontFamily(family);
      if (family == null) return false;
    }
   String cs = null;
   if (c != null) cs = SwingColorSet.getColorName(c);
   setFont(cs,family);
   
   return true;
}



private void setFont(String color,String family)
{
   String cnts = "";
   if (color == null && family == null) return;
   if (family != null) cnts += " family='" + family + "'";
   if (color != null) cnts += " color='" + color + "'";
   current_text.append("<font" + cnts + ">");
   nest_items.push("font");
}


void setBold()
{
   current_text.append("<b>");
   nest_items.push("b");
}

void setItalic()
{
   current_text.append("<i>");
   nest_items.push("i");
}


void setUnderline()
{
   current_text.append("<u>");
   nest_items.push("u");
}

void setNormal()
{
   while (!nest_items.isEmpty()) {
      String what = nest_items.peek();
      switch (what) {
         case "i" :
         case "u" :
         case "b" :
            pop();
            break;
         default :
            return;
       }
    }
}


void pop()
{
   if (nest_items.isEmpty()) return;
   String what = nest_items.pop();
   current_text.append("</" + what + ">");
}


void popAll()
{
   while (!nest_items.isEmpty()) {
      pop();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return current_text.toString();
}


}       // end of class SignMakerText




/* end of SignMakerText.java */

