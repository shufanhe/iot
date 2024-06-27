/********************************************************************************/
/*										*/
/*		CattestSetup.java						*/
/*										*/
/*	Setup CATRE for our own home						*/
/*										*/
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss, Molly E. McHenry         */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




package edu.brown.cs.catre.cattest;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreJson;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.file.IvyFile;

public class CattestSetup implements CattestConstants, CatreJson
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   CattestSetup setup = new CattestSetup(args);

   setup.runSetup();
}


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean do_clean = false;

private static final String IQSIGN = "iQsign_BGR8yt9M_1";
private static final String MONITOR = "COMPUTER_MONITOR_BROWN-F1QWPJJ9";


private static Set<String> OK_DEVICES;

static {
   OK_DEVICES = new HashSet<>();
   OK_DEVICES.add("GoogleCalendar_spr");
   OK_DEVICES.add("iQsign Office");
   OK_DEVICES.add("Weather-Rehoboth,MA,US");
   OK_DEVICES.add(MONITOR);
   
}
      


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private CattestSetup(String [] args)
{
   System.out.println();
   for (String arg : args) {
      System.out.println("ARG: " + arg);
    }
   System.out.println();
   
   boolean local = true;
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-r")) {                           // -remote
         CattestUtil.setTestHost(TEST_HOST1);
         local = false;
       }
      else if (args[i].startsWith("-c")) {                      // -clean
         do_clean = true;
       }
    }
   
   if (local) {
      CattestUtil.startCatre();
    }
}




/********************************************************************************/
/*										*/
/*	Running methods 							*/
/*										*/
/********************************************************************************/

private void runSetup()
{
   File logindata = new File("/private/iot/secret/catrelogin");
   if (!logindata.exists()) {
      logindata = new File("/pro/iot/secret/catrelogin");
    }
   

   JSONObject data = null;
   try {
      data = new JSONObject(IvyFile.loadFile(logindata));
    }
   catch (IOException e) {
      System.exit(1);
    }
   String user = data.getString("user");
   String pwd = data.getString("password");
   String email = data.getString("email");
   String stacc = data.getString("smartthings-spr");
   String genuid = data.getString("generic_uid");
   String genpat = data.getString("generic_pat");
   String iqsuid = data.getString("iqsign_user");
   String iqspat = data.getString("iqsign_token");
   String gcalnms = data.getString("gcal_names");

   String v1 = CatreUtil.secureHash(pwd);
   String v2 = v1 + user;
   String v3 = CatreUtil.secureHash(v2);

   JSONObject rslt2 = CattestUtil.sendGet("/login");
   String sid = rslt2.getString("CATRESESSION");
   String salt = rslt2.getString("SALT");

   String v4 = v3 + salt;
   String v5 = CatreUtil.secureHash(v4);
   
   JSONObject rslt3 = CattestUtil.sendJson("POST","/login",
	 "CATRESESSION",sid,"SALT",salt,
	 "username",user,"password",v5);
   if (!rslt3.getString("STATUS").equals("OK")) {
      // if login fails, try to register
      sid = rslt3.getString("CATRESESSION");
      JSONObject rslt1 = CattestUtil.sendJson("POST","/register",
	    "CATRESESSION",sid,
	    "username",user,
	    "email",email,
	    "password",v3,
	    "universe","MyWorld");
       sid = rslt1.getString("CATRESESSION");
    }

   JSONObject rslt5 = CattestUtil.sendJson("POST","/bridge/add",
	 "CATRESESSION",sid,"BRIDGE","generic",
	 "AUTH_UID",genuid,
	 "AUTH_PAT",genpat);
   sid = rslt5.getString("CATRESESSION");

   JSONObject rslt6 = CattestUtil.sendJson("POST","/bridge/add",
	 "CATRESESSION",sid,"BRIDGE","iqsign",
	 "AUTH_UID",iqsuid,
	 "AUTH_PAT",iqspat);
   sid = rslt6.getString("CATRESESSION");

   JSONObject rslt6a = CattestUtil.sendJson("POST","/bridge/add",
	 "CATRESESSION",sid,"BRIDGE","gcal",
	 "AUTH_CALENDARS",gcalnms);
   CatreLog.logI("CATTEST","Add gcal bridge = " + rslt6a.toString(2));

   JSONObject rslt4 = CattestUtil.sendJson("POST","/bridge/add",
	 "CATRESESSION",sid,"BRIDGE","samsung",
	 "AUTH_TOKEN",stacc);
   sid = rslt4.getString("CATRESESSION");

   JSONObject rslt7 = CattestUtil.sendJson("GET","/universe",
	 "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Universe = " + rslt7.toString(2));
   

   JSONObject devjson = buildJson("VTYPE","Weather","CITY","Rehoboth,MA,US",
	 "UNITS","imperial");
   JSONObject rslt8 = CattestUtil.sendJson("POST","/universe/addvirtual",
	 "CATRESESSION",sid,"DEVICE",devjson);
   CatreLog.logI("CATTEST","Add Virtual = " + rslt8.toString(2));

   JSONObject rslt9 = CattestUtil.sendJson("GET","/universe",
	 "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Universe = " + rslt9.toString(2));
   
   JSONObject rslt9a = CattestUtil.sendJson("POST","/universe/discover",
         "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Discover = " + rslt9a.toString(2));
   
   JSONObject rslt9b = CattestUtil.sendJson("GET","/universe",
	 "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Universe = " + rslt9b.toString(2));  
   
   if (do_clean) {
      cleanUniverse(rslt9b,sid);
      JSONObject rslt9c = CattestUtil.sendJson("GET","/universe",
            "CATRESESSION",sid);
      CatreLog.logI("CATTEST","Universe after cleaning = " + rslt9c.toString(2)); 
    }
   
   JSONObject cond1 = buildJson("TYPE","Parameter",
	 "PARAMREF",buildJson("DEVICE",MONITOR,"PARAMETER","Presence"),
               "NAME","Working at home",
               "LABEL","Check if working at home",
               "USERDESC",false,
	       "STATE","WORKING",
	       "TRIGGER",false);
   JSONObject act0 = buildJson("TRANSITION",
	 buildJson("DEVICE",IQSIGN,"TRANSITION","setSign"),
         "NAME","SetSign=WorkingAtHome",
         "LABEL","Set sign to Working At Home",
	 "PARAMETERS",buildJson("setTo","Working at Home"));
   JSONObject rul0 = buildJson("_id","RULE_aIRlbJhDwWdsjyjjnUtcfPYc",
         "NAME","Working at home",
         "LABEL","Set sign to Working at Home",
         "USERDESC",false,
	 "PRIORITY",500.0,
	 "CONDITIONS",buildJsonArray(cond1),
         "TRIGGER",false,
         "DEVICEID",IQSIGN,
	 "ACTIONS",buildJsonArray(act0));
   JSONObject rslt10 = CattestUtil.sendJson("POST","/rule/add",
	 "CATRESESSION",sid,"RULE",rul0);
   CatreLog.logI("CATTEST","Add Rule = " + rslt10.toString(2));

   JSONObject rslt11 = CattestUtil.sendJson("GET","/rules",
	 "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Rules: " + rslt11.toString(2));
   
   JSONObject rslt12 = CattestUtil.sendJson("GET","/universe",
	 "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Universe = " + rslt12.toString(2));
   
   System.exit(0);
}


/********************************************************************************/
/*                                                                              */
/*      Clean up any mess in original universe                                  */
/*                                                                              */
/********************************************************************************/



private void cleanUniverse(JSONObject u,String sid)
{
   JSONArray devs = u.getJSONArray("DEVICES");
   for (int i = 0; i < devs.length(); ++i) {
      JSONObject dev = devs.getJSONObject(i);
      String nm = dev.getString("NAME");
      if (OK_DEVICES.contains(nm)) continue;
      String br = dev.optString("BRIDGE","NONE");
      if (br.equals("samsung")) continue;
      CattestUtil.sendJson("POST","/universe/removedevice",
            "CATRESESSION",sid,
            "DEVICEID",dev.getString("UID"));
    }
   
   JSONObject pgm = u.getJSONObject("PROGRAM");
   
   JSONArray shrd = pgm.getJSONArray("SHARED");
   for (int i = 0; i < shrd.length(); ++i) {
      JSONObject scond = shrd.getJSONObject(i);
      String nm = scond.getString("NAME");
      if (nm.equals("ALWAYS")) continue;
      CattestUtil.sendJson("POST","/universe/unshareCondition",
            "CATRESESSION",sid,
            "CONDNAME",nm);
    }
   
   JSONArray rules = pgm.getJSONArray("RULES");
   for (int i = 0; i < rules.length(); ++i) {
      JSONObject rule = rules.getJSONObject(i);
      String id = rule.getString("_id");
      CattestUtil.sendJson("POST",
            "/rule/remove",
            "CATRESESSION",sid,
            "RULEID",id);
    }
}




}	// end of class CattestSetup




/* end of CattestSetup.java */

