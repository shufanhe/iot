/********************************************************************************/
/*                                                                              */
/*              CatreSession.java                                               */
/*                                                                              */
/*      Extended session implementation                                         */
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



package edu.brown.cs.catre.catre;

import org.nanohttpd.protocols.http.response.Response;

public interface CatreSession extends CatreSavable {

   CatreUser getUser(CatreController cc);
   
   CatreUniverse getUniverse(CatreController cc);
   
   String getSessionId();
   
   void setupSession(CatreUser user);
   
   void saveSession(CatreController cc);
   
   void setValue(String key,String val);
   
   String getValue(String key);
   
   Response jsonResponse(Object ... val);
   Response errorResponse(String msg);

}       // end of class CatreSession




/* end of CatreSession.java */

