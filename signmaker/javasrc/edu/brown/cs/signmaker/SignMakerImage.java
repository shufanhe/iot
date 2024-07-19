/********************************************************************************/
/*										*/
/*		SignMakerImage.java						*/
/*										*/
/*	Container for information about an image				*/
/*										*/
/********************************************************************************/



package edu.brown.cs.signmaker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.JSVGComponent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.DescendantSelector;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.Locator;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.steadystate.css.parser.HandlerBase;
import com.steadystate.css.parser.SACParserCSS3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.brown.cs.ivy.swing.SwingColors;

class SignMakerImage extends SignMakerComponent implements SignMakerConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Color	background_color;
private Color	foreground_color;
private String	image_contents;
private SignImageType image_type;
private SvgComponent svg_component;
private int     user_id;

static private Font fa_solid;
static private Font fa_regular;
static private Map<String,String> fa_map;

static private File svg_library;

static {
   GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
   File f0 = new File("/pro/iot/signmaker");
   if (!f0.exists()) f0 = new File("/vol/iot/signmaker");
   
   File f1 = new File(f0,"resources/free-fa-solid-900.ttf");
   File f2 = new File(f0,"resources/free-fa-regular-400.ttf");
   try {
      fa_solid = Font.createFont(Font.TRUETYPE_FONT,f1);
      ge.registerFont(fa_solid);
    }
   catch (Throwable t) {
      System.err.println("signmaker: Problem creating font " + f1);
      t.printStackTrace();
      System.exit(1);
    }
   try {
      fa_regular = Font.createFont(Font.TRUETYPE_FONT,f2);
      ge.registerFont(fa_regular);
    }
   catch (Throwable t) {
      System.err.println("signmaker: Problem creating font " + f2);
      t.printStackTrace();
      System.exit(1);
    }
   fa_map = new HashMap<>();
   loadFontAwesome();

   svg_library = SignMaker.getSvgLibrary();
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

SignMakerImage(int uid)
{
   image_type = SignImageType.NONE;
   background_color = null;
   foreground_color = null;
   image_contents = null;
   setSizeLevel(3);
   user_id = uid;
}



/********************************************************************************/
/*										*/
/*	Generation Methods							*/
/*										*/
/********************************************************************************/

@Override JComponent setupComponent(Rectangle2D r,JComponent par,SignMakerSign sgn)
{
   if (image_contents.endsWith(".svg")) {
      return setupSVGComponent(r,par);
    }

   JLabel lbl = new JLabel();
   lbl.setVerticalAlignment(JLabel.CENTER);
   lbl.setHorizontalAlignment(JLabel.CENTER);
   lbl.setSize((int) r.getWidth(),(int) r.getHeight());
   if (background_color != null) lbl.setBackground(background_color);
   if (foreground_color != null) lbl.setForeground(foreground_color);

   String cnts = sgn.mapString(image_contents);
   
   switch (image_type) {
      case FILE :
	 loadFileImage(lbl,r,image_contents);
	 break;
      case FONT_AWESOME :
	 loadFontAwesomeImage(lbl,r,image_contents);
	 break;
      case NONE :
	 return null;
      case QR :
	 loadQRImage(lbl,r,cnts);
	 break;
      case URL :
	 loadUrlImage(lbl,r,image_contents);
	 break;
    }

   return lbl;
}


private JComponent setupSVGComponent(Rectangle2D r,JComponent par)
{
   SvgComponent c = new SvgComponent(r.getBounds().getSize());
   svg_component = c;

   String uri = image_contents;
   switch (image_type) {
      case FILE :
	 uri = "file://" + image_contents;
	 break;
      case URL :
	 break;
      case FONT_AWESOME :
      case QR :
      case NONE :
	 return null;
    }

   c.loadSVGDocument(uri);

   return c;
}


@Override void waitForReady()
{
   if (svg_component == null) return;

   svg_component.waitForReady();

}



@Override boolean isEmpty()
{
   return image_type == SignImageType.NONE;
}

@Override boolean isImage()                             { return true; } 



/********************************************************************************/
/*										*/
/*	Svg classes								*/
/*										*/
/********************************************************************************/

class SvgComponent extends JSVGComponent {

   private LoadImageListener load_listener;
   private TreeImageListener tree_listener;
   private boolean is_ready;
   private Rectangle target_bounds;
   
   private static final long serialVersionUID = 1;

   SvgComponent(Dimension d) {
      super();
      load_listener = new LoadImageListener();
      addSVGDocumentLoaderListener(load_listener);
      tree_listener = new TreeImageListener();
      addGVTTreeRendererListener(tree_listener);
      
      setDocumentState(ALWAYS_STATIC);
      if (background_color == null) {
         setBackground(SwingColors.SWING_TRANSPARENT);
         setOpaque(true);
       }
      else {
         setBackground(background_color);
       }
      if (foreground_color != null) setForeground(foreground_color);
   
      setRecenterOnResize(false);
      is_ready = false;
      target_bounds = null;
      super.setBounds(0,0,2048,2048);
    }

    void waitForReady() {
      if (is_ready) return;
      tree_listener.waitForRendered();
      while (image == null) {
         synchronized (tree_listener) {
            try {
               tree_listener.wait(100);
             }
            catch (InterruptedException e) { }
          }
       }
      is_ready = true;
    }
   
   void waitForLoaded() {
      if (is_ready) return;
      load_listener.waitForLoaded();
    }

   @Override protected void handleException(Exception e) {
      System.err.println(e);
   }
   
   @Override public void setBounds(Rectangle r) {
      target_bounds = r;
      setLocation(r.x,r.y);
    }
   
   @Override public Rectangle getBounds() {
      return target_bounds;
    }

   @Override public void paintComponent(Graphics g) {
//    if (!is_ready) return;
      
      if (image == null) {
         waitForLoaded();
         super.paintComponent(g);
         if (image == null) return;
       }
         
      Dimension2D dsize = getSVGDocumentSize();
      
      int dw = (int) dsize.getWidth();
      int dh = (int) dsize.getHeight();
      
      int w = target_bounds.width;
      int h = target_bounds.height;
      
      if (dsize.getWidth() == 0) return;
      double margin = 0.05;
      double sizer = 1.0 - 2*margin;
      double dx = w*margin;
      double dy = h*margin;
      double w1 = w*sizer;
      double h1 = h*sizer;
      double sx = w1/dsize.getWidth();
      double sy = h1/dsize.getHeight();
   
      Image img0 = image.getSubimage(0,0,dw,dh);
      BufferedImage bimg0 = (BufferedImage) img0;
      AffineTransform at = new AffineTransform();
      at.translate(dx,dy);
      at.scale(sx,sy);
      
      Map<RenderingHints.Key,Object> rhmap = new HashMap<>();
      rhmap.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
      rhmap.put(RenderingHints.KEY_DITHERING,RenderingHints.VALUE_DITHER_ENABLE);
      rhmap.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);     
      
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHints(rhmap);
      
      setPaintingTransform(at);
      image = bimg0;
      g.setClip(0,0,w,h);
      
      super.paintComponent(g);
    }
   
   @Override public Rectangle getRenderRect() {
      if (is_ready) {
         return new Rectangle(0,0,target_bounds.width,target_bounds.height);
       }
      else {
         return new Rectangle(0,0,2048,2048);
       }
    }

}	// end of inner class SvgComponent


private static class LoadImageListener extends SVGDocumentLoaderAdapter {
   
   private boolean is_loaded;
   
   LoadImageListener() {
      is_loaded = false;
    }
   
   @Override public void documentLoadingCancelled(SVGDocumentLoaderEvent e) {
      noteLoaded();
    }
   @Override public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
      noteLoaded();
    }
   @Override public void documentLoadingFailed(SVGDocumentLoaderEvent e) {
      noteLoaded();
    }
   
   private synchronized void noteLoaded() {
      is_loaded = true;
      notifyAll();
    }
   
   synchronized void waitForLoaded() {
      while (!is_loaded) {
         try {
            wait();
          }
         catch (InterruptedException e) { }
       }
    }
   
}       // end of class LoadImageListener     



private static class TreeImageListener extends GVTTreeRendererAdapter {
   
   private boolean is_done;
   
   TreeImageListener() {
      is_done = false;
    }
   
   @Override public void gvtRenderingCancelled(GVTTreeRendererEvent e) {
      noteDone();
    }
   @Override public void gvtRenderingFailed(GVTTreeRendererEvent e) {
      noteDone();
    }
   @Override public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
      noteDone();
    }
   
   private synchronized void noteDone() {
      is_done = true;
      notifyAll();
    }
   
   synchronized void waitForRendered() {
      while (!is_done) {
         try {
            wait();
          }
         catch (InterruptedException e) { }
       }
    }
   
}       // end of inner class TreeImageListener

       

/********************************************************************************/
/*										*/
/*	Image management							*/
/*										*/
/********************************************************************************/

private void loadFileImage(JLabel lbl,Rectangle2D r,String cnts)
{
   File f1 = new File(cnts);
   if (f1.exists()) {
      ImageIcon ii = new ImageIcon(f1.getAbsolutePath(),cnts);
      setupIcon(lbl,r,ii);
    }
}


private void loadUrlImage(JLabel lbl,Rectangle2D r,String cnts)
{
   try {
      URL u = new URI(cnts).toURL();
      ImageIcon ii = new ImageIcon(u,cnts);
      setupIcon(lbl,r,ii);
    }
   catch (MalformedURLException | URISyntaxException e) {
      System.err.println("signmaker: Bad url " + cnts);
    }
}


private void setupIcon(JLabel lbl,Rectangle2D r,ImageIcon ii)
{
   int w = (int)(r.getWidth() * 0.90);
   int h = (int)(r.getHeight() * 0.90);
   double iw = ii.getIconWidth();
   double ih = ii.getIconHeight();
   if (iw == 0) return;

   double ws = iw/w;
   double hs = ih/h;
   double scl = Math.max(ws,hs);
   ws = ws/scl;
   hs = hs/scl;
   w = (int)(w*ws);
   h = (int)(h*hs);

   BufferedImage output = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
   Graphics g = output.createGraphics();
   g.drawImage(ii.getImage(),0,0,w,h,null);
   g.dispose();
   ii = new ImageIcon(output);

   lbl.setIcon(ii);
}



/********************************************************************************/
/*										*/
/*	Font-Awesome image management						*/
/*										*/
/********************************************************************************/

private void loadFontAwesomeImage(JLabel lbl,Rectangle2D r,String cnts)
{
   Font ft = fa_solid.deriveFont(100f);
   lbl.setFont(ft);
   lbl.setHorizontalAlignment(JLabel.CENTER);
   lbl.setText(cnts);
   lbl.setPreferredSize(null);
   Dimension sz = lbl.getPreferredSize();

   Font lastfont = lbl.getFont();
   double szw  = r.getWidth()/sz.getWidth();
   double szh = r.getHeight()/sz.getHeight();
   double scale = Math.min(szw,szh);
   float s = lastfont.getSize2D();
   s *= scale * 0.9;
   Font newfont = lastfont.deriveFont(s);
   lbl.setFont(newfont);
}



/********************************************************************************/
/*										*/
/*	QR images								*/
/*										*/
/********************************************************************************/

private void loadQRImage(JLabel lbl,Rectangle2D r,String cnts)
{
   Dimension d = r.getBounds().getSize();
   int sz = Math.min(d.width,d.height);
   try {
      File f1 = File.createTempFile("qrimage",".png");
      f1.deleteOnExit();
      BitMatrix matrix = new MultiFormatWriter().encode(cnts,
	    BarcodeFormat.QR_CODE,sz,sz);
      MatrixToImageWriter.writeToPath(matrix,"png",f1.toPath());
      ImageIcon ii = new ImageIcon(f1.getAbsolutePath());
      setupIcon(lbl,r,ii);
    }
   catch (WriterException e) {
       System.err.println("signmaker: Problem generating QR image: " + e);
    }
   catch (IOException e) {
      System.err.println("signmaker: Problem generating QR image: " + e);
    }
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setBackgroundColor(Color c)
{
   background_color = c;
}


void setForegroundColor(Color c)
{
   foreground_color = c;
}


/**
*	Set the image by name.	The name can be a url of the image.  It can also
*	be fa-xxx for a font-awesome image; sv-xxx for name from the svg image library;
*	or an image name the user has saved in the database.
**/

void setImage(String name0) throws SignMakerException
{
   String name = name0;
   image_contents = null;
   image_type = SignImageType.NONE;
   if (name == null) return;

   if (!name.startsWith("http:") && !name.startsWith("https:")) {
      String lup = lookupImage(name);
      if (lup != null) name = lup;
    }
   if (name.startsWith("http:") || name.startsWith("https:")) {
      image_type = SignImageType.URL;
      image_contents = name;
      return;
    }

   if (name.contains("/") || name.contains("\\")) {
      File f1 = new File(name);
      if (f1.exists() && f1.isAbsolute()) {
	 image_type = SignImageType.FILE;
	 image_contents = f1.getAbsolutePath();
	 return;
       }
    }
   if (name.startsWith("fa-")) {
      String fatoken = fa_map.get(name);
      if (fatoken == null) throw new SignMakerException("Unknown font-awesome image " + name);
      image_type = SignImageType.FONT_AWESOME;
      image_contents = fatoken;
      return;
    }
   if (name.startsWith("sv-")) {
      String svname = name.substring(3);
      File svgfile = findSvgLibraryFile(svname);
      if (svgfile == null) throw new SignMakerException("Unknown svg library image " + svname);
      image_type = SignImageType.FILE;
      image_contents = svgfile.getAbsolutePath();
      return;
    }

   throw new SignMakerException("Unknown image: " + name0);
}


void setQR(String name)
{
   image_contents = name;
   image_type = SignImageType.QR;
}



/********************************************************************************/
/*										*/
/*	Stored image methods							*/
/*										*/
/********************************************************************************/

private String lookupImage(String name)
{
   Connection sql = SignMaker.getSqlDatabase();
   if (sql == null) return null;
   String rslt = null;
   try {
      String q = "SELECT * FROM iQsignImages WHERE (userid = ? OR userid IS NULL) AND name = ?";
      PreparedStatement st = sql.prepareStatement(q);
      st.setInt(1,user_id);
      st.setString(2,name);
      ResultSet rs = st.executeQuery();
      if (rs.next()) {
         String url = rs.getString("url");
         String file = rs.getString("file");
         if (url != null) rslt = url;
         else if (file != null) rslt = file;
       }
      st.close();
      if (rslt != null) return rslt;
    }
   catch (SQLException e) {
      System.err.println("signmaker: Database problem: " + e);
      e.printStackTrace();
    }

   return rslt;
}


/********************************************************************************/
/*										*/
/*	SVG image library methods						*/
/*										*/
/********************************************************************************/

private File findSvgLibraryFile(String name)
{
   File f1 = svg_library;
   if (f1 == null) return null;

   if (!name.startsWith("svg/")) f1 = new File(f1,"svg");
   
   File f0 = f1;

   for (StringTokenizer tok = new StringTokenizer(name,"/"); tok.hasMoreTokens(); ) {
      String what = tok.nextToken().trim();
      if (!tok.hasMoreTokens() && !what.endsWith(".svg")) what += ".svg";
      f1 =  new File(f1,what);
    }

   if (f1.exists()) return f1;
   
   if (!name.contains("/")) {
      if (!name.endsWith(".svg")) name = name += ".svg";
      for (File f2 : f0.listFiles()) {
         if (f2.isDirectory()) {
            File f3 = new File(f2,name);
            if (f3.exists()) return f3;
          }
       }
    }

   return null;
}


/********************************************************************************/
/*										*/
/*	Font-Awesome methods							*/
/*										*/
/********************************************************************************/

private static void loadFontAwesome()
{
   String token = SignMaker.getFontAwesomeToken();

// fa_map.put("fa-cat","\f6be");
// fa_map.put("fa-hippo","\f6ed");

   try {
      String uri = "https://ka-f.fontawesome.com/releases/v6.1.1/css/free.min.css?token=" + token;
      parseFontAwesomeCss(uri);
    }
   catch (IOException e) {
      System.err.println("signmaker: Problem reading font-awesome css");
    }
}


private static void parseFontAwesomeCss(String uri) throws IOException
{
   SACParserCSS3 sacp = new SACParserCSS3();
   CssDocumentHandler hdlr = new CssDocumentHandler();
   sacp.setDocumentHandler(hdlr);
   sacp.setErrorHandler(hdlr);
   InputSource src = new InputSource(uri);
   sacp.parseStyleSheet(src);
}

private static class CssDocumentHandler extends HandlerBase {

   private List<String> current_item;

   CssDocumentHandler() {
      current_item = new ArrayList<>();
    }

   @Override public void error(CSSParseException e) { }
   @Override public void warning(CSSParseException e) { }
   @Override public void fatalError(CSSParseException e) { }


   @Override public void startSelector(SelectorList sellst) {
      current_item.clear();
      for (int i = 0; i < sellst.getLength(); ++i) {
	 Selector selitm = sellst.item(i);
	 if (selitm.getSelectorType() != Selector.SAC_DESCENDANT_SELECTOR) continue;
	 DescendantSelector dessel = (DescendantSelector) selitm;
	 Selector par = dessel.getAncestorSelector();
	 Selector sim = dessel.getSimpleSelector();
	 if (sim.getSelectorType() != Selector.SAC_PSEUDO_ELEMENT_SELECTOR) continue;
	 ElementSelector elmsel = (ElementSelector) sim;
	 String nm = elmsel.getLocalName();
	 if (!nm.equals("before")) continue;
	 checkClassSelector(par);
       }
    }


   private void checkClassSelector(Selector s)
   {
      if (s.getSelectorType() != Selector.SAC_CONDITIONAL_SELECTOR) return;
      ConditionalSelector cndsel = (ConditionalSelector) s;
      Condition c = cndsel.getCondition();
      if (c.getConditionType() != Condition.SAC_CLASS_CONDITION) return;
      AttributeCondition ac = (AttributeCondition) c;
      String nm = ac.getValue();
      if (nm.startsWith("fa-")) current_item.add(nm);
   }

   @Override public void startSelector(SelectorList sellst,Locator loc) {
      startSelector(sellst);
    }


   @Override public void endSelector(SelectorList sel) {
      current_item.clear();
    }

   @Override public void property(String name,LexicalUnit val,boolean imp,Locator loc) {
      property(name,val,imp);
    }

   @Override public void property(String name,LexicalUnit val,boolean imp) {
      if (!name.equals("content")) return;
      String str = val.getStringValue();
      if (str != null) {
	 for (String s : current_item) fa_map.put(s,str);
       }
    }

}	// end of inner class CssDocumentHandler




/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return "<IMG " + image_contents + " " + image_type + ">";
}


}	// end of class SignMakerImage




/* end of SignMakerImage.java */

