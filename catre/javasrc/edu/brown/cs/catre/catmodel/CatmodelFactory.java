/********************************************************************************/
/*                                                                              */
/*              CatmodelFactory.java                                            */
/*                                                                              */
/*      Factory for creating model entities                                     */
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

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUniverse;

public class CatmodelFactory implements CatmodelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private CatreController catre_control;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatmodelFactory(CatreController cc)
{
   catre_control = cc;
   cc.register(new UniverseTable());
}



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

public CatreUniverse createUniverse(CatreController cc,String name)
{
   return new CatmodelUniverse(cc,name);
}




/********************************************************************************/
/*                                                                              */
/*      Table Descriptor for universes                                          */
/*                                                                              */
/********************************************************************************/

private static class UniverseTable implements CatreTable {

   @Override public String getTableName()               { return "CatreUniverses"; }
   
   @Override public String getTablePrefix()             { return UNIVERSE_PREFIX; }
   
   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatmodelUniverse;
    }
   
   @Override public CatmodelUniverse create(CatreStore store,Map<String,Object> data) {
      return new CatmodelUniverse(store.getCatre(),data);
    }

}       // end of inner class UniverseTable

}       // end of class CatmodelFactory




/* end of CatmodelFactory.java */

