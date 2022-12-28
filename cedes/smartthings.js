/********************************************************************************/
/*										*/
/*		smartthings.js							*/
/*										*/
/*	Handle RESTful interface for SmartThings and CATRE			*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
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
/*										*/
/*	Authentication for iqsign						*/
/*										*/
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
}


function mainPage(context,page,configData)
{
}


async function handleSmartThingsUpdates(context,data)
{
   console.log("Update/Install",context,data)
   await context.api.subscriptions.delete();
}


async function handleSmartThingsEvents(context,event)
{
   console.log("Event",context,event);
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


