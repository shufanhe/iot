/********************************************************************************/
/*                                                                              */
/*              CatreUser.java                                                  */
/*                                                                              */
/*      Information about a user for CATRE                                      */
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




package edu.brown.cs.catre.catre;

import java.util.Map;

public interface CatreUser extends CatreSavable
{


/**
 *      Return the user name
 **/

String getUserName();


/**
 *      Return the homes associated with this user.  Note that thre 
 *      returned universes are associated with the given user and
 *      thus have the appropriate permissions.
 **/

CatreUniverse getUniverse();



/**
 *      Get authorization information for a bridge
 **/

CatreBridgeAuthorization getAuthorization(String name);


boolean addAuthorization(String name,Map<String,String> map);
     

void setUniverse(CatreUniverse cu);

void setNewPassword(String pwd);

boolean isTemporary();
void setTemporary(boolean fg);
void setTemporaryPassword(String  pwd);



}       // end of interface CatreUser




/* end of CatreUser.java */

