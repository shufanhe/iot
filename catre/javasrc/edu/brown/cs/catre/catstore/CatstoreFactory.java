/********************************************************************************/
/*										*/
/*		CatstoreFactory.java						*/
/*										*/
/*	Main access point for CATSTORE objects					*/
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




package edu.brown.cs.catre.catstore;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;


public class CatstoreFactory implements CatstoreConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatstoreMongo mongo_store;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatstoreFactory(CatreController cc)
{
   mongo_store = new CatstoreMongo(cc);
   mongo_store.register(new UsersTable());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public CatreStore getStore()
{
   return mongo_store;
}



/********************************************************************************/
/*										*/
/*	Table Descriptor for users						*/
/*										*/
/********************************************************************************/

private static class UsersTable implements CatreTable {

   @Override public String getTableName()		{ return "CatreUsers"; }

   @Override public String getTablePrefix()		{ return USERS_PREFIX; }

   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatstoreUser;
    }

   @Override public CatstoreUser create(CatreStore store,Map<String,Object> data) {
      return new CatstoreUser(store,data);
    }

}	// end of inner class UsersTable



}	// end of class CatstoreFactory




/* end of CatstoreFactory.java */

