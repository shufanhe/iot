/********************************************************************************/
/*                                                                              */
/*              CatstoreFactory.java                                            */
/*                                                                              */
/*      Main access point for CATSTORE objects                                  */
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



package edu.brown.cs.catre.catstore;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;

public class CatstoreFactory implements CatstoreConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatstoreMongo mongo_store;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatstoreFactory(CatreController cc)
{
   mongo_store = new CatstoreMongo(cc);
   mongo_store.register(new UsersTable());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public CatreStore getStore()
{
   return mongo_store;
}



/********************************************************************************/
/*                                                                              */
/*      Table Descriptor for users                                              */
/*                                                                              */
/********************************************************************************/

private static class UsersTable implements CatreTable {
   
   @Override public String getTableName()               { return "CatreUsers"; }
   
   @Override public String getTablePrefix()             { return USERS_PREFIX; }
   
   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatstoreUser;
    }
   
   @Override public CatstoreUser create(CatreStore store,Map<String,Object> data) {
      return new CatstoreUser(store,data);
    }
   
}       // end of inner class UsersTable



}       // end of class CatstoreFactory




/* end of CatstoreFactory.java */

