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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

private Map<String,JSONObject> capability_map;
private Map<String,JSONObject> condition_map;
private Map<String,JSONObject> action_map;
private Map<String,String> reference_map;
private Set<String> referenced_set;

private static String [] PARAM_FIELDS = { 
   "DEFAULT_UNIT","MIN","MAX","ISSENSOR",
      "UNITS","TYPE","NAME",
      "LABEL","DESCRIPTION","VALUES"
};

private static String [] TRANSITION_FIELDS = {
   "NAME","TYPE","LABEL","DESCRIPTION",
};


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
   capability_map = new HashMap<>();
   condition_map = new HashMap<>();
   action_map = new HashMap<>();
   reference_map = new HashMap<>();
   referenced_set = new HashSet<>();
    
   JSONObject presentation = predev.optJSONObject("presentation");
   
   analyzePresentation(presentation);
   
   JSONArray caparr = predev.optJSONArray("capabilities");
   for (int i = 0; i < caparr.length(); ++i) {
      JSONObject cap = caparr.getJSONObject(i);
      String nm = cap.getString("id");
      capability_map.put(nm,cap);
      analyzeCapability(cap);
    }
   
   JSONObject rslt = new JSONObject();
   rslt.put("DESCRIPTION",predev.get("DESCRIPTION"));
   rslt.put("LABEL:",predev.get("LABEL"));
   rslt.put("NAME",predev.get("NAME"));
   rslt.put("UID",predev.get("UID"));
   rslt.put("BRIDGE",predev.get("BRIDGE"));
   
   JSONArray paramarr = predev.optJSONArray("PARAMETERS");
   if (paramarr != null) {
      JSONArray params = new JSONArray();
      for (int i = 0; i < paramarr.length(); ++i) {
         JSONObject p = paramarr.getJSONObject(i);
         JSONObject p1 = fixParameter(p,rslt);
         if (p1 != null) params.put(p1);
       }
      rslt.put("PARAMETERS",params);
      
    }
   JSONArray transarr = predev.optJSONArray("TRANSITIONS");
   if (transarr != null) {
      JSONArray trans = new JSONArray();
      for (int i = 0; i < transarr.length(); ++i) {
         JSONObject t = transarr.getJSONObject(i);
         JSONObject t1 = fixTransition(t,presentation,rslt);
         if (t1 != null) trans.put(t1);
       }
      rslt.put("TRANSITIONS",trans);
    }
   
   capability_map = null;
   condition_map = null;
   action_map = null;
   reference_map = null;
   referenced_set = null;
   
   return rslt;
}



private void analyzePresentation(JSONObject presentation)
{
   JSONObject use = presentation.optJSONObject("automation");
   if (use == null) use = presentation.optJSONObject("dashboard");
   if (use == null) return;
   JSONArray conds = use.getJSONArray("conditions");
   for (int i = 0; i < conds.length(); ++i) {
      JSONObject cond = conds.getJSONObject(i);
      condition_map.put(cond.getString("capability"),cond);
      checkReference(cond);
    }
   JSONArray acts = use.getJSONArray("actions");
   for (int i = 0; i < acts.length(); ++i) {
      JSONObject act = acts.getJSONObject(i);
      action_map.put(act.getString("capability"),act);
      checkReference(act);
    }
}


private void analyzeCapability(JSONObject capability)
{
}


private void checkReference(JSONObject obj)
{
   String sup = obj.optString("supportedValues",null);
   if (sup == null) {
      JSONObject lst = obj.optJSONObject("list");
      if (lst != null) sup = lst.optString("supportedValues",null);
    }
   if (sup != null && !sup.isEmpty()) {
      int idx = sup.lastIndexOf(".");
      String capid = obj.getString("capability");
      String refcap = sup.substring(0,idx);
      reference_map.put(capid,refcap);
      referenced_set.add(refcap);
    }
}


private JSONObject fixParameter(JSONObject prepar,JSONObject device)
{
   JSONObject rslt = new JSONObject(prepar,PARAM_FIELDS);
   String capid = prepar.getString("capabilityid");
   JSONObject cap = capability_map.get(capid);
   String name = prepar.getString("NAME"); 
   JSONObject ocap = condition_map.get(capid);
   boolean use = true;
   if (referenced_set.contains(name)) {
      rslt.put("ISSENSOR",false);
      use = true;
    }
   if (!cap.optString("status","live").equals("live")) use = false;
   
   boolean sensor = rslt.optBoolean("ISSENSOR",false);
   if (use) {
      switch (rslt.getString("TYPE")) {
         case "INTEGER" :
         case "REAL" :
            Number minv = rslt.optNumber("MIN");
            Number maxv = rslt.optNumber("MAX");
            if (minv == null || maxv == null) use = false;
            break;
         case "ENUM" :
            JSONArray vals = rslt.optJSONArray("VALUES");
            if (vals == null || vals.isEmpty()) use = false;
            break;
         case "STRING" :
            if (sensor) {
               JSONArray svals = getValues(ocap);
               if (svals != null) {
                  rslt.put("TYPE","ENUM");
                  rslt.put("VALUES",svals);
                  break;
                }
               String sup = cap.optString("supportedValues",null);
               if (sup == null) sup = reference_map.get(capid);
               System.err.println("FOUND " + sup);
               if (sup != null) {
                  rslt.put("TYPE","ENUMREF");
                  JSONObject pref = new JSONObject();
                  pref.put("DEVICE",device.getString("UID"));
                  pref.put("PARAMETER",sup);
                  rslt.put("PARAMREF",pref);
                }
               else use = false;
             }
            break;
         case "STRINGLIST" :
         case "SET" :
            JSONArray svals = getValues(ocap);
            if (svals != null) {
               rslt.put("TYPE","ENUM");
               rslt.put("VALUES",svals);
               if (!sensor) use = false;             // not needed if values are there?
             }
            if (sensor && rslt.opt("VALUES") == null) use = false;
            break;
       }
    }
   
   if (!use) return null;
   
   return rslt;      
}


private JSONArray getValues(JSONObject obj)
{
   if (obj == null) return null;
   JSONObject lst = obj.optJSONObject("list");
   if (lst == null) return null;
   JSONArray arr = lst.optJSONArray("alternatives");
   if (arr == null) return null;
   JSONArray rslt = new JSONArray();
   for (int i = 0; i < arr.length(); ++i) {
      JSONObject alt = arr.getJSONObject(i);
      if (alt.optString("type","active").equals("active")) {
         String k = alt.getString("key");
         rslt.put(k);
       }
    }
   if (rslt.isEmpty()) return null;
   return rslt;
}



private JSONObject fixTransition(JSONObject pretrans,JSONObject presentation,JSONObject device)
{
   JSONObject rslt = new JSONObject(pretrans,TRANSITION_FIELDS);
// String capid = pretrans.getString("capabilityid");
// JSONObject cap = capability_map.get(capid);
// String name = pretrans.getString("NAME"); 
// JSONObject ocap = condition_map.get(capid);

   JSONObject odflts = pretrans.getJSONObject("DEFAULTS");
   JSONArray oparams = odflts.getJSONArray("PARAMETERS");
   JSONObject dflts = new JSONObject();
   JSONArray params = new JSONArray();
   
   for (int i = 0; i < oparams.length(); ++i) {
      JSONObject param = oparams.getJSONObject(i);
      JSONObject p1 = fixTransitionParameter(param,pretrans,device);
      if (p1 != null) params.put(p1);
      else return null;
    }
   
   dflts.put("PARAMETERS",params);
   rslt.put("DEFAULTS",dflts);
   
   return rslt;
}



private JSONObject fixTransitionParameter(JSONObject param,JSONObject trans,JSONObject device)
{
   String capid = trans.optString("capabilityid");
   JSONObject rslt = new JSONObject(param,PARAM_FIELDS);
   JSONObject cap = capability_map.get(capid);
// String name = param.getString("NAME"); 
   JSONObject ocap = condition_map.get(capid);
   boolean use = true;
   if (!cap.optString("status","live").equals("live")) {
      return null;
    }
   switch (rslt.getString("TYPE")) {
      case "INTEGER" :
      case "REAL" :
         Number minv = rslt.optNumber("MIN");
         Number maxv = rslt.optNumber("MAX");
         if (minv == null || maxv == null) use = false;
         break;
      case "ENUM" :
         JSONArray vals = rslt.optJSONArray("VALUES");
         if (vals == null || vals.isEmpty()) use = false;
         break;
      case "STRING" :
         JSONArray svals = getValues(ocap);
         if (svals != null) {
            rslt.put("TYPE","ENUM");
            rslt.put("VALUES",svals);
            break;
          }
         String sup = cap.optString("supportedValues",null);
         if (sup == null) sup = reference_map.get(capid);
         System.err.println("FOUND " + sup);
         if (sup != null) {
            rslt.put("TYPE","ENUMREF");
            JSONObject pref = new JSONObject();
            pref.put("DEVICE",device.getString("UID"));
            pref.put("PARAMETER",sup);
            rslt.put("PARAMREF",pref);
          }
         else use = false;
         break;
      case "STRINGLIST" :
      case "SET" :
         JSONArray svals1 = getValues(ocap);
         if (svals1 != null) {
            rslt.put("TYPE","ENUM");
            rslt.put("VALUES",svals1);
          }
         else use = false;
         break;
    }
   
   if (!use) return null;
   
   return rslt;
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

