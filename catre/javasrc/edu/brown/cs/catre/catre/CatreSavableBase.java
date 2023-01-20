/********************************************************************************/
/*										*/
/*		CatreSavableBase.java						*/
/*										*/
/*	Base class for all savable items					*/
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

abstract public class CatreSavableBase implements CatreSavable
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	data_uid;
private boolean is_stored;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatreSavableBase(String pfx)
{
   data_uid = pfx + CatreUtil.randomString(24);
   is_stored = false;
}

protected CatreSavableBase(CatreStore store)
{
   data_uid = null;
   is_stored = true;
}

protected CatreSavableBase(CatreStore store,Map<String,Object> map)
{
   data_uid = map.get("_id").toString();
   is_stored = true;
   store.recordObject(this);

   fromJson(store,map);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getDataUID()			{ return data_uid; }

public boolean isStored()				{ return is_stored; }

public void setStored() 				{ is_stored = true; }


public void setDataUID(String uid) throws CatreException
{
   throw new CatreException("Can't set uid");
}


@Override public Map<String,Object> toJson()
{
   Map<String,Object> obj = new HashMap<>();
   obj.put("_id",data_uid);
   return obj;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   data_uid = map.get("_id").toString();
}




}	// end of class CatreSavableBase




/* end of CatreSavableBase.java */

