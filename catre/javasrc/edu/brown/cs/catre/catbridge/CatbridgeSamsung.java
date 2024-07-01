/********************************************************************************/
/*                                                                              */
/*              CatbridgeSamsung.java                                           */
/*                                                                              */
/*      Bridge to smartthings core-sdk                                          */
/*                                                                              */
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
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




package edu.brown.cs.catre.catbridge;

import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;

class CatbridgeSamsung extends CatbridgeBase 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          auth_uid;
private String          auth_pat;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatbridgeSamsung(CatreController cc)
{
   auth_uid = null;
   auth_pat = null;
}


CatbridgeSamsung(CatbridgeBase base,CatreUniverse u,CatreBridgeAuthorization ba)
{
   super(base,u);
   auth_uid = u.getUser().getDataUID();
   auth_pat = ba.getValue(0,"AUTH_TOKEN");
}


protected CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization ba)
{
   return new CatbridgeSamsung(this,u,ba);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()               { return "samsung"; }




@Override protected Map<String,Object> getAuthData()
{
   Map<String,Object> rslt = super.getAuthData();
   rslt.put("uid",auth_uid);
   rslt.put("token",auth_pat);
   return rslt;
}


@Override protected String getUserId()          { return auth_uid; }


@Override public JSONObject getBridgeInfo()
{
   JSONObject rslt = super.getBridgeInfo();
   
   JSONObject f1 = buildJson("KEY","AUTH_TOKEN","LABEL","Access Token","VALUE",auth_pat,
         "HINT","Personal access token from SmartThings","TYPE","STRING");
   rslt.put("FIELDS",buildJsonArray(f1));  
   rslt.put("SINGLE",true);
   
   String desc = "You should login to SmartThings and request a personal access token. " +
         "Then provide this token above.";
   rslt.put("HELP",desc);
   
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Device creation                                                         */
/*                                                                              */
/********************************************************************************/

@Override public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   CatreLog.logD("CATBRIDGE","Create samsung device " + map);
   
   return new CatbridgeSamsungDevice(this,cs,map); 
}






}       // end of class CatbridgeSamsung




/* end of CatbridgeSamsung.java */

