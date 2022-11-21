/********************************************************************************/
/*                                                                              */
/*              CatmodelPropertySet.java                                        */
/*                                                                              */
/*      Map to hold properties for a world                                      */
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

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;

class CatmodelPropertySet extends HashMap<String, Object> implements CatrePropertySet
{



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/


CatmodelPropertySet()
{ }


CatmodelPropertySet(CatrePropertySet ps)
{
   super(ps);
}


CatmodelPropertySet(CatreParameterSet ps)
{
   super();
   
   for (Map.Entry<CatreParameter,Object> ent : ps.entrySet()) {
      String nm = ent.getKey().getName();
      put(nm,ent.getValue());
    }
}



}       // end of class CatmodelPropertySet




/* end of CatmodelPropertySet.java */

