/********************************************************************************/
/*										*/
/*		CatreJson.java							*/
/*										*/
/*	Conversion methods to and from JSON					*/
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;


public interface CatreJson
{


/********************************************************************************/
/*										*/
/*	Methods to create JSON objects						*/
/*										*/
/********************************************************************************/

public default String [] getUIDArrayToSave(List<? extends CatreIdentifiable> elts)
{
   String [] rslt = new String[elts.size()];
   int ct = 0;
   for (CatreIdentifiable s : elts) {
      rslt[ct++] = s.getDataUID();
    }
   return rslt;
}


public default List<Object> getSubObjectArrayToSave(Collection<? extends CatreSubSavable> elts)
{
   List<Object> rslt = new ArrayList<>();
   for (CatreSubSavable s : elts) {
      rslt.add(s.toJson());
    }
   return rslt;
}


public default String getUIDToSave(CatreIdentifiable e)
{
   if (e == null) return null;

   return e.getDataUID();
}



/********************************************************************************/
/*										*/
/*	Methods to return basic objects from JSON				*/
/*										*/
/********************************************************************************/

public default String getSavedString(Map<String,Object> map,String key,String dflt)
{
   Object ov = map.get(key);
   if (ov == null) ov = dflt;
   if (ov == null) return null;

   return ov.toString();
}

public default long getSavedLong(Map<String,Object> map,String key,Number v)
{
   Object ov = map.get(key);
   if (ov != null) {
      if (ov instanceof String) v = Long.valueOf((String) ov);
      else if (ov instanceof Number) v = (Number) ov;
    }
   return v.longValue();
}


@SuppressWarnings("unchecked")
public default <T extends Enum<T>> T getSavedEnum(Map<String,Object> map,String key,T dflt)
{
   Enum<?> v = dflt;
   String s = getSavedString(map,key,null);
   if (s == null || s.length() == 0) return dflt;
   Object [] vals = dflt.getClass().getEnumConstants();
   if (vals == null) return null;

   for (int i = 0; i < vals.length; ++i) {
      Enum<?> e = (Enum<?>) vals[i];
      if (e.name().equals(s)) {
	 v = e;
	 break;
       }
    }

   return (T) v;
}


public default int getSavedInt(Map<String,Object> map,String key,Number v)
{
   Object ov = map.get(key);
   if (ov != null) {
      if (ov instanceof String) v = Integer.valueOf((String) ov);
      else if (ov instanceof Number) v = (Number) ov;
    }
   return v.intValue();
}


public default Integer getOptSavedInt(Map<String,Object> map,String key,Number v)
{
   Object ov = map.get(key);
   if (ov == null) return null;
   if (ov != null) {
      if (ov instanceof String) v = Integer.valueOf((String) ov);
      else if (ov instanceof Number) v = (Number) ov;
    }
   if (v == null) return null; 
   int vi = v.intValue();
   if (vi == Integer.MAX_VALUE || vi == Integer.MIN_VALUE) return null;
   return vi;
}


public default double getSavedDouble(Map<String,Object> map,String key,Number v)
{
   Object ov = map.get(key);
   if (ov != null) {
      if (ov instanceof String) v = Double.valueOf((String) ov);
      else if (ov instanceof Number) v = (Number) ov;
    }
   return v.doubleValue();
}

public default Double getOptSavedDouble(Map<String,Object> map,String key,Number v)
{
   Object ov = map.get(key);
   if (ov != null) {
      if (ov instanceof String) v = Double.valueOf((String) ov);
      else if (ov instanceof Number) v = (Number) ov;
    }
   if (v == null) return null;
   
   if (v instanceof Double) {
      Double dv = (Double) v;
      if (dv.isInfinite() || dv.isNaN()) return null;
    }
   
   return v.doubleValue();
}

public default Boolean getSavedBool(Map<String,Object> map,String key,Boolean v)
{
   Object ov = map.get(key);
   if (ov != null) {
      if (ov instanceof String) v = Boolean.getBoolean((String) ov);
      else if (ov instanceof Number) {
	 Number nv = (Number) ov;
	 v = nv.longValue() != 0;
       }
      else if (ov instanceof Boolean) {
	 Boolean bv = (Boolean) ov;
	 v = bv.booleanValue();
       }
    }

   return v;
}


public default Date getSavedDate(Map<String,Object> map,String key,Date v)
{
   Object ov = map.get(key);
   if (ov == null) return v;
   if (ov instanceof Date) return ((Date) ov);

   return v;
}


public default Object getSavedValue(Map<String,Object> map,String key,Object dflt)
{
   Object ov = map.get(key);
   if (ov == null) ov = dflt;
   return ov;
}


@SuppressWarnings("unchecked")
public default Map<String,Object> getSavedJson(Map<String,Object> map,String key,Map<String,Object> dflt)
{
   Object ov = getSavedValue(map,key,null);
   if (ov == null) return dflt;
   if (ov instanceof Map) {
      return ((Map<String,Object>) ov);
    }
   else if (ov instanceof JSONObject) {
      JSONObject jo = (JSONObject) ov;
      return jo.toMap();
    }
   return dflt;
}

@SuppressWarnings("unchecked")
public default Map<String,Object> getSavedObject(Object ov,Map<String,Object> dflt)
{
   if (ov == null) return dflt;
   if (ov instanceof Map) {
      return ((Map<String,Object>) ov);
    }
   else if (ov instanceof JSONObject) {
      JSONObject jo = (JSONObject) ov;
      return jo.toMap();
    }
   return dflt;
}



public default List<?> getSavedList(Map<String,Object> map,String key,List<?> dflt)
{
   Object ov = map.get(key);
   if (ov == null) return dflt;
   if (ov instanceof List) return ((List<?>) ov);
   if (ov instanceof JSONArray) {
      JSONArray ova = (JSONArray) ov;
      return ova.toList();
    }
   if (!(ov instanceof List)) {
      CatreLog.logE("CATRE","Wrong type for saved list " + ov.getClass().getName() + " " + ov);
    }

   return dflt;
}


public default Set<String> getSavedStringSet(CatreStore cs,Map<String,Object> map,String key,Set<String> dflt)
{
   List<?> data = getSavedList(map,key,null);
   if (data == null) return dflt;
   Set<String> rslt = new LinkedHashSet<>();
   for (Object s : data) rslt.add(s.toString());
   return rslt;
}




public default List<String> getSavedStringList(Map<String,Object> map,String key,List<String> dflt)
{
   List<?> data = getSavedList(map,key,null);
   if (data == null) return dflt;
   List<String> rslt = new ArrayList<>();
   for (Object s : data) rslt.add(s.toString());
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Lookup of saved objects 						*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
public default	<T extends CatreSavable> T getSavedObject(CatreStore cs,Map<String,Object> map,String id,T dflt)
{
   String okey = null;
   if (dflt != null) okey = dflt.getDataUID();
   String key = getSavedString(map,id,okey);
   if (key == null) return dflt;

   return (T) cs.loadObject(key);
}

@SuppressWarnings("unchecked")
public default <T extends CatreSavable> List<T> getSavedObjectList(CatreStore cs,
      Map<String,Object> map,String key,List<T> dflt)
      {
   List<String> data = getSavedStringList(map,key,null);
   if (data == null) return dflt;

   List<T> rslt = new ArrayList<>();
   for (String s : data) {
      T v = (T) cs.loadObject(s);
      if (v != null) rslt.add(v);
    }

   return rslt;
}


public default Map<String,Object> getSavedSubobject(CatreSubSavable e)
{
   if (e == null) return null;

   Map<String,Object> rslt = e.toJson();

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Lookup of saved subobjects						*/
/*										*/
/********************************************************************************/

public default <T extends CatreSubSavable> T getSavedSubobject(CatreStore cs,Map<String,Object> map,
								  String id,Creator<T> c,T dflt)
{
   Object obj = map.get(id);
   return createSubObject(cs,obj,c,dflt);
}


public default <T extends CatreSubSavable> T getSavedSubobject(CatreStore cs,Map<String,Object> map,
								  String id,Finder<T> c,T dflt)
{
   String uid = getSavedString(map,id,null);
   if (uid == null) return null;
   return c.find(uid);
}


public default <T extends CatreSubSavable> List<T> getSavedSubobjectList(CatreStore cs,
      Map<String,Object> map,String key,Creator<T> c,List<T> dflt)
{
   List<T> rslt = new ArrayList<>();
   List<?> data = getSavedList(map,key,null);
   if (data == null) return dflt;

   for (Object s : data) {
      T v = (T) createSubObject(cs,s,c,null);
      if (v != null) rslt.add(v);
    }

   return rslt;
}



public default <T extends CatreSubSavable> List<T> getSavedSubobjectList(CatreStore cs,
      Map<String,Object> map,String key,Finder<T> c,List<T> dflt)
{
   List<T> rslt = new ArrayList<>();
   List<String> data = getSavedStringList(map,key,null);

   for (String s : data) {
      T v = c.find(s);
      if (v != null) rslt.add(v);
    }

   return rslt;
}


public default <T extends CatreSubSavable> Set<T> getSavedSubobjectSet(CatreStore cs,
      Map<String,Object> map,String key,Creator<T> c,Set<T> dflt)
{
   Set<T> rslt = new LinkedHashSet<>();
   List<?> data = getSavedList(map,key,null);
   if (data == null) return dflt;

   for (Object s : data) {
      T v = (T) createSubObject(cs,s,c,null);
      if (v != null) rslt.add(v);
    }

   return rslt;
}



public default <T extends CatreSubSavable> Set<T> getSavedSubobjectSet(CatreStore cs,
      Map<String,Object> map,String key,Finder<T> c,Set<T> dflt)
{
   Set<T> rslt = new HashSet<>();
   List<String> data = getSavedStringList(map,key,null);

   for (String s : data) {
      T v = c.find(s);
      if (v != null) rslt.add(v);
    }

   return rslt;
}


/********************************************************************************/
/*										*/
/*	Create a subobject from scratch 					*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
private static <T extends CatreSubSavable> T createSubObject(CatreStore cs,Object data,Creator<T> creator,T dflt)
{
   CatreSubSavable rslt = null;
   if (data == null) return dflt;
   if (data instanceof JSONObject) {
      JSONObject jdata = (JSONObject) data;
      data = jdata.toMap();
    }
   if (!(data instanceof Map)) {
      CatreLog.logX("CATRE","Wrong type for saved subobject " + data.getClass().getName() + " " + data);
      return dflt;
    }

   if (data instanceof Map) {
      Map<String,Object> smap = (Map<String,Object>) data;
      if (creator != null) {
	 return creator.create(cs,smap);
       }
    }
   else rslt = dflt;

   return (T) rslt;
}


/********************************************************************************/
/*										*/
/*	Creator class (Functional)						*/
/*										*/
/********************************************************************************/

interface Creator<T extends CatreSubSavable> {

   T create(CatreStore cs,Map<String,Object> map);

}      // end of subinterface Creator


interface Finder<T extends CatreSubSavable> {

   T find(String uid);

}	// end of subinterface Finder




/********************************************************************************/
/*										*/
/*	Create JSON objects easily						*/
/*										*/
/********************************************************************************/

public default JSONObject buildJson(Object ... val)
{
   JSONObject rslt = new JSONObject();

   if (val.length > 1) {
      for (int i = 0; i+1 < val.length; i += 2) {
	 String key = val[i].toString();
	 Object v = val[i+1];
	 rslt.put(key,v);
       }
    }

   return rslt;
}

public default JSONArray buildJsonArray(Object ... val)
{
   JSONArray rslt = new JSONArray();
   for (Object v : val) {
      rslt.put(v);
    }

   return rslt;
}


}	// end of interface CatreJson




/* end of CatreJson.java */

