/********************************************************************************/
/*										*/
/*		CatprogRule.java						*/
/*										*/
/*	Implementation of a rule						*/
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

import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreCreationException;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreRule;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTriggerContext;


class CatprogRule extends CatreDescribableBase implements CatreRule, CatprogConstants
{

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatprogProgram	for_program;
private String          device_id;
private List<CatreCondition> for_conditions;
private List<CatreAction>  for_actions;
private double		rule_priority;
private volatile RuleRunner active_rule;
private long		creation_time;
private boolean         force_trigger;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogRule(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
	throws CatreCreationException
{
   super("RULE_");

   for_program = (CatprogProgram) pgm;
   rule_priority = -1;
   for_conditions = null;
   for_actions = null;
   active_rule = null;
   force_trigger = false;
   device_id = null;

   fromJson(cs,map);
   
   if (device_id == null && for_actions != null) {
      for (CatreAction ca : for_actions) {
         device_id = ca.getTransition().getDevice().getDeviceId();
         break;
       }
    }

   if (!validateRule()) {
      CatreLog.logE("Invalid rule found: " + map + " " + rule_priority + " " +
         for_conditions + " " + for_actions + " " + getName() + " " + device_id);
      throw new CatreCreationException("Invalid rule");
    }
}



private boolean validateRule()
{
   if (rule_priority < 0) return false;
   if (for_conditions == null || for_conditions.size() == 0) return false;
   if (for_actions == null || for_actions.size() == 0) return false;
   if (getName() == null || getName().equals("")) return false;
   if (device_id == null) return false;

   return true;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public List<CatreCondition> getConditions()   { return for_conditions; }

@Override public List<CatreAction> getActions() 	{ return for_actions; }

@Override public double getPriority()			{ return rule_priority; }

@Override public void setPriority(double p)		{ rule_priority = p; }


@Override public long getCreationTime() 		{ return creation_time; }

@Override public boolean isExplicit()			{ return true; }

@Override public boolean isTrigger()                    { return force_trigger; }

@Override public CatreDevice getTargetDevice()         
{
   return for_program.getUniverse().findDevice(device_id);
}

@Override public String getTargetDeviceId()
{
   return device_id;
}


/********************************************************************************/
/*										*/
/*	Application methods							*/
/*										*/
/********************************************************************************/

@Override public boolean apply(CatreTriggerContext ctx)
	throws CatreConditionException, CatreActionException
{
   CatrePropertySet ps = null;
   for (CatreCondition cc : for_conditions) {
      CatrePropertySet ns = null;
      if (ctx != null) ns = ctx.checkCondition(cc);
      if (ns == null) ns = cc.getCurrentStatus();
      if (ns == null) return false;
      if (ps == null) ps = ns;
      else ps.putAll(ns);
    }
   if (ps == null) return false;

   CatreLog.logI("CATPROG","Apply " + getLabel());

   if (for_actions != null) {
      active_rule = new RuleRunner(ps);
      active_rule.applyRule();
    }

   return true;
}


@Override public void abort()
{
   RuleRunner rr = active_rule;
   if (rr != null) rr.abort();
}




private class RuleRunner implements Runnable {

   private CatrePropertySet param_set;
   private Thread runner_thread;
   private boolean is_aborted;
   private Throwable fail_code;

   RuleRunner(CatrePropertySet ps) {
      param_set = ps;
      fail_code = null;
      is_aborted = false;
      runner_thread = null;
    }

   void abort() {
      synchronized (this) {
	 // don't want to interrupt thread if it has finished rule
	 if (active_rule != null && runner_thread != null) {
	    is_aborted = true;
	    if (runner_thread != Thread.currentThread())
	       runner_thread.interrupt();
	  }
       }
    }

   @Override public void run() {
      runner_thread = Thread.currentThread();
      applyRule();
    }

   void applyRule() {
      try {
	 try {
	    for (CatreAction a : for_actions) {
	       a.perform(param_set);
	       synchronized (this) {
		  if (Thread.currentThread().isInterrupted() || is_aborted) {
		     break;
		   }
		}
	     }
	  }
	 catch (CatreActionException ex) {
	    fail_code = ex;
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATPROG","Problem execution action",t);
	    t.printStackTrace();
	    fail_code = t;
	  }
	 if (fail_code != null) {
	    // might want to run exception actions here
	  }
       }
      finally {
	 synchronized (this) {
	    active_rule = null;
	    runner_thread = null;
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("PRIORITY",getPriority());
   rslt.put("CREATED",creation_time);
   rslt.put("TRIGGER",force_trigger);
   rslt.put("CONDITIONS",getSubObjectArrayToSave(for_conditions));
   rslt.put("ACTIONS",getSubObjectArrayToSave(for_actions));
   rslt.put("DEVICEID",device_id);

   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   rule_priority = getSavedDouble(map,"PRIORITY",-1);
   force_trigger = getSavedBool(map,"TRIGGER",false);
   for_conditions = getSavedSubobjectList(cs,map,"CONDITIONS",for_program::createCondition,
	 for_conditions);
   for_actions = getSavedSubobjectList(cs,map,"ACTIONS",
	 for_program::createAction,for_actions);
   device_id = getSavedString(map,"DEVICEID",device_id);

   creation_time = getSavedLong(map,"CREATED",System.currentTimeMillis());
}


}	// end of class CatprogRule




/* end of CatprogRule.java */

