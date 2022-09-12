/********************************************************************************/
/*										*/
/*		smartthings.js							*/
/*										*/
/*	Server for smartthing interface 					*/
/*										*/
/********************************************************************************/
"use strict";


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
   req.url = req.originalUrl;
   
   if (req.body.lifecycle == null && req.body.headers != null) {
      return await handleSmartInteraction(req,res);
    }
   
   console.log("HANDLE SMART THINGS",req.body,req.path);
   
   let rslt = { }
   switch (req.body.lifecycle) {
      case "CONFIRMATION" :
	 rslt = { targetUrl : "https://sherpa.cs.brown.edu:3336/smartapp" };
	 break;
      case "CONFIGURATION" :
         rslt = handleConfiguration(req.body);
         break;
      case "INSTALL" :
         rslt = await handleInstall(req.body);
         break;
      case "UPDATE" :
         break;
      case "EVENT" :
         break;
      case "UNINSTALL" :
         break;
      case "PING" :
         break;
    }

   res.end(JSON.stringify(rslt));
}



function handleConfiguration(body)
{
   let cfd = { };
         
   switch (body.configurationData.phase) {
      case "INITIALIZE" :
         console.log("INIT",body.configurationData.config);
         cfd = { initialize : { 
            name : "iQsign",
               description : "Intelligent Sign",
               id : "iQsignApp",
               firstPageId : "1" }
          };
         break;
      case "PAGE" :
         let page = {
               pageId : "1",
               name : "Intelligent Sign",
               nextPageId : null,
               previousPageId : null,
//             complete : true,
               sections : [ {
                  name : "Login Code for Sign",
                     settings : [ {
                        id : "logincode",
                           name : "Login Code",
                           description : "Login code from web site",
                           type : "TEXT",
                           required : true
                      } ]
                } ]
          };
         cfd = { page : page };
         break;
    }
   console.log("CONFIGURE RESULT",cfd,cfd.sections);   
   
   if (cfd != null) return { configurationData : cfd };
}



async function handleInstall(body)
{
   let code = body.installData.config.logincode[0].stringConfig.value;
   let row = await db.query1("SELECT * FROM iQsignLoginCodes WHERE code = $1",
         [ code ]);
   let outid = body.executionId;
   if (row.outid == null) {
      await db.query("UPDATE iQsignLoginCodes SET outsideid = $1 WHERE code = $2",
            [ outid, code ]);
    }
   else if (outid != row.outid) {
      // probably should be error
      await db.query("UPDATE iQsignLoginCodes SET outsideid = $1 WHERE code = $2",
            [ outid, code ]); 
    }
   
   return { installData : {} };
}




/********************************************************************************/
/*                                                                              */
/*      StSchema callbacks                                                      */
/*                                                                              */
/********************************************************************************/

async function handleSmartInteraction(req,res)
{
   console.log("ST HANDLE INTERATION",req.path,req.body.headers,req.body.authentication);
   
   let user = await validateToken(req,res);
   if (!user) return;
   
   req.body.user = user;
   
   console.log("SETTING UP CALLBACK",req.body);
   
   connector.handleHttpCallback(req,res);
}


async function handleSmartThingsCommand(req,res)
{ 
   console.log("ST HANDLE COMMAND",req.body,req.header);
}



async function handleSTDiscovery(token,resp,body)
{
   console.log("ST DISCOVERY",token,resp,body);
   
   let usr = body.user;
   
   let rows = await db.query("SELECT * FROM iQsignSigns WHERE userid = $1",
         [ usr.id ]);
   for (let row of rows) {
      console.log("ADD DEVICE",row);
      let sid = await getSignCode(row);
      resp.addDevice(sid,row.name,stcreds.device_profile_id)
         .manufacturerName("SPR")
         .modelName("iQsign")
         .swVersion("1.0")
    }
}


async function handleSTStateRefresh(token,resp,body)
{
   console.log("ST STATE REFRESH",token,resp,body);
   
   let usr = body.user;
   
   let rows = await db.query("SELECT * FROM iQsignSigns WHERE userid = $1",
         [ usr.id ]);
   for (let row of rows) {
      console.log("REFRESH DEVICE",row);
      let sid = await getSignCode(row);
      let states = await getStates(row);
      console.log("STATES",states);
      // should look at states in signdata
      resp.addDevice(sid,states);
    } 
}


async function getStates(devinfo)
{
   let weburl = sign.getWebUrl(devinfo.namekey);
   let imageurl = sign.getImageUrl(devinfo.namekey);
   let dispname = await sign.getDisplayName(devinfo);
   
   let s0 = { component: "main",
              capability: "st.healthCheck",
              attribute: "DeviceWatch-DeviceStatus",
              value : "online" };
// let s1 = { component: "main",
//       capability: "st.healthCheck",
//       attribute: "DeviceWatch-Enroll",
//       value : "https://sherpa.cs.brown.edu:3336/health" };
   let s2 = { component: "main",
         capability: "st.healthCheck",
         attribute: "checkInterval",
         value : 302400 };
   let s3 = { component: "main",
         capability: "st.healthCheck",
         attribute: "healthStatus",
         value : "online" };  
   let s4 = { component: "main",
         capability: "valleyafter35319.iqsignIntelligentSign",
         attribute : "weburl",
         value : weburl };
   let s5 = { component: "main",
         capability: "valleyafter35319.iqsignIntelligentSign",
         attribute : "imageurl",
         value : imageurl };   
   let s6 = { component: "main",
         capability: "valleyafter35319.iqsignIntelligentSign",
         attribute : "sign",
         value : devinfo.lastsign };  
   let s7 = { component: "main",
         capability: "valleyafter35319.iqsignIntelligentSign",
         attribute : "display",
         value : dispname };  
   
   
   return [ s0,s2,s3,s4,s5,s6,s7 ];
}


async function handleSTCommand(token,resp,devices,body)
{
   console.log("ST COMMAND",token,devices,body);
   
   let usr = body.user;
   
   for (let dev of devices) {
      console.log("DEVICE",dev.externalDeviceId);
      let eid = dev.externalDeviceId;
      let did = await getSignId(eid,usr);
      let devresp = resp.addDevice(eid);
      for (let cmd of dev.commands) {
         let args = cmd["arguments"];
         console.log("COMMAND",cmd.command,args);
         let signdata = null;
         switch (cmd.command) {
            case 'chooseSign' :
               signdata = await handleChooseSign(did,usr,args);
               break;
            case 'setSign' :
               signdata = await handleSetSign(did,usr,args);
               break;
            default :
               console.log("ERR: COMMAND NOT SUPPORTED",cmd);
               break;
          }
         if (signdata != null) {
            let dname = await sign.getDisplayName(signdata);
            addCommandState(devresp,cmd,'sign',signdata.lastsign);
            addCommandState(devresp,cmd,'display',dname);
          }
       }
    }
}




async function handleSTIntegrationDeleted(token,data)
{
   console.log("ST INTEGRATION DELETED",token,data);
   // need to remove row from iQsignSignCodces
}



async function handleSTResult(token,data)
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
/*                                                                              */
/*      Command methods                                                         */
/*                                                                              */
/********************************************************************************/

async function handleChooseSign(did,usr,args)
{
   let cnts = "=" + args[0];
 
   let rows = await db.query("SELECT * FROM iQsignParameters WHERE defineid = $1 " +
         "ORDER BY index ASC",
         [ did ]);
   for (let i = 1; i < args.length; ++i) {
      let val = args[i];
      if (!val.contains("="))  {
         val = rows[i].name + '=' + val;
       }
      cnts += " " + val;
    }
   return await handleSetSign(did,usr,[ cnts ]);
}


async function handleSetSign(did,usr,args)
{
   let cnts = args[0];  
   let row = await db.query1("SELECT * FROM iQsignSigns WHERE id = $1 AND userid = $2",
         [ did, usr.id ]);
   await sign.changeSign(row,cnts);
   
   return row;
}


function addCommandState(devresp,cmd,att,val)
{
   let state = { 
         component : cmd.component, 
         capability : cmd.capability,
         attribute : att,
         value : val
    };
   devresp.addState(state);     
}



async function getSignCode(data)
{
   let row = await db.query01("SELECT * FROM iQsignSignCodes WHERE signid = $1",
         [ data.id ]);
   if (row != null) return row.code;
   
   let code = config.randomString(48);
   await db.query("INSERT INTO iQsignSignCodes (code,userid,signid,callback_url) " +
         "VALUES ( $1, $2, $3, $4 )",
         [ code,data.userid,data.id,"smartthings.com" ]);
   
   return code;
}


async function getSignId(code,user)
{
   if (code.startsWith("iQsign_")) {
      let did = code.substring(7);
      did = Number(did);
      return did;
    }   
   let row = await db.query1("SELECT * FROM iQsignSignCodes WHERE code = $1",
         [ code ]);
   if (user != null) {
      if (user.id != row.userid) throw "Illegal user for sign";
    }
   return row.signid;
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
exports.handleSmartInteraction = handleSmartInteraction;
exports.handleSmartThingsCommand = handleSmartThingsCommand;



/* end of module smartthings */
