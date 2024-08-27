/********************************************************************************/
/*                                                                              */
/*              CatprogConditionOr.java                                         */
/*                                                                              */
/*      Or of a set of conditions                                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogConditionOr extends CatprogCondition
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<CatprogCondition>  or_conditions;
private CondChanged             cond_handler;
private StateRepr               active_state;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionOr(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   or_conditions = new ArrayList<>();
   active_state = new StateRepr();
   noteIsShared();
   pgm.addSharedCondition(this);
}


CatprogConditionOr(CatprogConditionOr cc)
{
   super(cc);
   or_conditions = new ArrayList<>(cc.or_conditions);
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionOr(this);
}


@Override public void activate()
{
   for (CatreCondition cc : or_conditions) {
      cc.activate();
    }
}


/********************************************************************************/
/*										*/
/*	Handle changes to the condition 					*/
/*										*/
/********************************************************************************/

@Override public void addConditionHandler(CatreConditionListener hdlr) 
{
   super.addConditionHandler(hdlr);
   
   if (cond_handler == null) {
      cond_handler = new CondChanged();
      for (CatreCondition cc : or_conditions) {
         cc.addConditionHandler(cond_handler);
       }
    }
}


@Override public void removeConditionHandler(CatreConditionListener hdlr)
{
   super.removeConditionHandler(hdlr);
   
   if (cond_handler != null && !hasConditionHandlers()) { 
      for (CatreCondition cc : or_conditions) {
         cc.removeConditionHandler(cond_handler);
       }
      cond_handler = null;
    }
}



private class CondChanged implements CatreConditionListener {

   @Override public void conditionError(CatreCondition cc,Throwable t) {
      active_state.noteError(cc,t);
    } 
   
   @Override public void conditionOn(CatreCondition cc,CatrePropertySet ps) {
      active_state.noteOn(cc,ps);
    }
   
   @Override public void conditionOff(CatreCondition cc) {
      active_state.noteOff(cc);
    }
   
   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet ps) {
      active_state.noteOn(cc,ps);
    }
   
   @Override public void conditionValidated(CatreCondition cc,boolean valid) {
      setValid(valid);
      if (!valid) active_state.noteOff(cc);
    }

}	// end of inner class CondChanged



/********************************************************************************/
/*                                                                              */
/*      State representation                                                    */
/*                                                                              */
/********************************************************************************/

private class StateRepr {
   
   private Map<CatreCondition,CatrePropertySet> cur_values;
   private Map<CatreCondition,Throwable> error_cause;
   private boolean is_on;
   private boolean is_error;
   
   StateRepr() {
      error_cause = new HashMap<>();
      cur_values = new HashMap<>();
      is_on = false;
      is_error = false;
    }
   
   void noteError(CatreCondition cc,Throwable t) {
      if (t == null) t = new Error("Unknown error");
      error_cause.put(cc,t);
      cur_values.remove(cc);
      updateStatus();
    }
   
   void noteOn(CatreCondition cc,CatrePropertySet ps) {
      error_cause.remove(cc);
      cur_values.put(cc,ps);
      updateStatus();
    }
   
   void noteOff(CatreCondition cc) {
      error_cause.remove(cc);
      updateStatus();
    }
   
   private void updateStatus() {
      if (error_cause.isEmpty()) {
         if (!cur_values.isEmpty()) {
            if (!is_on) {
               CatrePropertySet rslt = getUniverse().createPropertySet();
               for (CatrePropertySet ps : cur_values.values()) {
                  rslt.putAll(ps);
                }
               fireOn(rslt);
               is_on = true;
             }
          }
         else if (is_on || is_error) {
            fireOff();
          }
         is_error = false;
       }
      else if (!is_error) {
         Throwable t = null;
         for (Throwable t1 : error_cause.values()) {
            t = t1;
            break;
          }
         fireError(t);
         is_error = true;
       }
    }
   
}       // end of inner class StateRepr



/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Or");
   rslt.put("CONDITIONS",getSubObjectArrayToSave(or_conditions));
   
   return rslt;
}


@Override 
public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   or_conditions = getSavedSubobjectList(cs,map,"CONDITIONS",
         for_program::createCondition,or_conditions);
}


@Override boolean isUndefined()
{
   for (Iterator<CatprogCondition> it = or_conditions.iterator(); it.hasNext(); ) {
      CatprogCondition cc = it.next();
      if (cc.isUndefined()) it.remove();
    }
         
   if (or_conditions == null || or_conditions.isEmpty()) return true;
   
   return super.isUndefined();
}



}       // end of class CatprogConditionOr




/* end of CatprogConditionOr.java */

