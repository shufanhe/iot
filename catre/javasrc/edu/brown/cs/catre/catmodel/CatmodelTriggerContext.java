/********************************************************************************/
/*                                                                              */
/*              CatmodelTriggerContext.java                                     */
/*                                                                              */
/*      Handle information about pending triggers                               */
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreTriggerContext;

class CatmodelTriggerContext implements CatreTriggerContext
{




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<CatreCondition,CatrePropertySet>      pending_triggers;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelTriggerContext()
{
   pending_triggers = new ConcurrentHashMap<CatreCondition,CatrePropertySet>();
}

CatmodelTriggerContext(CatreCondition uc,CatrePropertySet us)
{
   this();
   addCondition(uc,us);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void addCondition(CatreCondition uc,CatrePropertySet us) 
{
   if (us == null) us = new CatmodelPropertySet();
   us.put("*TRIGGER*",Boolean.TRUE);
   pending_triggers.put(uc,us);
}


void addContext(CatmodelTriggerContext ctx)
{
   pending_triggers.putAll(ctx.pending_triggers);
}


void clear()
{
   pending_triggers.clear();
}


@Override public CatrePropertySet checkCondition(CatreCondition c)
{
   return pending_triggers.get(c);
}





}       // end of class CatmodelTriggerContext




/* end of CatmodelTriggerContext.java */

