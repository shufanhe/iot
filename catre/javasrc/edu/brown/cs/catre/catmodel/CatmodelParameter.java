/********************************************************************************/
/*										*/
/*		CatmodelParameter.java						*/
/*										*/
/*	description of class							*/
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




package edu.brown.cs.catre.catmodel;

import java.awt.Color;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.ivy.swing.SwingColorSet;

abstract class CatmodelParameter extends CatreDescribableBase implements CatreParameter, 
      CatreSubSavable, CatmodelConstants 
{      
      


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected CatreUniverse for_universe;
private boolean is_sensor;
protected String default_unit;
private Set<String> all_units;
protected CatreParameterRef range_ref;
private String parameter_data;

private static final DateFormat [] formats = new DateFormat [] {
   DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG),
   DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT),
   DateFormat.getDateInstance(DateFormat.LONG),
   DateFormat.getDateInstance(DateFormat.SHORT),
   DateFormat.getTimeInstance(DateFormat.LONG),
   DateFormat.getTimeInstance(DateFormat.SHORT),
   new SimpleDateFormat("MM/dd/yyyy hh:mma"),
   new SimpleDateFormat("MM/dd/yyyy HH:mm"),
   new SimpleDateFormat("MM/dd/yyyy"),
   new SimpleDateFormat("h:mma"),
   new SimpleDateFormat("H:mm"),
   new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
};




/********************************************************************************/
/*										*/
/*	Creation Methods							*/
/*										*/
/********************************************************************************/

static CatreParameter createParameter(CatreUniverse cu,CatreStore cs,Map<String,Object> map)
{
   String typnm = map.get("TYPE").toString();
   String pnm = map.get("NAME").toString();

   CatmodelParameter p = null;
   switch (typnm) {
      case "STRING":
	p =  new StringParameter(cu,pnm);
	break;
      case "BOOLEAN":
	p = new BooleanParameter(cu,pnm);
	break;
      case "INTEGER" :
	 p = new IntParameter(cu,pnm);
	 break;
      case "REAL" :
	 p = new RealParameter(cu,pnm);
	 break;
      case "TIME" :
	 p = new TimeParameter(cu,pnm);
	 break;
      case "DATE" :
	 p = new DateParameter(cu,pnm);
	 break;
      case "DATETIME" :
	 p = new DateTimeParameter(cu,pnm);
	 break;
      case "COLOR" :
	 p = new ColorParameter(cu,pnm);
	 break;
      case "SET" :
	 p = new SetParameter(cu,pnm);
	 break;
      case "ENUM" :
	 p = new EnumParameter(cu,pnm);
	 break;
      case "EVENTS" :
	 p = new EventsParameter(cu,pnm);
	 break;
      case "STRINGLIST" :
	 p = new StringListParameter(cu,pnm);
	 break;
      case "OBJECT" :
         p = new ObjectParameter(cu,pnm);
         break;
    }

   if (p == null) return null;

   p.fromJson(cs,map);

   return p;
}

static CatmodelParameter createStringParameter(CatreUniverse cu,String name)
{
   return new StringParameter(cu,name);
}

static CatmodelParameter createBooleanParameter(CatreUniverse cu,String name)
{
   return new BooleanParameter(cu,name);
}


static CatmodelParameter createIntParameter(CatreUniverse cu,String name,int from,int to)
{
   return new IntParameter(cu,name,from,to);
}


static CatmodelParameter createIntParameter(CatreUniverse cu,String name)
{
   return new IntParameter(cu,name);
}

static CatmodelParameter createRealParameter(CatreUniverse cu,String name,double from,double to)
{
   return new RealParameter(cu,name,from,to);
}

static CatmodelParameter createRealParameter(CatreUniverse cu,String name)
{
   return new RealParameter(cu,name);
}


public static CatmodelParameter createEnumParameter(CatreUniverse cu,String name,Enum<?> e)
{
   return new EnumParameter(cu,name,e);
}


public static CatmodelParameter createEnumParameter(CatreUniverse cu,String name,Iterable<String> vals)
{
   return new EnumParameter(cu,name,vals);
}


public static CatmodelParameter createEnumParameter(CatreUniverse cu,String name,String [] vals)
{
   return new EnumParameter(cu,name,vals);
}

public static CatmodelParameter createSetParameter(CatreUniverse cu,String name,Iterable<String> vals)
{
   return new SetParameter(cu,name,vals);
}


static CatmodelParameter createColorParameter(CatreUniverse cu,String name)
{
   return new ColorParameter(cu,name);
}


static CatmodelParameter createEventsParameter(CatreUniverse cu,String name)
{
   return new EventsParameter(cu,name);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatmodelParameter(CatreUniverse cu,String name)
{
   super(null);

   for_universe = cu;
   
   setName(name);

   is_sensor = false;
   all_units = null;
   default_unit = null;
   range_ref = null;
   parameter_data = null;
}



/********************************************************************************/
/*										*/
/*	Default Access Methods							*/
/*										*/
/********************************************************************************/

@Override public boolean isSensor()		{ return is_sensor; }

@Override public void setIsSensor(boolean fg)		{ is_sensor = fg; }


@Override public String getDefaultUnits()	
{
   return default_unit;
}

@Override public Collection<String> getAllUnits()
{
   return all_units;
}

void addUnits(Collection<String> u)
{
   if (all_units == null) all_units = new LinkedHashSet<>();
   all_units.addAll(u);
   if (default_unit == null && all_units.size() > 0) {
      for (String s : all_units) {
	 default_unit = s;
	 break;
       }
    }
}

void addDefaultUnit(String u)
{
   if (all_units == null) all_units = new LinkedHashSet<>();
   all_units.add(u);
   default_unit = u;
}


@Override public String getParameterData() 
{
   return parameter_data; 
}


@Override public CatreParameterRef getActiveSensor() 
{
   return range_ref;
}


/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

@Override public Double getMinValue()		 	{ return null; }
@Override public Double getMaxValue()			{ return null; }
@Override public List<Object> getValues()		{ return null; }

@Override public Object normalize(Object v)		{ return v; }

@Override public String unnormalize(Object v)
{
   v = normalize(v);
   if (v == null) return null;

   return externalString(v);
}


protected String externalString(Object v)
{
   return v.toString();
}



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   is_sensor = getSavedBool(map,"ISSENSOR",is_sensor);
   all_units = getSavedStringSet(cs,map,"UNITS",null);
   default_unit = getSavedString(map,"DEFAULT_UNIT",null);
   parameter_data = getSavedString(map,"DATA",null);
   if (all_units != null && all_units.size() > 0 && default_unit == null) {
      for (String s : all_units) {
	 default_unit = s;
	 break;
       }
    }
   range_ref = getSavedSubobject(cs,map,"RANGEREF",
         this::createRangeParamRef,range_ref);
}




@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   checkRange();

   rslt.put("TYPE",getParameterType());
   rslt.put("ISSENSOR",isSensor());
   if (parameter_data != null) rslt.put("DATA",parameter_data);

   List<Object> vals = getValues();
   if (vals != null) {
      List<String> strs = new ArrayList<>();
      for (Object o : vals) {
	 strs.add(o.toString());
       }
      rslt.put("VALUES",strs);
    }

   if (all_units != null) {
      rslt.put("UNITS",all_units);
      rslt.put("DEFAULT_UNIT",default_unit);
    }
   
   if (range_ref != null) {
      rslt.put("RANGEREF",range_ref.toJson());
    }

   return rslt;
}


@Override public boolean update(CatreParameter cp0) 
{
   CatmodelParameter cp = (CatmodelParameter) cp0;
   // NAME and TYPE must be the same
   
   boolean chng = super.update(cp); 
   if (cp.is_sensor != is_sensor) {
      chng = true;
      is_sensor = cp.is_sensor;
    }
   all_units = cp.all_units;
   default_unit = cp.default_unit;
   range_ref = cp.range_ref;
   
   return chng;
}



@Override public String toString()
{
   return getName();
}



/********************************************************************************/
/*                                                                              */
/*      Handle ranges                                                           */
/*                                                                              */
/********************************************************************************/

private CatreParameterRef createRangeParamRef(CatreStore cs,Map<String,Object> map) 
{
   CatreLog.logD("Create range parameter reference " + map);
   
   return for_universe.createParameterRef(new RangeAvailable(),cs,map);
}





protected void setRangeValues(Object vals)
{
   if (vals == null) return;
   
   CatreLog.logD("CATMODEL","HANDLE SET RANGE VALUES " + this + " : " + vals);
}


protected void checkRange()
{
   if (range_ref != null) range_ref.getDevice();
}


private class RangeAvailable implements CatreReferenceListener, CatreDeviceListener {

   RangeAvailable() { }
   
   @Override public void referenceValid(boolean fg) {
      if (fg) {
         CatreDevice cd = range_ref.getDevice();
         CatreParameter cp = range_ref.getParameter();
         cp.setIsSensor(false);
         Object vals = cd.getParameterValue(cp);
         setRangeValues(vals);
         cd.addDeviceListener(this);
       }
    }
   
   @Override public void stateChanged(CatreParameter p) {
      if (p == range_ref) {
         CatreDevice cd = range_ref.getDevice();
         CatreParameter cp = range_ref.getParameter();
         Object vals = cd.getParameterValue(cp);
         setRangeValues(vals);
       }
    }

}       // end of inner class RangeAvailable



/********************************************************************************/
/*										*/
/*	String parameters							*/
/*										*/
/********************************************************************************/

private static class StringParameter extends CatmodelParameter {

   StringParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public Object normalize(Object o) {
      if (o == null) return null;
      return o.toString();
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.STRING;
    }

}	// end of inner class StringParameter




/********************************************************************************/
/*										*/
/*	Boolean parameters							*/
/*										*/
/********************************************************************************/

private static class BooleanParameter extends CatmodelParameter {

   BooleanParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public Object normalize(Object o) {
      if (o != null && o instanceof Boolean) return o;
      boolean bvl = false;
      if (o == null) ;
      else if (o instanceof Number) {
	 Number n = (Number) o;
	 if (n.doubleValue() != 0) bvl = true;
       }
      else if (o instanceof String) {
	 String s = (String) o;
	 if (s.trim().equals("")) bvl = false;
	 if (s.startsWith("t") || s.startsWith("T") || s.startsWith("1") ||
	       s.startsWith("y") || s.startsWith("Y"))
	    bvl = true;
       }
      else bvl = true;
      return Boolean.valueOf(bvl);
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.BOOLEAN;
    }

   @Override public List<Object> getValues() {
      List<Object> rslt = new ArrayList<Object>();
      rslt.add(Boolean.FALSE);
      rslt.add(Boolean.TRUE);
      return rslt;
    }

}	// end of inner class BooleanParameter



/********************************************************************************/
/*										*/
/*	Numeric Parameters							*/
/*										*/
/********************************************************************************/

private abstract static class NumberParameter extends CatmodelParameter {
   
   protected Number min_value;
   protected Number max_value;
   
   NumberParameter(CatreUniverse cu,String name) {
      this(cu,name,null,null);
    }
   
   NumberParameter(CatreUniverse cu,String name,Number min,Number max) {
      super(cu,name);
      min_value = min;
      max_value = max;
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      // ensure referenced ranges are included 
      Number min = getMinValue();
      if (min != null) rslt.put("MIN",min);
      Number max = getMaxValue();
      if (max != null) rslt.put("MAX",max);
      return rslt;
    }
   
   @Override public boolean update(CatreParameter cp) {
      boolean chng = super.update(cp);
      NumberParameter np = (NumberParameter) cp;
      min_value = np.min_value;
      max_value = np.max_value;
      return chng;
    }
   
   @Override public Double getMinValue() { 
      if (range_ref != null) {
         CatreParameter cp = range_ref.getParameter();
         if (cp != null) {
            Object v = for_universe.getValue(cp);
            JSONObject jo = (JSONObject) cp.normalize(v);
            if (jo != null) min_value = jo.optNumber("minimum",min_value);
          }
       }
      return min_value == null ? null : min_value.doubleValue();
    }
   
   @Override public Double getMaxValue() {
      if (range_ref != null) {
         CatreParameter cp = range_ref.getParameter();
         if (cp != null) {
            Object v = for_universe.getValue(cp);
            JSONObject jo = (JSONObject) cp.normalize(v);
            if (jo != null) max_value = jo.optNumber("maximum",min_value);
          }
       }
      return max_value == null ? null : max_value.doubleValue();
    }
   
   @Override protected void setRangeValues(Object vals) {
      super.setRangeValues(vals);
      if (vals != null && range_ref != null) {
         CatreParameter cp = range_ref.getParameter();
         if (cp != null) {
            JSONObject jo = (JSONObject) cp.normalize(vals);
            min_value = jo.optNumber("minimum",min_value);
            max_value = jo.optNumber("maximum",max_value);
            default_unit = jo.optString("unit",default_unit);
          }
       }
    }
   
}       // end of inner abstract class NumberParameter

private static class IntParameter extends NumberParameter {

   IntParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   IntParameter(CatreUniverse cu,String name,Integer min,Integer max) {
      super(cu,name,min,max);
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      try {
         min_value = getOptSavedInt(map,"MIN",min_value);
       }
      catch (NumberFormatException e) { }
      if (min_value != null && min_value.intValue() == Integer.MIN_VALUE) {
         min_value = null;
       }
      try {
         max_value = getOptSavedInt(map,"MAX",max_value);
       }
      catch (NumberFormatException e) { }
      if (max_value != null && max_value.intValue() == Integer.MAX_VALUE) {
         max_value = null;
       }
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.INTEGER;
    }
   
   @Override public Object normalize(Object value) {
      if (value == null) return null;
      int ivl = 0;
      if (value instanceof Number) {
	 Number n = (Number) value;
	 ivl = n.intValue();
       }
      else {
	 String s = value.toString();
	 try {
	    ivl = Integer.parseInt(s);
	  }
	 catch (NumberFormatException e) { }
       }
      if (min_value != null && ivl < min_value.intValue()) ivl = min_value.intValue();
      if (max_value != null && ivl > max_value.intValue()) ivl = max_value.intValue();
      return Integer.valueOf(ivl);
    }

}	// end of inner class IntParameter



/********************************************************************************/
/*										*/
/*	Real Parameter								*/
/*										*/
/********************************************************************************/

private static class RealParameter extends NumberParameter {

   RealParameter(CatreUniverse cu,String name,double min,double max) {
      super(cu,name,min,max);
    }

   RealParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      try {
         min_value = getOptSavedDouble(map,"MIN",min_value);
       }
      catch (NumberFormatException e) { }
      try {
         max_value = getOptSavedDouble(map,"MAX",max_value);
       }
      catch (NumberFormatException e) { }
   }
 
   @Override public ParameterType getParameterType() {
      return ParameterType.REAL;
    }

   @Override public Object normalize(Object value) {
      if (value == null) return null;
      double dvl = 0;
      if (value instanceof Number) {
	 Number n = (Number) value;
	 dvl = n.doubleValue();
       }
      else {
	 String s = value.toString();
	 try {
	    dvl = Double.parseDouble(s);
	  }
	 catch (NumberFormatException e) { }
       }
      if (min_value != null && dvl < min_value.doubleValue()) {
         dvl = min_value.doubleValue();
       }
      if (max_value != null && dvl > max_value.doubleValue()) {
         dvl = max_value.doubleValue();
       }
      return Double.valueOf(dvl);
    }

}	// end of inner class RealParameter




/********************************************************************************/
/*										*/
/*	Time-based parameters							*/
/*										*/
/********************************************************************************/

private static abstract class CalendarParameter extends CatmodelParameter {

   CalendarParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override protected String externalString(Object o) {
      if (o == null) return null;
      if (!(o instanceof Calendar)) o = normalize(o);
      Calendar c = (Calendar) o;
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
      Date d = c.getTime();
      String rslt = sdf.format(d);
      return rslt;
    }

   protected Calendar createCalendar(Object o) {
      if (o == null) return null;
      if (o instanceof Calendar) return ((Calendar) o);
      Date d = createDate(o);
      if (d == null) return null;
      Calendar c = Calendar.getInstance();
      c.setTime(d);
      return c;
    }

   protected Date createDate(Object o) {
      if (o == null) return null;
      if (o instanceof Date) return ((Date) o);
      if (o instanceof Number) {
	 Number n = (Number) o;
	 long tm = n.longValue();
	 return new Date(tm);
       }
      String svl = o.toString();
      if (svl.equals("*") || svl.equals("NOW")) return new Date();
      for (DateFormat df : formats) {
	 try {
	    Date d = df.parse(svl);
	    return d;
	  }
	 catch (ParseException e) { }
       }
      return null;
    }

}	// end of inner class TimeParameter



private static class TimeParameter extends CalendarParameter {

   TimeParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.TIME;
    }

   @Override public Object normalize(Object o) {
      Calendar c = createCalendar(o);
      if (c == null) return null;
      c.set(Calendar.HOUR_OF_DAY,0);
      c.set(Calendar.MINUTE,0);
      c.set(Calendar.SECOND,0);
      c.set(Calendar.MILLISECOND,0);
      return c;
    }

}	// end of inner class TimeParameter


private static class DateParameter extends CalendarParameter {

   DateParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.DATE;
    }

   @Override public Object normalize(Object o) {
      Calendar c = createCalendar(o);
      if (c == null) return null;
      c.set(0,0,0);
      return c;
    }

}	// end of inner class DateParameter


private static class DateTimeParameter extends CalendarParameter {

   DateTimeParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.DATETIME;
    }

   @Override public Object normalize(Object o) {
      Calendar c = createCalendar(o);
      return c;
    }

}	// end of inner class DateTimeParameter




/********************************************************************************/
/*										*/
/*	Color parameter 							*/
/*										*/
/********************************************************************************/

private static class ColorParameter extends CatmodelParameter {

   ColorParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.COLOR;
    }

   @Override public Object normalize(Object value) {
      if (value == null) return null;
      if (value instanceof java.awt.Color) {
	 return value;
       }
      else if (value instanceof Number) {
	 Number n = (Number) value;
	 int ivl = n.intValue();
	 return new Color(ivl);
       }
      else {
	 String s = value.toString();
	 return SwingColorSet.getColorByName(s);
       }
    }

   @Override protected String externalString(Object o) {
      if (o == null) return "#000000";
      if (!(o instanceof Color)) o = normalize(o);
      Color c = (Color) o;
      String rslt = SwingColorSet.getColorName(c);
      return rslt;
    }

}	// end of inner class ColorParameter



/********************************************************************************/
/*										*/
/*	SetParameter -- set of strings						*/
/*										*/
/********************************************************************************/

private static class SetParameter extends CatmodelParameter {

   private Set<String> value_set;

   SetParameter(CatreUniverse cu,String nm) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
    }

   SetParameter(CatreUniverse cu,String nm,Enum<?> e) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
      for (Enum<?> x : e.getClass().getEnumConstants()) {
	 value_set.add(x.toString().intern());
       }
    }

   SetParameter(CatreUniverse cu,String nm,Iterable<String> vals) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }


   SetParameter(CatreUniverse cu,String nm,String [] vals) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      value_set = getSavedStringSet(cs,map,"VALUES",value_set);
    }
   
   @Override public boolean update(CatreParameter cp) {
      boolean chng = super.update(cp);
      SetParameter sp = (SetParameter) cp;
      value_set = sp.value_set;
      return chng;
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.SET;
    }

   @Override public List<Object> getValues() {
      checkRange();
      return new ArrayList<Object>(value_set);
    }

   @Override public Object normalize(Object o) {
      if (o == null) return null;
      if (o instanceof Set<?>) return o;
      Set<String> rslt = new HashSet<>();
      String s = o.toString();
      StringTokenizer tok = new StringTokenizer(s,",;");
      while (tok.hasMoreTokens()) {
         String v1 = tok.nextToken().trim();
         for (String v : value_set) {
            if (v.equalsIgnoreCase(v1)) rslt.add(v1);
          }
       }
      return rslt;
    }

   @Override protected String externalString(Object o) {
      if (o == null) return null;
      if (!(o instanceof Set<?>)) o = normalize(o);
      Set<?> itms = (Set<?>) o;
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (Object v : itms) {
	 if (ct++ > 0) buf.append(";");
	 buf.append(v.toString());
       }
      return buf.toString();
    }

}	// end of inner class SetParameter



/********************************************************************************/
/*										*/
/*	EnumParameter -- one of a set of string 				*/
/*										*/
/********************************************************************************/

private static class EnumParameter extends CatmodelParameter {

   private Set<String> value_set;

   EnumParameter(CatreUniverse cu,String nm) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
    }

   EnumParameter(CatreUniverse cu,String nm,Enum<?> e) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
      for (Enum<?> x : e.getClass().getEnumConstants()) {
	 value_set.add(x.toString().intern());
       }
    }

   EnumParameter(CatreUniverse cu,String nm,Iterable<String> vals) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }

   EnumParameter(CatreUniverse cu,String nm,String [] vals) {
      super(cu,nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      value_set = getSavedStringSet(cs,map,"VALUES",value_set);
    }
   
   @Override public boolean update(CatreParameter cp) {
      boolean chng = super.update(cp);
      EnumParameter sp = (EnumParameter) cp;
      value_set = sp.value_set;
      return chng;
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.ENUM;
    }

   @Override public List<Object> getValues() {
      if (range_ref != null) {
         checkRange();
         CatreParameter cp = range_ref.getParameter();
         if (cp != null) {
            Object v = for_universe.getValue(range_ref.getParameter());
            if (value_set.isEmpty()) {
               CatreLog.logD("CATBRIDGE","Value load if needed here");
               setRangeValues(v);   
             }
          }
       }
      return new ArrayList<Object>(value_set);
    }
   
   @Override protected void setRangeValues(Object vals) {
      super.setRangeValues(vals);
      if (vals != null && range_ref != null) {
         CatreParameter cp = range_ref.getParameter();
         if (cp != null) {
            Object vals1 = cp.normalize(vals);
            if (vals1 == null) return;
            if (vals1 instanceof Collection) {
               value_set = new LinkedHashSet<>();
               for (Object o : (Collection<?>) vals1) {
                  value_set.add(o.toString());
                }
             }
            else {
               CatreLog.logE("Problem with enum range values from " +
                     vals1.getClass() + " " + vals1);
             }
          }
       }
    }
   
   @Override public Object normalize(Object o) {
      if (o == null) return null;
      String s = o.toString();
      for (String v : value_set) {
	 if (v.equals(s)) return v;
       }
      for (String v : value_set) {
	 if (v.equalsIgnoreCase(s)) return v;
       }
      return null;
    }

}	// end of inner class EnumParameter



/********************************************************************************/
/*                                                                              */
/*      Object parameter                                                        */
/*                                                                              */
/********************************************************************************/

private static class ObjectParameter extends CatmodelParameter {
   
   ObjectParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }
   
   @Override public Object normalize(Object o) {
      if (o == null) return null;
      if (o instanceof JSONObject) return o;
      else if (o instanceof String) {
         return new JSONObject((String) o);
       }
      else if (o instanceof Map) {
         Map<?,?> map = (Map<?,?>) o;
         return new JSONObject(map);
       }
      return new JSONObject();
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.OBJECT; 
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      // add field information
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      // get field information
    }
   
}       // end of inner class Object Parameter



/********************************************************************************/
/*										*/
/*	Events parameters					        	*/
/*										*/
/********************************************************************************/

private static class EventsParameter extends CatmodelParameter {

   EventsParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public Object normalize(Object o) {
      if (o == null) return null;
      if (o instanceof List<?>) return o;
      return new ArrayList<>();
    }

   @Override protected String externalString(Object o) {
      return "[]";
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.EVENTS;
    }
   
   
}	// end of inner class EventsParameter



/********************************************************************************/
/*										*/
/*	String List parameter							*//*										  */
/********************************************************************************/

private static class StringListParameter extends CatmodelParameter {

   StringListParameter(CatreUniverse cu,String name) {
      super(cu,name);
    }

   @Override public ParameterType getParameterType() {
      return ParameterType.STRINGLIST;
    }

   @Override public Object normalize(Object o) {
      if (o == null) return null;
      List<String> rslt = new ArrayList<>();
   
      if (o instanceof List) {
         return o;
       }
      else if (o instanceof Collection) {
         Collection<?> c = (Collection<?>) o;
         for (Object s : c) {
            rslt.add(s.toString());
          }
       }
      else if (o instanceof JSONArray) {
         JSONArray ja = (JSONArray) o;
         for (Object v : ja) {
            rslt.add(v.toString());
          }
       }
      else {
         String s = o.toString();
         StringTokenizer tok = new StringTokenizer(s,",;[]");
         while (tok.hasMoreTokens()) {
            String v1 = tok.nextToken().trim();
            rslt.add(v1);
          }
       }
      return rslt;
    }

   @Override protected String externalString(Object o) {
      if (o == null) return null;
      if (!(o instanceof List<?>)) o = normalize(o);
      List<?> itms = (List<?>) o;
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (Object v : itms) {
         if (ct++ > 0) buf.append(";");
         buf.append(v.toString());
       }
      return buf.toString();
    }

}	// end of inner class StringListParameter



}	// end of class CatmodelParameter




/* end of CatmodelParameter.java */

