/********************************************************************************/
/*                                                                              */
/*              CatdevTransition.java                                           */
/*                                                                              */
/*      Basic implementation of a transition for a device                       */
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



package edu.brown.cs.catre.catdev;

import java.util.Collection;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

public abstract class CatdevTransition implements CatreTransition, CatdevConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreParameterSet	default_parameters;
private CatreUniverse           for_universe;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevTransition(CatreUniverse cu,CatreParameterSet dflts)
{
   this(cu);
   for_universe = cu;
   if (dflts != null) default_parameters.putAll(dflts);
}

protected CatdevTransition(CatreUniverse cu)
{
   for_universe = cu;
   default_parameters = cu.createParameterSet();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

protected void addParameter(CatreParameter p,Object value)
{
   if (value != null) {
      default_parameters.put(p,value);
    }
   else {
      default_parameters.addParameter(p);
    }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public abstract String getName();

@Override public abstract String getDescription();

@Override public String getLabel()
{
   return getName();
}


@Override public CatreParameterSet getDefaultParameters()
{
   CatreParameterSet cps = for_universe.createParameterSet();
   cps.putAll(default_parameters);
   return cps;
}

@Override public Collection<CatreParameter> getParameterSet()
{
   return default_parameters.getValidParameters();
}


@Override public CatreParameter getEntityParameter()	{ return null; }


@Override public CatreParameter findParameter(String nm)
{
   String nm1 = nm;
   String nm2 = nm;
   if (!nm.startsWith(getName() + NSEP)) {
      nm1 = getName() + NSEP + nm;
      nm2 = getName() + NSEP + "SET" + NSEP + nm;
    }
   for (CatreParameter up : default_parameters.getValidParameters()) {
      if (up.getName().equals(nm)) return up;
      if (up.getName().equals(nm1)) return up;
      if (up.getName().equals(nm2)) return up;
    }
   
   return null;
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void perform(CatreWorld w,CatreDevice e,CatrePropertySet params)
        throws CatreActionException
{
   if (e == null) throw new CatreActionException("No entity to act on");
   if (w == null) throw new CatreActionException("No world to act in");
   try {
      e.apply(this,params,w);
    }
   catch (CatreActionException ex) {
      throw ex;
    }
   catch (Throwable t) {
      throw new CatreActionException("Action aborted",t);
    }
}



}       // end of class CatdevTransition




/* end of CatdevTransition.java */

