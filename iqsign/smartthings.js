/********************************************************************************/
/*										*/
/*		smartthings.js							*/
/*										*/
/*	Server for smartthing interface 					*/
/*										*/
/********************************************************************************/
"use string";


const http = require('http');
const https = require('https');

const express = require('express');
const errorHandler = require('errorhandler');

const logger = require('morgan');
const bodyparser = require('body-parser');
const favicons = require("connect-favicons");

const { StateRefreshResopnse, StateUpdateRequest, 
      SchemaConnector, DeviceErrorTypes,
      partnerHelper, CommandResponse } = require('st-schema');

const stPartnerHelper = new partnerHelper( {}, {} );

const util = require('util');

const config = require("./config");
const db = require("./database");
const sign = require("./sign");

const stcreds = config.getSmartThingsCredentials();

const connector = new SchemaConnector()
   .clientId(stcreds.client_id)
   .clientSecret(stcreds.client_secret)
   .enableEventLogging()
   .discoveryHandler(handleSTDiscovery)
   .stateRefreshHandler(handleSTStateRefresh)
   .commandHandler(handleSTCommand)
// .callbackAccessHandler(handleSTCallbackAccess)
   .integrationDeletedHandler(handleSTIntegrationDeleted);




/********************************************************************************/
/*										*/
/*	Handle requests 							*/
/*										*/
/********************************************************************************/

async function handleSmartThingsGet(req,res)
{
   req.body = req.query;
   return await handleSmartThings(req,res);
}


async function handleSmartThings(req,res)
{
   console.log("HANDLE SMART THINGS",req.body,req.header('Authorization'),req.path,req.query,
      req.body.headers,req.body.authentication);

   req.url = req.originalUrl;
   
   if (req.body.lifecycle == null && req.body.headers != null) {
      return await handleInteraction(req,res);
    }
   
   let rslt = { }
   switch (req.body.lifecycle) {
      case "CONFIRMATION" :
	 rslt = { targetUrl : "https://sherpa.cs.brown.edu:3334/smartthings" };
	 break;
	
    }

   res.end(JSON.stringify(rslt));
}


async function handleInteraction(req,res)
{
   console.log("HANDLE INTERATION",req.path,req.body.headers,req.body.authentication);
   if (!validateToken(req,res)) return;
   
   console.log("SETTING UP CALLBACK",req.body);
   
   connector.handleHttpCallback(req,res);
}


async function handleSmartThingsCommand(req,res)
{ 
   console.log("HANDLE COMMAND",req.body,req.header);
}



/********************************************************************************/
/*                                                                              */
/*      StSchema callbacks                                                      */
/*                                                                              */
/********************************************************************************/

async function handleSTDiscovery(token,resp,body)
{
   console.log("ST DISCOVERY",token,resp,body);
   
   let rows = await db.select("SELECT * FROM iQsignSigns WHERE userid = $1",
         [ body.user.id ]);
   for (let row of rows) {
      console.log("ADD DEVICE",row);
      resp.addDevice(row.id,row.name,"iQsign1")
         .manufacturerName("SPR")
         .modelName("iQsign")
         .swVersion("1.0")
    }
}


async function handleSTStateRefresh(token,resp,body)
{
   console.log("ST STATE REFRESH",token,resp,body);
   let rows = await db.select("SELECT * FROM iQsignSigns WHERE userid = $1",
         [ body.user.id ]);
   for (let row of rows) {
      console.log("ADD DEVICE",row);
      let weburl = sign.getWebUrl(row.namekey);
      let imageurl = sign.getImageUrl(row.namekey);
      resp.addDevice(row.id, [
      { component : 'lastupdate',
            capability: 'iqsign',
            attribute : 'lastupdate',
            value : row.lastupdate },
      { component : 'weburl',
            capability : 'iqsign',
            attribute : 'weburl',
            value : weburl },
      { component : 'imageurl',
            capability : 'iqsign',
            attribute : 'imageurl',
            value : imageurl } ]);
    } 
}



function handleSTCommand(token,resp,devices)
{
   console.log("ST COMMAND",token,resp,devices);
}


function handleSTIntegrationDeleted(token,data)
{
   console.log("ST INTEGRATION DELETED",token,data);
}




/********************************************************************************/
/*										*/
/*	Basic handlers								*/
/*										*/
/********************************************************************************/

function handleRequest(req,res)
{
   if (checkAccessToken(req,res)) {
      connector.handleHttpCallback(req, res)
    }
   else {
      res.status(401).send('Unauthorized');
    }
}

function handleCommand(req,res)
{
   console.log("COMMAND",req.params,req.body);

   if (checkAccessToken(req,res)) {
      connector.handleHttpCallback(req, res)
    }
   else {
      res.status(401).send('Unauthorized');
    }

   for (const accesstoken of Object.keys(connector.accessTokens)) {
      const item = connector.accessTokens[accesstoken];
//    const updatereq = new StateUpdateRequest(process.env.ST_CLIENT_ID,process.env.ST_CLIENT_SECRET);
//    const devicestate = [ { externalDeviceId: 'external-device-1' , states: [
//    {
//	 component: 'main',
//	    capability: req.body.attribute === 'level' ? 'st.switchLevel' : 'st.switch',
//		  attribute: req.body.attribute,
//		  value: req.body.value
//     } ] } ];
//    updatereq.updateState(item.callbackUrls,item.callbackAuthentication,devicestate);
      res.send({});
      res.end();
    }

}



/********************************************************************************/
/*                                                                              */
/*      Authorization                                                           */
/*                                                                              */
/********************************************************************************/

function checkAccessToken(req,res) {
   // Replace with proper validation of issued access token
   if (req.body.authentication && req.body.authentication.token) {
      return true;
    }
   return false;
}


async function validateToken(req,res,next)
{
   let auth = req.body.authentication;
   let now = new Date();
   
   if (auth && auth.token) {
      let row = await db.query1("SELECT * FROM OauthTokens WHERE access_token = $1",
            [ auth.token ]);
      let d1 = new Date(row.access_expires_on);
      console.log("CHECK",now,d1);
      console.log("CHECK1",now.getTime(),d1.getTime());
      if (d1.getTime() >= now.getTime()) {
         let urow = await db.query1("SELECT * FROM iQsignUsers WHERE id = $1",
               [ row.userid ]);
         req.body.user = urow;
         console.log("SET USER",urow);
         return true;
       }
    }
   
   console.log("INVALID",auth,auth.token);

   res.status(401).send('Unauthorized');
   
   return false;
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.handleSmartThings = handleSmartThings;
exports.handleSmartThingsGet = handleSmartThingsGet;
exports.handleSmartThingsCommand = handleSmartThingsCommand;



/* end of module smartthings */
