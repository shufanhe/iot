/********************************************************************************/
/*                                                                              */
/*              CatbridgeSamsungDevice.java                                     */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.catbridge;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.units.qual.s;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.brown.cs.catre.catdev.CatdevDevice;
import edu.brown.cs.catre.catre.CatreJson;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.ivy.file.IvyFile;

class CatbridgeSamsungDevice extends CatdevDevice implements CatreJson
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatbridgeSamsungDevice(CatbridgeBase bridge,CatreStore cs,Map<String,Object> map) 
{
   super(bridge.getUniverse(),bridge);
   
   JSONObject predev = new JSONObject(map);
   JSONObject dev = fixupSamsungDevice(predev);
   
   fromJson(cs,dev.toMap());
}


private CatbridgeSamsungDevice()
{
   super(null,null);
}


/********************************************************************************/
/*                                                                              */
/*      Fixup samsung device based on presentation                              */
/*                                                                              */
/********************************************************************************/

private JSONObject fixupSamsungDevice(JSONObject predev)
{
   JSONObject presentation = predev.optJSONObject("presentation");
   if (presentation == null) return predev;
   
   JSONArray paramarr = predev.optJSONArray("PARAMETERS");
   if (paramarr != null) {
      for (Object o : paramarr) {
         // fix parameter o
       }
    }
   JSONArray transarr = predev.optJSONArray("TRANSITIONS");
   if (transarr != null) {
      for (Object o : transarr) {
         // fix transition o
       }
    }
   
   return predev;
}


/********************************************************************************/
/*                                                                              */
/*      Test program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   String testf = "/pro/iot/catre/catbridge/src/samsungtest.json";
   if (args.length > 0) testf = args[0];
   
   JSONArray tests = new JSONArray();
   try {
      String cnts = IvyFile.loadFile(new File(testf));
      cnts = cnts.trim();
      if (cnts.startsWith("[")) {
         tests = new JSONArray(cnts);
       }
      else {
         JSONObject obj = new JSONObject(cnts);
         tests.put(obj);
       }
    }
   catch (IOException e) {
      System.err.println("Can't open file " + testf);
      System.exit(1);
    }
   catch (JSONException e) {
      System.err.println("Problem with JSON: " + e);
      System.exit(1);
    }
   
   CatbridgeSamsungDevice dev = new CatbridgeSamsungDevice();
   for (int i = 0; i < tests.length(); ++i) {
      JSONObject test = tests.getJSONObject(i);
      JSONObject rslt = dev.fixupSamsungDevice(test);
      System.err.println("FIXUP RESULT: " + rslt.toString(2));
    }
   
   System.exit(0);
}



}       // end of class CatbridgeSamsungDevice




/* end of CatbridgeSamsungDevice.java */

