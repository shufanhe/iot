/********************************************************************************/
/*										*/
/*		CatmodelFactory.java						*/
/*										*/
/*	Factory for creating model entities					*/
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




package edu.brown.cs.catre.catmodel;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;

public class CatmodelFactory implements CatmodelConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unused")
private CatreController catre_control;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatmodelFactory(CatreController cc)
{
   catre_control = cc;
   cc.register(new UniverseTable());
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

public CatreUniverse createUniverse(CatreController cc,String name,CatreUser cu)
{
   return new CatmodelUniverse(cc,name,cu);
}




/********************************************************************************/
/*										*/
/*	Table Descriptor for universes						*/
/*										*/
/********************************************************************************/

private static class UniverseTable implements CatreTable {

   @Override public String getTableName()		{ return "CatreUniverses"; }

   @Override public String getTablePrefix()		{ return UNIVERSE_PREFIX; }

   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatmodelUniverse;
    }

   @Override public CatmodelUniverse create(CatreStore store,Map<String,Object> data) {
      return new CatmodelUniverse(store.getCatre(),store,data);
    }

}	// end of inner class UniverseTable

}	// end of class CatmodelFactory




/* end of CatmodelFactory.java */

