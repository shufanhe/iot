/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionCalendarEvent.java                             */
/*                                                                              */
/*      Event based on user calendar access                                     */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavable;
import edu.brown.cs.catre.catre.CatreWorld;


class CatmodelConditionCalendarEvent extends CatmodelCondition 
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<FieldMatch> field_matches;
private String		 condition_name;

private static CatmodelCalendarChecker cal_checker = new CatmodelCalendarChecker();


enum NullType { EITHER, NULL, NONNULL };
enum MatchType { IGNORE, MATCH, NOMATCH };


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelConditionCalendarEvent(CatreProgram pgm,String name)
{
   super(pgm.getUniverse());
   
   field_matches = new ArrayList<>();
   
   condition_name = name;
   
   cal_checker.addCondition(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addFieldMatch(String name,String val)
{
   FieldMatch fm = new FieldMatch(name,NullType.EITHER,MatchType.MATCH,val);
   field_matches.add(fm);
}


@Override public String getDescription()
{
   // should get field data here
   return condition_name;
}


@Override public String getLabel()
{
   String s= super.getLabel();
   if (s == null) s = condition_name;
   // get default description here
   return s;
}


@Override public String getName()
{
   return condition_name;
}


@Override public boolean isBaseCondition()               { return true; }


@Override public void getSensors(Collection<CatreDevice> rslt)	{ }




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   CatmodelGoogleCalendar bc = CatmodelGoogleCalendar.getCalendar(w);
   if (bc == null) return;
   String evt = getEventString();
   Map<String,String> rslt = new HashMap<>();
   if (bc.findEvent(w.getTime(),evt,rslt)) {
      CatmodelPropertySet prms = new CatmodelPropertySet();
      for (Map.Entry<String,String> ent : rslt.entrySet()) {
	 prms.put(ent.getKey(),ent.getValue());
       }
      CatreLog.logI("CATMODEL","CONDITION " + getLabel() + " ON");
      fireOn(w,prms);
    }
   else{
      CatreLog.logI("CATMODEL","CONDITION " + getLabel() + " OFF");
      fireOff(w);
    }
}


private String getEventString()
{
   StringBuffer buf = new StringBuffer();
   for (FieldMatch fm : field_matches) {
      String s = fm.toPattern();
      if (s == null || s.length() == 0) continue;
      if (buf.length() > 0) buf.append(",");
      buf.append(s);
    }
   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Overlap checking							*/
/*										*/
/********************************************************************************/

protected boolean isConsistentWith(CatreCondition bc)
{
   if (!(bc instanceof CatmodelConditionCalendarEvent)) return true;
   
   // check if two calendar events can overlap
   return true;
}


@Override public void addImpliedProperties(CatrePropertySet ups)
{
   // need to add special calendar properties here
}



/********************************************************************************/
/*										*/
/*	Field Match information 						*/
/*										*/
/********************************************************************************/

private static class FieldMatch implements CatreSubSavable {
   
   private String   field_name;
   private NullType null_type;
   private MatchType match_type;
   private List<String> match_values;
   
   FieldMatch(String name,NullType ntyp,MatchType mtyp,String txt) {
      field_name = name;
      null_type = ntyp;
      match_type = mtyp;
      match_values = new ArrayList<String>();
      StringTokenizer tok = new StringTokenizer(txt,", ");
      while (tok.hasMoreTokens()) {
         match_values.add(tok.nextToken());
       }
    }
   
   String toPattern() {
      StringBuffer buf = new StringBuffer();
      switch (null_type) {
         case EITHER :
            break;
         case NULL :
            buf.append("!");
            buf.append(field_name);
            break;
         case NONNULL :
            buf.append(field_name);
            break;
       }
      if (!match_values.isEmpty()) {
         if (buf.length() > 0) buf.append(",");
         buf.append(field_name);
         if (match_type == MatchType.NOMATCH) buf.append("!");
         else buf.append("=");
         int ctr = 0;
         for (String s : match_values) {
            if (ctr++ > 0) buf.append("|");
            buf.append(s);
          }
       }
      return buf.toString();
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = new HashMap<>();
      rslt.put("NAME",field_name.trim()); 
      rslt.put("NULL",null_type);
      rslt.put("MATCH",match_type);
      StringBuffer buf = new StringBuffer();
      for (String s : match_values) {
         if (buf.length() > 0) buf.append(",");
         buf.append(s);
       }
      if (buf.length() > 0) rslt.put("MATCHVALUE",buf.toString());
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      field_name = getSavedString(map,"NAME","").trim();
      null_type = getSavedEnum(map,"NULL",NullType.EITHER);
      match_type = getSavedEnum(map,"MATCH",MatchType.IGNORE);
      match_values = new ArrayList<>();
      String txt = getSavedString(map,"MATCHVALUE",null);
      if (txt != null) {
         StringTokenizer tok = new StringTokenizer(txt,",| ");
         while (tok.hasMoreTokens()) {
            match_values.add(tok.nextToken());
          }
       }
    }
}	// end of inner class FieldMatch




}       // end of class CatmodelConditionCalendarEvent




/* end of CatmodelConditionCalendarEvent.java */

