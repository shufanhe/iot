/********************************************************************************/
/*										*/
/*		CatreSubSavableBase.java					*/
/*										*/
/*	Base class for identifiable subsavable object				*/
/*										*/
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




package edu.brown.cs.catre.catre;

import java.util.HashMap;
import java.util.Map;

abstract public class CatreSubSavableBase implements CatreSubSavable, CatreIdentifiable
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	data_uid;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatreSubSavableBase(String pfx)
{
   if (pfx != null) data_uid = pfx + CatreUtil.randomString(24);
}



protected CatreSubSavableBase(CatreStore store,Map<String,Object> map)
{
   data_uid = getSavedString(map,"_id",null);
   fromJson(store,map);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getDataUID()			{ return data_uid; }


@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = new HashMap<>();
   if (data_uid != null) rslt.put("_id",data_uid);
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   data_uid = getSavedString(map,"_id",data_uid);
}

}	// end of class CatreSubSavableBase




/* end of CatreSubSavableBase.java */

