/********************************************************************************/
/*										*/
/*		CatserveConstants.java						*/
/*										*/
/*	Constants for RESTful web interface for CATRE				*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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



package edu.brown.cs.catre.catserve;



public interface CatserveConstants
{

int	HTTP_PORT = 3334;
int	HTTPS_PORT = 3334;

String SESSION_COOKIE = "Catre.Session";
String SESSION_PARAMETER = "CATRESESSION";


String	XML_MIME = "application/xml";
String	JSON_MIME = "application/json";
String	TEXT_MIME = "text/plain";
String	HTML_MIME = "text/html";
String	CSS_MIME = "text/css";
String	JS_MIME = "application/javascript";
String	SVG_MIME = "image/svg+xml";
String	PNG_MIME = "image/png";

String SESSION_PREFIX = "SESS_";





}	// end of interface CatserveConstants




/* end of CatserveConstants.java */

