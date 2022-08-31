/********************************************************************************/
/*                                                                              */
/*              SignMakerSign.java                                              */
/*                                                                              */
/*      Hold all the information for the current sign                           */
/*                                                                              */
/********************************************************************************/

package edu.brown.cs.signmaker;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingColorSet;

class SignMakerSign implements SignMakerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SignMakerText []        text_regions;
private SignMakerImage []       image_regions;

private Color                   background_color;
private Color                   foreground_color;
private String                  font_family;
private int                     border_width;
private Color                   border_color;
private Map<SignMakerComponent,Rectangle2D> item_positions;
private Map<String,String>      key_values;
private int                     user_id;


private static double [] SCALE_VALUES = {
   0.0, 1.0, 0.7, 0.45, 0.3, 0.2, 0.0
};

private static final DateFormat TIME_FORMAT = new SimpleDateFormat("h:mm");

private static Set<String> font_names;

static {
   font_names = new HashSet<>();
   GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
   String [] nms = ge.getAvailableFontFamilyNames();
   for (String s : nms) {
      font_names.add(s.toLowerCase());
      int idx = s.indexOf(" ");
      if (idx > 0) {
         String s1 = s.substring(0,idx);
         font_names.add(s1);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SignMakerSign(int uid) 
{
   text_regions = new SignMakerText[8];
   image_regions = new SignMakerImage[8];
   background_color = Color.WHITE;
   foreground_color = Color.BLACK;
   font_family = null;
   border_width = 0;
   border_color = null;
   item_positions = new HashMap<>();
   key_values = new HashMap<>();
   user_id = uid;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

BufferedImage createSignImage(int w,int h)
{
   System.err.println("SIGNMAKER: create sign image " + w + " " + h + " " +
         text_regions + " " + image_regions + " " + background_color + " " +
         font_family + " " + border_width + " " + border_color);
   
   for (int i = 0; i < text_regions.length; ++i) {
      if (text_regions[i] != null && text_regions[i].isEmpty()) text_regions[i] = null;
    }
   for (int i = 0; i < image_regions.length; ++i) {
      if (image_regions[i] != null && image_regions[i].isEmpty()) image_regions[i] = null;
    }
   
   setDimensions(w,h);
   
   JPanel pnl = new JPanel();
   pnl.setSize(w,h);
   pnl.setBackground(background_color);
   pnl.setForeground(foreground_color);
   if (font_family != null) {
      Font ft = new Font(font_family,0,10);
      pnl.setFont(ft);
    }
   
   BufferedImage img = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
   Graphics g = img.getGraphics();
   Graphics2D g2 = (Graphics2D) g;
   for (int i = 0; i < text_regions.length; ++i) {
      setup(pnl,text_regions[i]);
    }
   for (int i = 0; i < image_regions.length; ++i) {
      setup(pnl,image_regions[i]);
    }
   
   pnl.paint(g2);
   
   for (int i = 0; i < text_regions.length; ++i) {
      waitForReady(text_regions[i]);
    }
   for (int i = 0; i < image_regions.length; ++i) {
      waitForReady(image_regions[i]);
    }
   
   pnl.paint(g2);
   
   return img;
}



private void setup(JPanel pnl,SignMakerComponent c)
{
   if (c == null) return;
   
   Rectangle2D r2 = item_positions.get(c);
   if (r2 == null) return;
   
   JComponent comp = c.setupComponent(r2,pnl,this);
   if (comp == null) return;
   
   pnl.add(comp);
   
   comp.setBounds(r2.getBounds());
}


private void waitForReady(SignMakerComponent c) 
{
   if (c == null) return;
   c.waitForReady();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void setBackground(Color c)
{
   background_color = c;
}

void setForeground(Color c)
{
   foreground_color = c;
}

void setBorder(int w,String c)
{
   border_width = w;
   border_color = SwingColorSet.getColorByName(c);
}

boolean setFontFamily(String name)
{
   name = getFontFamily(name);
   if (name == null) return false;
   
   font_family = name;
   return true;
}



static String getFontFamily(String name)
{
   name = name.toLowerCase().trim();
   
   if (!font_names.contains(name)) {
      int idx = name.indexOf("_");
      if (idx < 0) return null;
      String base = name.substring(0,idx);
      if (!font_names.contains(base)) return null;
      name = name.replace("_"," ");
    }
   
   return name;
}


void setTextRegion(int which,SignMakerText rgn)
{
   if (which >= text_regions.length) return;
   if (rgn != null && !rgn.isEmpty()) text_regions[which] = rgn;
}


void setImageRegion(int which,SignMakerImage rgn)
{
   if (which >= image_regions.length) return;
   if (rgn != null && !rgn.isEmpty()) image_regions[which] = rgn;
}


void setProperty(String key,String value)
{
   if (value.startsWith("@")) {
      value = computeTimeValue(value);
    }
   key_values.put(key,value);
}


private String computeTimeValue(String val)
{
   boolean approx = false;
   if (val.startsWith("@~")) {
      approx = true;
      val = val.substring(2);
    }
   else if (val.startsWith("@")) {
      val = val.substring(1);
    }
   else return val;
   
   int delta = 60*60*1000;
   if (val.startsWith("+")) {
      val = val.substring(1);
      delta = Integer.parseInt(val);
    }
   long t0 = System.currentTimeMillis() + delta; 
   if (approx) {
      t0 = (t0 + APPROX_TIME-1) / APPROX_TIME;
      t0 = t0 * APPROX_TIME;
    }
    
   Date d = new Date(t0);
   String rslt = TIME_FORMAT.format(d);
   
   return rslt;
}

String mapString(String s)
{
   if (key_values.isEmpty()) return s;
   s = IvyFile.expandText(s,key_values,false);
   return s;
}


String useSavedImage(String name)
{
   Connection sql = SignMaker.getSqlDatabase();
   if (sql == null || name == null || name.length() == 0) return null;
   String cnts = null;
   int defid = -1;
   try {
      String q = "SELECT * FROM iQsignDefines WHERE name = ? AND userid = ?";
      PreparedStatement st = sql.prepareStatement(q);
      st.setString(1,name);
      st.setInt(2,user_id);
      ResultSet rs = st.executeQuery();;
      if (rs.next()) {
         cnts = rs.getString("contents");
         defid = rs.getInt("id");
       }
      st.close();
      
      if (cnts == null) {
         String q1 = "SELECT * FROM iQsignDefines WHERE name = ? AND userid = NULL";
         PreparedStatement st1 = sql.prepareStatement(q1);
         st1.setString(1,name);
         ResultSet rs1 = st1.executeQuery();;
         if (rs1.next()) {
            cnts = rs1.getString("contents");
            defid = rs.getInt("id");
          }
         st1.close();
       }
      
      if (defid > 0) {
         String q2 = "SELECT * FROM iQsignParameters WHERE defineid = ?";
         PreparedStatement st2 = sql.prepareStatement(q2);
         st2.setInt(1,defid);
         ResultSet rs2 = st2.executeQuery();
         while (rs2.next()) {
            String pnm = rs2.getString("name");
            String val = rs2.getString("value");
            if (val != null) {
               setProperty(pnm,val);
             }
          }
       }
      
    }
   catch (SQLException e) {
      System.err.println("signmaker: Database problem: " + e);
      e.printStackTrace();
    }
   
   
   return cnts;
}



void clearContents()
{
   for (int i = 0; i < text_regions.length; ++i) {
      text_regions[i] = null;
    }
   for (int i = 0; i < image_regions.length; ++i) {
      image_regions[i] = null;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Layout methods                                                          */
/*                                                                              */
/********************************************************************************/

private void setDimensions(double w,double h)
{
   // compute sizing for each of the regions
   SignMakerComponent c0 = text_regions[5];
   if (c0 == null) c0 = image_regions[6];
   SignMakerComponent c1 = text_regions[4];
   if (c1 == null) c1 = image_regions[5];
   
   double [] rows = new double[5];
   rows[0] = getLevel(image_regions[3],c0,image_regions[4]);
   rows[1] = getLevel(text_regions[1]);
   rows[2] = getLevel(text_regions[2]);
   rows[3] = getLevel(text_regions[3]);
   rows[4] = getLevel(image_regions[1],c1,image_regions[2]);
   
   double tot = 0;
   for (int i = 0; i < rows.length; ++i) tot += rows[i];
   if (tot == 0) return;
   for (int i = 0; i < rows.length; ++i) {
      rows[i] = rows[i] / tot * h;
    }
   
   double ypos = 0;
   ypos = setPositions(w,ypos,rows[0], image_regions[3],c0,image_regions[4]);
   ypos = setPositions(w,ypos,rows[1],text_regions[1]);
   ypos = setPositions(w,ypos,rows[2],text_regions[2]);
   ypos = setPositions(w,ypos,rows[3],text_regions[3]);
   ypos = setPositions(w,ypos,rows[4],image_regions[1],c1,image_regions[2]);
}



private double getLevel(SignMakerComponent ... cset)
{
   double level = 0;
   for (SignMakerComponent c : cset) {
      if (c == null) continue;
      int lvl = c.getSizeLevel();
      double v = SCALE_VALUES[lvl];
      level = Math.max(v,level);
    }
   return level;
}

double setPositions(double w,double y,double h,SignMakerComponent c0,SignMakerComponent c1,SignMakerComponent c2)
{
   if (c0 == null && c2 == null) {
      return setPositions(w,y,h,c1);
    }
   
   double w0 = h;
   if (c0 != null) {
      Rectangle2D r = new Rectangle2D.Double(0,y,w0,h);
      item_positions.put(c0,r);
    }
   if (c2 != null) {
      Rectangle2D r = new Rectangle2D.Double(w-w0,y,w0,h);
      item_positions.put(c2,r); 
    }
   if (c1 != null) {
      Rectangle2D r = new Rectangle2D.Double(w0,y,w-2*w0,h);;
      item_positions.put(c1,r);
    }
   
   return y+h;
}


double setPositions(double w,double y,double h,SignMakerComponent c0)
{
   if (c0 != null) {
      Rectangle2D r = new Rectangle2D.Double(0,y,w,h);
      item_positions.put(c0,r);
    }
   
   return y+h;
}

}       // end of class SignMakerSign




/* end of SignMakerSign.java */

