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
const signdata = config.getSignDeviceData();


const connector = new SchemaConnector()
   .clientId(stcreds.client_id)
   .clientSecret(stcreds.client_secret)
   .enableEventLogging()
   .discoveryHandler(handleSTDiscovery)
   .stateRefreshHandler(handleSTStateRefresh)
   .commandHandler(handleSTCommand)
// .callbackAccessHandler(handleSTCallbackAccess)
   .interactionResultHandler(handleSTResult)
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
   
   let user = await validateToken(req,res);
   if (!user) return;
   
   req.body.user = user;
   
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
   
   let usr = body.user;
   
   let rows = await db.query("SELECT * FROM iQsignSigns WHERE userid = $1",
         [ usr.id ]);
   for (let row of rows) {
      console.log("ADD DEVICE",row);
      let sid = "" + row.id;
      resp.addDevice(sid,row.name,signdata.uuid)
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
      let sid = "" + row.id;
      // should look at states in signdata
      resp.addDevice(sid, [ ]);
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



function handleSTResult(token,data)
{
   console.log("INTERACTION RESULT:",token,JSON.stringify(data));
   for (let ds of data.deviceState) {
      console.log("DEVICE",ds.externalDeviceId);
      for (let de of ds.deviceError) {
         console.log("ERR",de.errorEnum,de.detail,de.value);
       }
    }
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


async function validateToken(req,res)
{
   let tok = req.body.authentication.token;
   let now = new Date();
   
   let row = await db.query1("SELECT * FROM OauthTokens WHERE access_token = $1",
         [ tok ]);
   let d1 = row.access_expires_on;
   console.log("CHECK",now.getTime(),d1.getTime());
   if (d1.getTime() >= now.getTime()) {
      let urow = await db.query1("SELECT * FROM iQsignUsers WHERE id = $1",
            [ row.userid ]);
      console.log("SET USER",urow);
      if (urow.valid) {
         return urow;
       }
    }
   
   console.log("INVALID",tok);

   res.status(401).send('Unauthorized');
   
   return null;
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
