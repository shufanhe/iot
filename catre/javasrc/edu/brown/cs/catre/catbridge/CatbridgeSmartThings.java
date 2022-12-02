/********************************************************************************/
/*                                                                              */
/*              CatbridgeSmartThings.java                                       */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.catbridge;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;



class CatbridgeSmartThings extends CatbridgeBase implements CatreBridge, CatbridgeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  access_token;
private String  api_address;
private JSONObject api_endpoint;

private static Map<String,JSONObject> known_capabilities = new HashMap<>();
private static Map<String,JSONObject> known_presentations = new HashMap<>();
private static Set<String> skip_capabilities;

static {
   skip_capabilities = new HashSet<>();
   skip_capabilities.add("ocf");
   skip_capabilities.add("custom.disabledCapabilities");
   skip_capabilities.add("execute");
   skip_capabilities.add("healthCheck");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CatbridgeSmartThings(CatbridgeSmartThings base,CatreUniverse u)
{
   super(base,u);
   CatreUser cu = u.getUser();
   CatreBridgeAuthorization ba = cu.getAuthorization("SmartThings");
   access_token = ba.getValue("AUTH_TOKEN");
   api_address = ba.getValue("AUTH_API");
}


CatbridgeSmartThings(CatreController cc)
{
   for_universe = null;
   access_token = null;
   api_address = null;
   setupRoutes(cc);
}



/********************************************************************************/
/*                                                                              */
/*      Create instance for a particular universe                               */
/*                                                                              */
/********************************************************************************/

@Override protected 
CatbridgeSmartThings createInstance(CatreUniverse u,CatreBridgeAuthorization ba)
{
   CatbridgeSmartThings cb = new CatbridgeSmartThings(this,u);
   
   return cb;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()               { return "SmartThings"; }



/********************************************************************************/
/*                                                                              */
/*      Find devices via the bridge                                             */
/*                                                                              */
/********************************************************************************/

@Override
public List<CatreDevice> findDevices()
{
   List<CatreDevice> rslt = new ArrayList<>();
   
   loadDevices();
   
   return rslt;
}



private void loadDevices()
{
  JSONArray arr = issueArrayCommand("devices");
  System.err.println("FOUND " + arr);
  if (arr == null) return;
  
  for (int i = 0; i < arr.length(); ++i) {
     JSONObject dev = arr.getJSONObject(i);
     String devid = dev.getString("deviceId");
     String devname = dev.getString("name");
     String devlabel = dev.getString("label");
     CatbridgeSmartThingsDevice device = new CatbridgeSmartThingsDevice(this,devlabel,devid);
     JSONArray comps = dev.getJSONArray("components");
     for (int j = 0; j < comps.length(); ++j) {
        JSONObject comp = comps.getJSONObject(j);
        JSONArray caps = comp.getJSONArray("capabilities");
        for (int k = 0; k < caps.length(); ++k) {
           JSONObject cap = caps.getJSONObject(k);
           String capid = cap.getString("id");
           int vid = cap.getInt("version");
           addCapability(device,capid,vid);      
         }
      }
   }
}



private void addCapability(CatbridgeSmartThingsDevice dev,String capid,int vid)
{
   if (skip_capabilities.contains(capid)) return;
   Map<String,CatreParameter> params = new HashMap<>();
   
   String key = capid + "__" + vid;
   JSONObject capd = known_capabilities.get(key);
   JSONObject pred = known_presentations.get(key);
   if (capd == null) {
      String cmd = "capabilities " + capid + " " + vid;
      capd = issueObjectCommand(cmd);
      if (capd == null) return;
      known_capabilities.put(key,capd);
      String cmd1 = "capabilities:presentation " + capid + " " + vid;
      pred = issueObjectCommand(cmd1);
      known_presentations.put(key,pred);
    }
   String sts = capd.getString("status");
   if (sts != null && !sts.equals("live")) return;
   JSONObject attrs = capd.getJSONObject("attributes");
   JSONArray names = attrs.names();
   if (names != null) {
      for (int i = 0; i < names.length(); ++i) {
         String attrnm = names.getString(i);
         JSONObject attr = attrs.getJSONObject(attrnm);
         JSONObject present = null;
         JSONObject cond = null;
         if (pred != null) {
            JSONArray prflds = pred.optJSONArray("detailView");
            if (prflds != null) {
               for (int j = 0; j < prflds.length(); ++j) {
                  JSONObject p = prflds.getJSONObject(j);
                  String dtyp = p.getString("displayType");
                  JSONObject dval = p.getJSONObject(dtyp);
                  String vname = dval.optString("value",null);
                  if (vname == null) {
                     JSONObject sval = dval.optJSONObject("state");
                     if (sval != null) vname = sval.optString("value",null);
                   }
                  if (vname != null && vname.equals(attrnm + ".value")) {
                     present = p;
                     break;
                   }
                  else if (names.length() == 1) {
                     present = p;
                     break;
                   }
                }
             }
            JSONObject auto = pred.getJSONObject("automation");
            JSONArray conds = auto.getJSONArray("conditions");
            if (conds != null) {
               for (int j = 0; j < conds.length(); ++j) {
                  JSONObject c0 = conds.getJSONObject(j);
                  String dtype = c0.getString("displayType");
                  JSONObject c1 = c0.getJSONObject(dtype);
                  String vname = c1.optString("value",null);
                  if (names.length() == 1 || (vname != null && vname.equals(attrnm + ".value"))) {
                     cond = c0;
                     break;
                   }
                }
             }
          }
         CatreParameter param = getParameter(attrnm,attr,present,cond);
         if (param != null) {
            param.setIsSensor(true);
            dev.addParameter(param);
            params.put(attrnm,param);
          }
         else {
            CatreLog.logE("CATBRIDGE","Unknown parameter: " + attr);
          }
         // handle enum commands
         // handle presentation commands
       }
    }
   
   JSONObject cmds = capd.getJSONObject("commands");
   JSONArray cnames = cmds.names();
   if (cnames != null) {
      for (int i = 0; i < cnames.length(); ++i) {
         String cname = cnames.getString(i);
         JSONObject cmdobj = cmds.getJSONObject(cname);
         String aname = cmdobj.optString("name",cname);
         JSONArray args = cmdobj.getJSONArray("arguments");
         for (int j = 0; j < args.length(); ++j) {
            JSONObject arg = args.getJSONObject(j);
            String argnm = arg.getString("name");
            CatreParameter baseparam = params.get(argnm);
            CatreParameter argparam = getParameter(argnm + "_SET",arg,null,null);
            // create new parameter from schema, name = <name>_SET
          }
         // create set transition for dev with baseparam, default value for state,
         //             name,routine name,argument parameter,default value for arg,
         //             force
       }
    }
}



private List<String> getStringArray(JSONArray arr)
{
   List<String> rslt = new ArrayList<>();
   for (int i = 0; i < arr.length(); ++i) {
      rslt.add(arr.getString(i));
    }
   return rslt;
}



private CatreParameter getParameter(String attrnm,JSONObject attr,JSONObject present,JSONObject cond)
{
   CatreLog.logD("PARAMETER " + attrnm + " " + attr + " " + present + " " + cond);
   JSONObject schema = attr.getJSONObject("schema");
   CatreParameter param = null;
   String typ = schema.getString("type");
   JSONObject props = schema.optJSONObject("properties");
   if (props == null) return null;
   JSONObject value = props.getJSONObject("value");
   JSONArray enm = value.optJSONArray("enum");
   String vtype = value.optString("type");
   // TODO: handle units
   
   switch (typ) {
      case "object" :
         if (enm != null) {
            param = getUniverse().createEnumParameter(attrnm,getStringArray(enm));
          }
         else if (vtype.equals("integer")) {
            // get range from present or cond
            int min = value.optInt("minimum",Integer.MIN_VALUE);
            int max = value.optInt("maximum",Integer.MAX_VALUE);
            param = getUniverse().createIntParameter(attrnm,min,max);
          }
         else if (vtype.equals("number")) {
            double min = value.optDouble("minimum",Double.MIN_VALUE);
            double max = value.optDouble("maximum",Double.MAX_VALUE);
            param = getUniverse().createRealParameter(attrnm,min,max);
          }
         else if (vtype.equals("string")) {
            if (attrnm.equals("color")) {
               param = getUniverse().createColorParameter(attrnm);
             }
            else {
               param = getUniverse().createStringParameter(attrnm);
             }
          }
         break;
    }
   if (param != null) {
      String ttl = schema.optString("title");
      if (ttl == null) ttl = value.optString("title");
      if (ttl != null) {
         param.setLabel(param.getLabel() + " " + ttl);
       }
      param.setIsSensor(true);
    }
   else {
      CatreLog.logE("CATBRIDGE","Unknown parameter: " + attrnm + ": " + attr);
    }
   
   return param;
}





/********************************************************************************/
/*                                                                              */
/*      Issue smartthings command                                               */
/*                                                                              */
/********************************************************************************/

private JSONObject issueObjectCommand(String cmd)
{
   String rslt = issueCommand(cmd);
   if (rslt == null || !rslt.startsWith("{")) return null;
   return new JSONObject(rslt);
}


private JSONArray issueArrayCommand(String cmd)
{
   String rslt = issueCommand(cmd);
   if (rslt == null || !rslt.startsWith("[")) return null;
   return new JSONArray(rslt);
}



private String issueCommand(String cmd)
{
   String cm = "smartthings " + cmd + " --json";
   // cm += " --token=" + access_token;                 // requires permanent token
   // for now, assume .config/@smarttthings/cli/credentials.json is up to date
   
   CatreLog.logD("CATBRIDGE","Issue command: " + cm);
   try {
      IvyExec ex = new IvyExec(cm,IvyExec.READ_OUTPUT);
      InputStream ins = ex.getInputStream();
      String cnts = IvyFile.loadFile(ins);
      ins.close();
      return cnts;
    }
   catch (IOException e) {
      CatreLog.logE("CATBRIDGE","Problem execution smartthings command",e);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Handle listening to smartthings events                                  */
/*                                                                              */
/********************************************************************************/

private void setupRoutes(CatreController cc)
{
   cc.addRoute("POST","/smartthings",this::handleSmartThings);
}


private Response handleSmartThings(IHTTPSession s,CatreSession cs) 
{
   CatreLog.logI("CATBRIDGE","Recieved from smartthings: " +   s.getUri());
   for (String key : s.getParameters().keySet()) {
       StringBuffer buf = new StringBuffer();
       for (String sv : s.getParameters().get(key)) {
          buf.append(sv);
          buf.append(",");
        }
       CatreLog.logI("CATBRIDGE","\tPARAMETER " + key + " = " + buf.toString());
    }
   
   Response resp = handleLifecycle(s,cs);
   if (resp != null) return resp;
   
   
   return cs.jsonResponse("STATUS","OK");
}



private Response handleLifecycle(IHTTPSession s,CatreSession cs)
{
   String lifecycle = cs.getParameter(s,"lifecycle");
   JSONObject empty = new JSONObject();
   
   if (lifecycle != null) {
      switch (lifecycle) {
         case "CONFIRMATION" :
            JSONObject cfdata = new JSONObject(cs.getParameter(s,"confirmationData"));
            CatreLog.logI("CATBRIDGE","Confirmation: appId = " + cfdata.get("appId"));
            CatreLog.logI("CATBRIDGE","Confirmation: url = " + cfdata.get("confirmationUrl"));
            return cs.jsonResponse("targetUrl","https://sherpa.cs.brown.edu:3332/smartthings");
         case "CONFIGURATION" :
            JSONObject cnfg = new JSONObject(cs.getParameter(s,"configurationData"));
            JSONObject sets = new JSONObject(cs.getParameter(s,"settings"));
            return handleConfiguration(s,cs,cnfg,sets);
         case "INSTALL" :
            // TODO: handle install
            return cs.jsonResponse("installData",empty);
         case "UPDATE" :
            // TODO: handle update
            return cs.jsonResponse("updateData",empty);
         case "EVENT" :
            // TODO: handle Event
            return cs.jsonResponse("eventData",empty);
         case "OAUTH_CALLBACK" :
            return cs.jsonResponse("oAuthCallbackData",empty);
         case "UNINSTALL" :
            // TODO: handle uninstall
            return cs.jsonResponse("uninstallData",empty);
         case "PING" :
            String pd = cs.getParameter(s,"pingData");
            JSONObject prslt = new JSONObject(pd);
            return cs.jsonResponse("pingData",prslt);
       }
    }
   
   return null;
}


private Response handleConfiguration(IHTTPSession s,CatreSession cs,JSONObject cfdata,JSONObject sets)
{
   switch (cfdata.getString("phase")) {
      case "INITIALIZE" :
         JSONObject initdata = new JSONObject();
         initdata.put("name","CATRE");
         initdata.put("description","CATRE Controller");
         initdata.put("id","CatreApp-" + getUniverse().getDataUID());
         List<String> perms = new ArrayList<>();
         perms.add("r:devices:*");
         perms.add("w:devices:*");
         perms.add("x:devices:*");
         initdata.put("permissions",perms);
         JSONObject cfd = new JSONObject();
         cfd.put("initialize",initdata);
         return cs.jsonResponse("configurationData",cfd.toString());
      case "PAGE" :
         break;
    }
   
   return null;
}


@Override public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   CatreLog.logD("CATBRIDGE","SMARTTHINGS CREATE");
   return null;
}

@Override public CatreTransition createTransition(CatreDevice cd,CatreStore cs,Map<String,Object> map)
{
   return null;
}


}       // end of class CatbridgeSmartThings




/* end of CatbridgeSmartThings.java */

