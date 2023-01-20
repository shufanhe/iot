/********************************************************************************/
/*                                                                              */
/*              CatmodelTriggerContext.java                                     */
/*                                                                              */
/*      Handle information about pending triggers for a world                   */
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




package edu.brown.cs.catre.catmodel;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreTriggerContext;


class CatmodelTriggerContext implements CatreTriggerContext, CatmodelConstants
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
   pending_triggers = new ConcurrentHashMap<>();
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
   if (us == null) us = uc.getUniverse().createPropertySet();
   us.put("*TRIGGER*",Boolean.TRUE);
   pending_triggers.put(uc,us);
}


@Override public void addContext(CatreTriggerContext cctx)
{
   CatmodelTriggerContext ctx = (CatmodelTriggerContext) cctx;
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

