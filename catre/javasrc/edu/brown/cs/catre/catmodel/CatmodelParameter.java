/********************************************************************************/
/*                                                                              */
/*              CatmodelParameter.java                                          */
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

import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavable;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.ivy.swing.SwingColorSet;

abstract class CatmodelParameter extends CatreSubSavableBase implements CatreParameter, CatreSubSavable, CatmodelConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String parameter_name;
private String parameter_label;
private String parameter_description;
private boolean is_sensor;
private boolean is_target;
private String default_unit;
private Set<String> all_units;

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

static CatreParameter createParameter(CatreStore cs,Map<String,Object> map) 
{
   String typnm = map.get("TYPE").toString();
   String pnm = map.get("NAME").toString();
   
   CatmodelParameter p = null;
   switch (typnm) {
      case "STRING":
        p =  new StringParameter(pnm);
        break;
      case "BOOLEAN":
        p = new BooleanParameter(pnm);
        break;
      case "INTEGER" :
         p = new IntParameter(pnm);
         break;
      case "REAL" :
         p = new RealParameter(pnm);
         break;
      case "TIME" :
         p = new TimeParameter(pnm);
         break;
      case "DATE" :
         p = new DateParameter(pnm);
         break;
      case "DATETIME" :
         p = new DateTimeParameter(pnm);
         break;
      case "COLOR" :
         p = new ColorParameter(pnm);
         break;
      case "SET" :
         p = new SetParameter(pnm);
         break;
      case "ENUM" :
         p = new EnumParameter(pnm);
         break;
    }
   
   if (p == null) return null;
   
   p.fromJson(cs,map);
   
   return p;
}

static CatmodelParameter createStringParameter(String name)
{
   return new StringParameter(name);
}

static CatmodelParameter createBooleanParameter(String name)
{
   return new BooleanParameter(name);
}


static CatmodelParameter createIntParameter(String name,int from,int to)
{
   return new IntParameter(name,from,to);
}


static CatmodelParameter createIntParameter(String name)
{
   return new IntParameter(name);
}

static CatmodelParameter createRealParameter(String name,double from,double to)
{
   return new RealParameter(name,from,to);
}

static CatmodelParameter createRealParameter(String name) 
{
   return new RealParameter(name);
}


static CatmodelParameter createTimeParameter(String name)
{
   return new TimeParameter(name);
}

static CatmodelParameter createDateParameter(String name)
{
   return new DateParameter(name);
}


static CatmodelParameter createDateTimeParameter(String name)
{
   return new DateTimeParameter(name);
}



public static CatmodelParameter createEnumParameter(String name,Enum<?> e)
{
   return new EnumParameter(name,e);
}


public static CatmodelParameter createEnumParameter(String name,Iterable<String> vals)
{
   return new EnumParameter(name,vals);
}


public static CatmodelParameter createEnumParameter(String name,String [] vals)
{
   return new EnumParameter(name,vals);
}

public static CatmodelParameter createSetParameter(String name,Iterable<String> vals)
{
   return new SetParameter(name,vals);
}






static CatmodelParameter createColorParameter(String name)
{
   return new ColorParameter(name);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatmodelParameter(String name)
{
   super("PARAM_");
   
   parameter_name = name;
   parameter_description = name;
   parameter_label = null;
   is_sensor = false;
   is_target = false;
   all_units = null;
   default_unit = null;
}




/********************************************************************************/
/*										*/
/*	Default Access Methods							*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return parameter_name; }

@Override public String getDescription()	{ return parameter_description; }

public void setDescription(String d)		{ parameter_description = d; }

@Override public String getLabel()
{
   if (parameter_label != null) return parameter_label;
   return getName();
}

public void setLabel(String l)			{ parameter_label = l; }

@Override public boolean isSensor()		{ return is_sensor; }

@Override public void setIsSensor(boolean fg)		{ is_sensor = fg; }

@Override public boolean isTarget()		{ return is_target; }

@Override public void setIsTarget(boolean fg)		{ is_target = fg; }



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
}

void addDefaultUnit(String u)
{
   if (all_units == null) all_units = new LinkedHashSet<>();
   all_units.add(u);
   default_unit = u;
}


/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

@Override public double getMinValue()			{ return 0; }
@Override public double getMaxValue()			{ return 0; }
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
/*      I/O methods             						*/
/*										*/
/********************************************************************************/

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   parameter_name = getSavedString(map,"NAME",parameter_name);
   parameter_label = getSavedString(map,"LABEL",parameter_label);
   parameter_description = getSavedString(map,"DESC",parameter_description);
   is_sensor = getSavedBool(map,"ISSENSOR",is_sensor);
   is_target = getSavedBool(map,"ISTARGET",is_target);
}




@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE",getParameterType());
   rslt.put("NAME",getName());
   rslt.put("LABEL",getLabel());
   rslt.put("DESC",getDescription());
   rslt.put("ISSENSOR",isSensor());
   rslt.put("ISTARGET",isTarget());

   List<Object> vals = getValues();
   if (vals != null) {
      List<String> strs = new ArrayList<>();
      for (Object o : vals) {
         strs.add(o.toString());
       }
      rslt.put("VALUES",strs);
    }
   
   return rslt;
}




@Override public String toString()
{
   return getName();
}


/********************************************************************************/
/*										*/
/*	String parameters							*/
/*										*/
/********************************************************************************/

private static class StringParameter extends CatmodelParameter {
   
   StringParameter(String name) {
      super(name);
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
   
   BooleanParameter(String name) {
      super(name);
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

private static class IntParameter extends CatmodelParameter {
   
   private int min_value;
   private int max_value;
   
   IntParameter(String name) {
      super(name);
      min_value = Integer.MIN_VALUE;
      max_value = Integer.MAX_VALUE;
    }
   
   IntParameter(String name,int min,int max) {
      super(name);
      min_value = min;
      max_value = max;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      min_value = getSavedInt(map,"MIN",min_value);
      max_value = getSavedInt(map,"MAX",max_value);
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("MIN",min_value);
      rslt.put("MAX",max_value); 
      return rslt;
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.INTEGER;
    }
   
   @Override public double getMinValue()		{ return min_value; }
   @Override public double getMaxValue()		{ return max_value; }
   
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
      if (ivl < min_value) ivl = min_value;
      if (ivl > max_value) ivl = max_value;
      return Integer.valueOf(ivl);
    }
   
}	// end of inner class IntParameter



/********************************************************************************/
/*										*/
/*	Real Parameter								*/
/*										*/
/********************************************************************************/

private static class RealParameter extends CatmodelParameter {
   
   private double min_value;
   private double max_value;
   
   RealParameter(String name,double min,double max) {
      super(name);
      min_value = min;
      max_value = max;
    }
   
   RealParameter(String name) {
      super(name);
      min_value = Double.NEGATIVE_INFINITY;
      max_value = Double.POSITIVE_INFINITY;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      min_value = getSavedDouble(map,"MIN",min_value);
      max_value = getSavedDouble(map,"MAX",max_value);
    }
   
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("MIN",min_value);
      rslt.put("MAX",max_value);
      return rslt;
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.REAL;
    }
   
   @Override public double getMinValue()		{ return min_value; }
   @Override public double getMaxValue()		{ return max_value; }
   
   @Override public Object normalize(Object value) {
      if (value == null) return null;
      double ivl = 0;
      if (value instanceof Number) {
         Number n = (Number) value;
         ivl = n.doubleValue();
       }
      else {
         String s = value.toString();
         try {
            ivl = Double.parseDouble(s);
          }
         catch (NumberFormatException e) { }
       }
      if (ivl < min_value) ivl = min_value;
      if (ivl > max_value) ivl = max_value;
      return Double.valueOf(ivl);
    }
   
}	// end of inner class RealParameter




/********************************************************************************/
/*										*/
/*	Time-based parameters							*/
/*										*/
/********************************************************************************/

private static abstract class CalendarParameter extends CatmodelParameter {
   
   CalendarParameter(String name) {
      super(name);
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
   
   TimeParameter(String name) {
      super(name);
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
   
}       // end of inner class TimeParameter


private static class DateParameter extends CalendarParameter {
   
   DateParameter(String name) {
      super(name);
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
   
}       // end of inner class DateParameter


private static class DateTimeParameter extends CalendarParameter {

   DateTimeParameter(String name) {
      super(name);
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.DATETIME;
    }
   
   @Override public Object normalize(Object o) {
      Calendar c = createCalendar(o);
      return c;
    }

}       // end of inner class DateTimeParameter








/********************************************************************************/
/*                                                                              */
/*      Color parameter                                                         */
/*                                                                              */
/********************************************************************************/

private static class ColorParameter extends CatmodelParameter {

   ColorParameter(String name) {
      super(name);
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
   
}       // end of inner class ColorParameter



/********************************************************************************/
/*										*/
/*	SetParameter -- set of strings						*/
/*										*/
/********************************************************************************/

private static class SetParameter extends CatmodelParameter {
   
   private Set<String> value_set;
   
   SetParameter(String nm) {
      super(nm);
      value_set = new LinkedHashSet<>();
    }
   
   SetParameter(String nm,Enum<?> e) {
      super(nm);
      value_set = new LinkedHashSet<>();
      for (Enum<?> x : e.getClass().getEnumConstants()) {
         value_set.add(x.toString().intern());
       }
    }
   
   SetParameter(String nm,Iterable<String> vals) {
      super(nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }
   
   
   SetParameter(String nm,String [] vals) {
      super(nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      value_set = getSavedStringSet(cs,map,"VALUES",value_set);
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.SET;
    }
   
   @Override public List<Object> getValues() {
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
/*	SetParameter -- set of strings						*/
/*										*/
/********************************************************************************/

private static class EnumParameter extends CatmodelParameter {
   
   private Set<String> value_set;
   
   EnumParameter(String nm) {
      super(nm);
      value_set = new LinkedHashSet<>();
    }
   
   EnumParameter(String nm,Enum<?> e) {
      super(nm);
      value_set = new LinkedHashSet<>();
      for (Enum<?> x : e.getClass().getEnumConstants()) {
         value_set.add(x.toString().intern());
       }
    }
   
   EnumParameter(String nm,Iterable<String> vals) {
      super(nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }
   
   
   EnumParameter(String nm,String [] vals) {
      super(nm);
      value_set = new LinkedHashSet<>();
      for (String s : vals) value_set.add(s.intern());
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      value_set = getSavedStringSet(cs,map,"VALUES",value_set);
    }
   
   @Override public ParameterType getParameterType() {
      return ParameterType.ENUM;
    }
   
   @Override public List<Object> getValues() {
      return new ArrayList<Object>(value_set);
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
   
}	// end of inner class SetParameter





}       // end of class CatmodelParameter




/* end of CatmodelParameter.java */

