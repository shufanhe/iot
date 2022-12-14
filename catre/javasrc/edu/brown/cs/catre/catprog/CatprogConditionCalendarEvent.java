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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreWorld;



class CatprogConditionCalendarEvent extends CatprogCondition
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<FieldMatch> field_matches;

private static CatprogCalendarChecker cal_checker = new CatprogCalendarChecker();


enum NullType { EITHER, NULL, NONNULL };
enum MatchType { IGNORE, MATCH, NOMATCH };

 
/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionCalendarEvent(CatreProgram pgm,String name,String ... fieldvalues)
{
   super(pgm,getUniqueName(name,fieldvalues));
   
   field_matches = new ArrayList<>();
   for (int i = 0; i+1 < fieldvalues.length; i += 2) {
      FieldMatch fm = new FieldMatch(fieldvalues[i],NullType.EITHER,
            MatchType.MATCH,fieldvalues[i+1]);
      field_matches.add(fm);
    }
   
   setName(name);
   
   cal_checker.addCondition(this);
}

CatprogConditionCalendarEvent(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
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

@Override public void getDevices(Collection<CatreDevice> rslt)	{ }




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   CatprogGoogleCalendar bc = CatprogGoogleCalendar.getCalendar(w);
   if (bc == null) return;
   String evt = getEventString();
   Map<String,String> rslt = new HashMap<>();
   if (bc.findEvent(w.getTime(),evt,rslt)) {
      CatrePropertySet prms = getUniverse().createPropertySet();
      for (Map.Entry<String,String> ent : rslt.entrySet()) {
	 prms.put(ent.getKey(),ent.getValue());
       }
      CatreLog.logI("CATPROG","CONDITION " + getLabel() + " ON");
      fireOn(w,prms);
    }
   else{
      CatreLog.logI("CATPROG","CONDITION " + getLabel() + " OFF");
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
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","CalendarEvent");
   
   rslt.put("FIELDS",getSubObjectArrayToSave(field_matches));
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   field_matches = getSavedSubobjectList(cs,map,"FIELDS",FieldMatch::new,
         field_matches);
   
   setUID(getUniqueName(getName(),field_matches.toArray(new String [0])));
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
   private List<String> match_values;
   
   FieldMatch(String name,NullType ntyp,MatchType mtyp,String txt) {
      super(null);
      field_name = name;
      null_type = ntyp;
      match_type = mtyp;
      match_values = new ArrayList<String>();
      StringTokenizer tok = new StringTokenizer(txt,", ");
      while (tok.hasMoreTokens()) {
         match_values.add(tok.nextToken());
       }
    }
   
   FieldMatch(CatreStore cs,Map<String,Object> map) {
      super(cs,map);
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
      Map<String,Object> rslt = super.toJson();
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
      super.fromJson(cs,map);
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





}       // end of class CatprogConditionCalendarEvent




/* end of CatprogConditionCalendarEvent.java */

