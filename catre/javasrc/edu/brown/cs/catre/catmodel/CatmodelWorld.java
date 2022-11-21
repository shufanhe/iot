/********************************************************************************/
/*                                                                              */
/*              CatmodelWorld.java                                              */
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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

abstract class CatmodelWorld implements CatreWorld
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatmodelUniverse        our_universe;
private CatreParameterSet       world_parameters;
private CatreTriggerContext     trigger_context;  
private int                     update_counter;
private ReentrantLock           update_lock;
private Condition               update_condition;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatmodelWorld(CatreUniverse uu) 
{
   our_universe = (CatmodelUniverse) uu;
   world_parameters = new CatmodelParameterSet(our_universe);
   trigger_context = null;
   update_counter = 0;
   update_lock = new ReentrantLock();
   update_condition = update_lock.newCondition();
}


/********************************************************************************/
/*                                                                              */
/*      New world methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public CatreUniverse getUniverse()             { return our_universe; }


@Override abstract public boolean isCurrent();

@Override abstract public String getUID();
 

@Override public CatreWorld createClone()
{
   return new CatmodelWorldHypothetical(this);
}



/********************************************************************************/
/*                                                                              */
/*      Parameter methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public Object getValue(CatreParameter p)
{
   return world_parameters.get(p);
}


@Override public void setValue(CatreParameter p,Object v)
{
   world_parameters.put(p,v);
}


@Override public CatreParameterSet getParameters()
{
   return world_parameters;
}


/********************************************************************************/
/*                                                                              */
/*      Condition methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override  public void addTrigger(CatreCondition c,CatrePropertySet ps)
{
   if (ps == null) ps = new CatmodelPropertySet();
   
   update_lock.lock();
   try {
      if (trigger_context == null) trigger_context = new CatmodelTriggerContext();
      trigger_context.addCondition(c,ps);
    }
   finally { 
      update_lock.unlock();
    }
}


@Override public void startUpdate()
{
   update_lock.lock();
   try {
      ++update_counter;
    }
   finally {
      update_lock.unlock();
    }
}


@Override public void endUpdate()
{
   update_lock.lock();
   try {
      --update_counter;
      if (update_counter == 0) 
         update_condition.signalAll();
    }
   finally {
      update_lock.unlock();
    }
}



@Override public CatreTriggerContext waitForUpdate()
{
   update_lock.lock();
   try {
      while (update_counter > 0) {
         update_condition.awaitUninterruptibly();
       }
      CatreTriggerContext ctx = trigger_context;
      trigger_context = null;
      return ctx;
    }
   finally {
      update_lock.unlock();
    }
}

@Override public void updateLock()
{
   update_lock.lock();
}


@Override public void updateUnlock()
{
   update_lock.unlock();
}

}       // end of class CatmodelWorld




/* end of CatmodelWorld.java */

