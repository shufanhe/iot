/********************************************************************************/
/*										*/
/*		CatprogConditionLogical.java					*/
/*										*/
/*	Logical (AND/OR) combination of conditions				*/
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




package edu.brown.cs.catre.catprog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTriggerContext;

abstract class CatprogConditionLogical extends CatprogCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


protected List<CatreCondition>	arg_conditions;
private CondChanged		cond_handler;
private CondUpdater		cond_updater;
private boolean 		first_time;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatprogConditionLogical(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
throws CatreConditionException
{
   super(pgm,cs,map);

   first_time = true;
   cond_handler = null;
   cond_updater = new CondUpdater();

   setupTriggers();
}


protected CatprogConditionLogical(CatprogConditionLogical ccl)
{
   super(ccl);
   arg_conditions = new ArrayList<>();
   for (CatreCondition cc : ccl.arg_conditions) {
      arg_conditions.add(cc.cloneCondition());
    }

   first_time = true;
   cond_handler = null;
   cond_updater = new CondUpdater();

   try {
      setupTriggers();
    }
   catch (CatreConditionException e) { }
}


protected void setConditionName(String s1,String s2)
{
   if (getName() != null && !getName().equals("")) return;

   StringBuffer buf = new StringBuffer();
   for (CatreCondition c : arg_conditions) {
      if (buf.length() > 0) buf.append(s1);
      buf.append(c.getName());
    }
   setName(buf.toString());
   buf = new StringBuffer();
   for (CatreCondition c : arg_conditions) {
      if (buf.length() > 0) buf.append(" " + s2 + " ");
      buf.append(c.getLabel());
    }
   setLabel(buf.toString());
   buf = new StringBuffer();
   for (CatreCondition c : arg_conditions) {
      if (buf.length() > 0) buf.append(s1);
      buf.append(c.getDescription());
    }
   setDescription(buf.toString());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void noteUsed(boolean fg)
{
   for (CatreCondition cc : arg_conditions) {
      cc.noteUsed(fg);
    }
}


@Override public boolean isTrigger()
{
   for (CatreCondition bc : arg_conditions) {
      if (bc.isTrigger()) return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override protected void setTime()
{
   if (first_time) {
      first_time = false;
      checkState(null);
    }

   cond_updater.beginHold();
   for (CatreCondition c : arg_conditions) {
      if (c != null && c instanceof CatprogCondition) {
	 CatprogCondition cc = (CatprogCondition) c;
	 cc.setTime();
       }
    }
   cond_updater.endHold();
}



private void setupTriggers() throws CatreConditionException
{
   checkTriggers();

}

protected abstract void checkTriggers() throws CatreConditionException;


private void checkState(CatreTriggerContext ctx)
{
   try {
      CatrePropertySet ps = recompute(ctx);
      CatreLog.logI("CATMODEL","CONDITION " + getLabel() + " " + (ps != null));
      if (ps != null) {
	 if (ps.get("*TRIGGER*") != null) fireTrigger(ps);
	 else fireOn(ps);
       }
      else fireOff();
    }
   catch (Throwable t) {
      fireError(t);
    }
}


abstract protected CatrePropertySet recompute(CatreTriggerContext ctx)
throws CatreConditionException;



private class CondUpdater {

   private boolean doing_hold;
   private CatreTriggerContext change_context;

   CondUpdater() {
      doing_hold = false;
      change_context = null;
    }

   synchronized void beginHold() {
     doing_hold = true;
    }

   void endHold() {
      CatreTriggerContext ctx = null;
      synchronized (this) {
	 doing_hold = false;
	 ctx = change_context;
	 change_context = null;
	 if (ctx == null) return;
       }
      checkState(ctx);
    }

   void update(CatreTriggerContext ctx) {
      synchronized (this) {
	 if (doing_hold) {
	    CatreTriggerContext octx = change_context;
	    if (octx != null && ctx != null) octx.addContext(ctx);
	    else if (octx == null) {
	       if (ctx == null) ctx = getUniverse().createTriggerContext();
	       change_context = ctx;
	     }
	    return;
	  }
       }
      checkState(ctx);
    }

}	// end of inner class CondUpdater




/********************************************************************************/
/*										*/
/*	Handle subcondition changes						*/
/*										*/
/********************************************************************************/

@Override public void addConditionHandler(CatreConditionListener hdlr)
{
   super.addConditionHandler(hdlr);

   if (cond_handler == null) {
      cond_handler = new CondChanged();
      for (CatreCondition cc : arg_conditions) {
	 cc.addConditionHandler(cond_handler);
       }
    }
}


@Override public void removeConditionHandler(CatreConditionListener hdlr)
{
   super.removeConditionHandler(hdlr);

   if (cond_handler != null) {
      for (CatreCondition cc : arg_conditions) {
	 cc.removeConditionHandler(cond_handler);
       }
      cond_handler = null;
    }
}



private class CondChanged implements CatreConditionListener {

   @Override public void conditionError(CatreCondition cc,Throwable t) {
      cond_updater.update(null);
    }

   @Override public void conditionOff(CatreCondition cc) {
      cond_updater.update(null);
    }

   @Override public void conditionOn(CatreCondition cc,CatrePropertySet ps) {
      cond_updater.update(null);
    }

   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet ps) {
      CatreTriggerContext ctx = getUniverse().createTriggerContext();
      ctx.addCondition(cc,ps);
      cond_updater.update(ctx);
    }

}	// end of inner class CondUpdateListener




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("CONDITIONS",getSubObjectArrayToSave(arg_conditions));

   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   arg_conditions = getSavedSubobjectList(cs,map,"CONDITIONS",
	 for_program::createCondition,arg_conditions);
}



/********************************************************************************/
/*										*/
/*	AND implementation							*/
/*										*/
/********************************************************************************/

static class And extends CatprogConditionLogical {

   And(CatreProgram pgm,CatreStore cs,Map<String,Object> map) throws CatreConditionException {
      super(pgm,cs,map);
      setConditionName("&&","AND");
      checkTriggers();
    }

   private And(And cc) {
      super(cc);
    }

   @Override public CatreCondition cloneCondition() {
      return new And(this);
    }

   protected void checkTriggers() throws CatreConditionException {
      int tct = 0;
      for (CatreCondition bc : arg_conditions) {
	 if (bc.isTrigger()) ++tct;
       }
      if (tct > 1) throw new CatreConditionException("Can't AND multiple triggers");
    }

   @Override protected	CatrePropertySet recompute(CatreTriggerContext ctx)
   throws CatreConditionException {
      CatrePropertySet ups = getUniverse().createPropertySet();
      for (CatreCondition c : arg_conditions) {
	 CatrePropertySet ns = null;
	 if (ctx != null) ns = ctx.checkCondition(c);
	 if (ns == null) ns = c.getCurrentStatus();
	 if (ns == null) return null;
	 ups.putAll(ns);
       }
      return ups;
    }

   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("TYPE","And");
      return rslt;
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
   }

}	// end of inner class And




/********************************************************************************/
/*										*/
/*	OR implementation							*/
/*										*/
/********************************************************************************/

static class Or extends CatprogConditionLogical {

   Or(CatreProgram pgm,CatreStore cs,Map<String,Object> map) throws CatreConditionException {
      super(pgm,cs,map);
      setConditionName("||","OR");
      checkTriggers();
    }

   private Or(Or cc) {
      super(cc);
    }

   @Override public CatreCondition cloneCondition() {
      return new Or(this);
    }


   @Override protected void checkTriggers() throws CatreConditionException {
      int tct = 0;
      for (CatreCondition bc : arg_conditions) {
	 if (bc.isTrigger()) ++tct;
       }
      if (tct != 0 && tct != arg_conditions.size())
	 throw new CatreConditionException("OR must be either all triggers or no triggers");
    }

   @Override protected	CatrePropertySet recompute(CatreTriggerContext ctx)
   throws CatreConditionException {
      CatrePropertySet ups = null;
      for (CatreCondition c : arg_conditions) {
	 CatrePropertySet ns = null;
	 if (ctx != null) ns = ctx.checkCondition(c);
	 if (ns == null) ns = c.getCurrentStatus();
	 if (ns != null) {
	    if (ups == null) ups = getUniverse().createPropertySet();
	    ups.putAll(ns);
	  }
       }
      return ups;
    }


   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("TYPE","Or");
      return rslt;
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
    }

}	// end of inner class Or


}	// end of class CatprogConditionLogical




/* end of CatprogConditionLogical.java */

