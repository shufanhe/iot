/********************************************************************************/
/*										*/
/*		smartthings.js							*/
/*										*/
/*	Handle RESTful interface for SmartThings and CATRE			*/
/*										*/
/*	Written by spr								*/
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

"use strict";
  
const SmartApp = require('@smartthings/smartapp');
const FileContextStore = require('@smartthings/file-context-store')  
      
const config = require("./config");

var smartapp;



/********************************************************************************/
/*										*/
/*	Setup Router								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   setupSmartApp();
   
   restful.post("/smartthings",handleSmartThings);
   restful.get("/smartthings",handleSmartThingsGet);
   
   restful.use(authenticate);
   
   restful.all("*",config.handle404)
   restful.use(config.handleError);

   return restful;
}


/********************************************************************************/
/*                                                                              */
/*      Authentication for iqsign                                               */
/*                                                                              */
/********************************************************************************/

function authenticate(req,res,next)
{
   next();
}



function addBridge(authdata)
{
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Setup smart app                                                         */
/*                                                                              */
/********************************************************************************/

function setupSmartApp()
{
   let creds = config.getSmartThingsCredentials();
   
   smartapp = new SmartApp()
      .enableEventLogging(2)
      .clientSecret(creds.clientSecret)
      .clientId(creds.clientID)
      .appId(creds.appId)
      .page('mainPage',mainPage)
      .permissions(['r:devices:*','w:devices:*','x:devices:*','r:customcapability'])
      .updated(handleSmartThingsUpdates)
      .subscribedEventHandler(handleSmartThingsEvents)
      .contextStore(new FileContextStore());
   
   console.log("SMARTTHINGS APP SETUP",smartapp);
   
   let devs = await smartapp.devices.list();
   console.log("SMARTAPP FOUND DEVICES",devs.length,devs);
}


function mainPage(context,page,configData)
{
   console.log("SMARTTHINGS MAINPAGE",page,configData);
}


async function handleSmartThingsUpdates(context,data)
{
   console.log("SMARTTHINGS Update/Install",context,data)
   await context.api.subscriptions.delete();
}


async function handleSmartThingsEvents(context,event)
{
   console.log("SMARTTHINGS Event",context,event);
}



/********************************************************************************/
/*                                                                              */
/*      Smart things interactions                                               */
/*                                                                              */
/********************************************************************************/

async function handleSmartThings(req,res)
{
   console.log("SMARTTHINGS",req.body);
   
   await smartapp.handleHttpCallback(req,res);
}


async function handleSmartThingsGet(req,res)
{
   console.log("SMARTTHINGS-GET",req.query);
   
   req.body = req.query;
   
   handleSmartThings(req,res);
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;



/* end of generic.js */


