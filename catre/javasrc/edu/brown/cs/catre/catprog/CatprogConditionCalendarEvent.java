/********************************************************************************/
/*                                                                              */
/*              CatprogConditionCalendarEvent.java                              */
/*                                                                              */
/*      Condition based on user (google) calendar access                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2022 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2022, Brown University, Providence, RI.                            *
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



package edu.brown.cs.catre.catprog;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.brown.cs.catre.catre.CatreCalendarEvent;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreWorld;



class CatprogConditionCalendarEvent extends CatprogCondition
       implements CatreDeviceListener, CatreReferenceListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<FieldMatch> field_matches;
private CatreParameterRef param_ref;
private Boolean is_on;
private CatreDevice last_device;


enum NullType { EITHER, NULL, NONNULL };
enum MatchType { IGNORE, MATCH, NOMATCH };

 
/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionCalendarEvent(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   is_on = null;
   
   param_ref.initialize();
   
   setValid(param_ref.isValid());
}


private static String getUniqueName(String name,String [] fldvals)
{
   StringBuffer buf = new StringBuffer();
   buf.append(name);
   for (int i = 0; i+1 < fldvals.length; i += 2) {
      buf.append("_");
      buf.append(fldvals[i]);
      buf.append(":");
      buf.append(fldvals[i+1]);
    }
   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)
{
   if (param_ref.isValid()) rslt.add(param_ref.getDevice());
}


@Override public void stateChanged(CatreWorld w)
{
   if (!param_ref.isValid()) return;
   
   if (!param_ref.getDevice().isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE && !isTrigger()) fireOff(w);
      is_on = null;
    }
   Object cvl = param_ref.getDevice().getValueInWorld(param_ref.getParameter(),w);
   boolean rslt = false;
   CatrePropertySet pset = getUniverse().createPropertySet();
   
   if (cvl instanceof Collection<?>) {
      for (Object o : (Collection<?>) cvl) {
         CatreCalendarEvent cce = (CatreCalendarEvent) o;
         boolean mtch = true;
         for (FieldMatch fm : field_matches) {
            if (!fm.match(cce)) {
               mtch = false;
               break;
             }
          }
         if (mtch) {
            getFields(cce,pset);
            rslt = true;
            break;
          }
       }
    }
   if (is_on != null && rslt == is_on && w.isCurrent()) return;
   is_on = rslt;
   
   CatreLog.logI("CATPROG","CONDITION: " + getName() + " " + is_on);
   if (rslt) {
       fireOn(w,pset);
    }
   else {
      fireOff(w);
    }
}



@Override public void referenceValid(boolean fg)
{
   if (fg == isValid()) return;
   
   setValid(fg);
   
   fireValidated();
}


@Override protected void localStartCondition()
{
   last_device = param_ref.getDevice();
   last_device.addDeviceListener(this);
}


@Override protected void localStopCondition() 
{
   if (last_device != null) last_device.removeDeviceListener(this);
   last_device = null;
}


private void getFields(CatreCalendarEvent cce,CatrePropertySet fields)
{
   Map<String,String> props = cce.getProperties();
   String v = props.get("WHERE");
   if (v != null && v.length() > 0) fields.put("WHERE",v);
   DateFormat df = new SimpleDateFormat("h:mmaa");
   if (cce.getStartTime() > 0) {
      fields.put("START",df.format(cce.getStartTime()));
    }
   if (cce.getEndTime() > 0) {
      // if end time is on another day, we should set things differently
      fields.put("END",df.format(cce.getEndTime()));
    }
   
   String w = props.get("TITLE");
   if (w != null) fields.put("CONTENT",w);
   
   if (props.get("ALLDAY") != null) fields.put("ALLDAY","TRUE");
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   if (w != null && !w.isCurrent()) {
      // TODO: handle non-current world calendar events
    }
}




/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","CalendarEvent");
   rslt.put("PARAMREF",param_ref.toJson());
   rslt.put("FIELDS",getSubObjectArrayToSave(field_matches));
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   param_ref = getSavedSubobject(cs,map,"PARAMREF",this::createParamRef,param_ref);
   field_matches = getSavedSubobjectList(cs,map,"FIELDS",FieldMatch::new,
         field_matches);
   
   setUID(getUniqueName(getName(),field_matches.toArray(new String [0])));
   
   if (param_ref == null) {
      String did = "GCAL_" + getUniverse().getDataUID();
      param_ref = getUniverse().createParameterRef(this,did,"EVENTS");
    }
}



private CatreParameterRef createParamRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}



/********************************************************************************/
/*										*/
/*	Field Match information 						*/
/*										*/
/********************************************************************************/

private static class FieldMatch extends CatreSubSavableBase {
   
   private String   field_name;
   private NullType null_type;
   private MatchType match_type;
   private List<Pattern> match_values;
   
   FieldMatch(String name,NullType ntyp,MatchType mtyp,String txt) {
      super(null);
      field_name = name;
      null_type = ntyp;
      match_type = mtyp;
      match_values = new ArrayList<>();
      StringTokenizer tok = new StringTokenizer(txt,", ");
      while (tok.hasMoreTokens()) {
         Pattern p = Pattern.compile(tok.nextToken(),Pattern.CASE_INSENSITIVE);
         match_values.add(p);
       }
    }
   
   FieldMatch(CatreStore cs,Map<String,Object> map) {
      super(cs,map);
    }
   
   boolean match(CatreCalendarEvent evt) {
      String fval = evt.getProperties().get(field_name);
      switch (null_type) {
         case EITHER : 
            break;
         case NULL :
            if (fval == null || fval.length() == 0) return true;
            else return false;
         case NONNULL :
            if (fval == null || fval.length() == 0) return false;
            else return true;
       }
      if (!match_values.isEmpty()) {
         if (fval == null) fval = ""; 
         boolean match = false;
         for (Pattern pmv : match_values) {
            Matcher m = pmv.matcher(fval);
            match |= m.find();
          }
         if (match_type == MatchType.NOMATCH) match = !match;
         return match;
       }
      return true;
    }
   
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("NAME",field_name.trim()); 
      rslt.put("NULL",null_type);
      rslt.put("MATCH",match_type);
      StringBuffer buf = new StringBuffer();
      for (Pattern p : match_values) {
         if (buf.length() > 0) buf.append(",");
         buf.append(p.pattern());
       }
      if (buf.length() > 0) rslt.put("MATCHVALUE",buf.toString());
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      field_name = getSavedString(map,"NAME","").trim();
      null_type = getSavedEnum(map,"NULL",NullType.EITHER);
      match_type = getSavedEnum(map,"MATCH",MatchType.IGNORE);
      match_values = new ArrayList<>();
      String txt = getSavedString(map,"MATCHVALUE",null);
      if (txt != null) {
         StringTokenizer tok = new StringTokenizer(txt,",| ");
         while (tok.hasMoreTokens()) {
            match_values.add(Pattern.compile(tok.nextToken(),Pattern.CASE_INSENSITIVE));
          }
       }
    }
   
}	// end of inner class FieldMatch





}       // end of class CatprogConditionCalendarEvent




/* end of CatprogConditionCalendarEvent.java */

