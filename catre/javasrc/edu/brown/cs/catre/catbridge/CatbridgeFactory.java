/********************************************************************************/
/*                                                                              */
/*              CatbridgeFactory.java                                           */
/*                                                                              */
/*      Factory for accessing bridges                                           */
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



package edu.brown.cs.catre.catbridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreUniverse;

public class CatbridgeFactory implements CatbridgeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<CatreBridge> all_bridges;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatbridgeFactory(CatreController cc)
{
// catre_control = cc;
   all_bridges = new ArrayList<>();
   
   all_bridges.add(new CatbridgeSmartThings(cc));
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public Collection<CatreBridge> getAllBridges(CatreUniverse cu)
{
   List<CatreBridge> rslt = new ArrayList<>();
   for (CatreBridge base : all_bridges) {
      CatreBridge bridge = base.createBridge(cu);
      if (bridge != null) rslt.add(bridge);
    }
   return rslt;
}




}       // end of class CatbridgeFactory




/* end of CatbridgeFactory.java */

