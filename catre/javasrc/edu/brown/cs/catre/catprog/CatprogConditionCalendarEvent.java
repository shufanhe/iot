/********************************************************************************/
/*                                                                              */
/*              CatprogConditionCalendarEvent.java                              */
/*                                                                              */
/*      Condition based on user (google) calendar access                        */
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
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;



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


enum CalOperator { IGNORE, ISNULL, ISNONNULL, MATCH, NOMATCH };



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionCalendarEvent(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   is_on = null;
   last_device = null;
   
   param_ref.initialize();
   
   setValid(param_ref.isValid());
}


private CatprogConditionCalendarEvent(CatprogConditionCalendarEvent cc)
{
   super(cc);
   field_matches = new ArrayList<>(cc.field_matches);
   param_ref = cc.getUniverse().createParameterRef(this,cc.param_ref.getDeviceId(),
         cc.param_ref.getParameterName());
   is_on = null;
   last_device = null;
}



@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionCalendarEvent(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void noteUsed(boolean fg)
{
   param_ref.noteUsed(fg);
}


@Override public void stateChanged(CatreParameter p)
{
   if (!param_ref.isValid()) return;
   
   if (!param_ref.getDevice().isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE && !isTrigger()) fireOff();
      is_on = null;
    }
   Object cvl = param_ref.getDevice().getParameterValue(param_ref.getParameter());
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
   if (is_on != null && rslt == is_on) return;
   is_on = rslt;
   
   CatreLog.logI("CATPROG","CONDITION: " + getName() + " " + is_on);
   if (rslt) {
       fireOn(pset);
    }
   else {
      fireOff();
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
   private CalOperator match_op;
   private List<Pattern> match_values;
   
   FieldMatch(String name,CalOperator calop,String txt) {
      super(null);
      field_name = name;
      match_op = calop;
      switch (match_op) {
         case MATCH :
         case NOMATCH :
            break;
         case IGNORE :
         case ISNULL :
         case ISNONNULL :
            txt = null;
            break;
       }
      match_values = new ArrayList<>();
      if (txt != null && !txt.isEmpty()) {
         StringTokenizer tok = new StringTokenizer(txt,", ");
         while (tok.hasMoreTokens()) {
            Pattern p = Pattern.compile(tok.nextToken(),Pattern.CASE_INSENSITIVE);
            match_values.add(p);
          }
       }
    }
   
   FieldMatch(CatreStore cs,Map<String,Object> map) {
      super(cs,map);
    }
   
   boolean match(CatreCalendarEvent evt) {
      String fval = evt.getProperties().get(field_name);
      
      boolean match = false;
      if (!match_values.isEmpty()) {
         if (fval == null) fval = ""; 
         for (Pattern pmv : match_values) {
            Matcher m = pmv.matcher(fval);
            match |= m.find();
          }
       }
      
      switch (match_op) {
         case IGNORE :
            break;
         case ISNULL :
            if (fval == null || fval.isEmpty()) return true;
            else return false;
         case ISNONNULL :
            if (fval != null && !fval.isEmpty()) return true;
            else return false;
         case MATCH :
            if (match) return true;
            else return false;
         case NOMATCH :
            if (!match) return true;
            else return false;
       }
     
      return true;
    }
   
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("NAME",field_name.trim());
      rslt.put("MATCHOP",match_op);
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
      match_op = getSavedEnum(map,"MATCHOP",CalOperator.IGNORE);
      match_values = new ArrayList<>();
      switch (match_op) {
         case MATCH :
         case NOMATCH :
            String txt = getSavedString(map,"MATCHVALUE",null);
            if (txt != null) {
               StringTokenizer tok = new StringTokenizer(txt,",| ");
               while (tok.hasMoreTokens()) {
                  match_values.add(Pattern.compile(tok.nextToken(),Pattern.CASE_INSENSITIVE));
                }
             }
            break;
         default :
            break;
       }
    }
   
}	// end of inner class FieldMatch





}       // end of class CatprogConditionCalendarEvent




/* end of CatprogConditionCalendarEvent.java */

