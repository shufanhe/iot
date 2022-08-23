/********************************************************************************/
/*                                                                              */
/*              smartthings.js                                                  */
/*                                                                              */
/*      Server for smartthing interface                                         */
/*                                                                              */
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
        partnerHelper, CommandResponse } = require('st-schema');
const stPartnerHelper = new partnerHelper( {}, {} );

const util = require('util');

const config = require("./config");
const connector = require("./connector");



/********************************************************************************/
/*                                                                              */
/*      Handle requests                                                         */
/*                                                                              */
/********************************************************************************/

async function handleSmartThings(req,res)
{
   console.log("HANDLE SMART THINGS",req.body,req.header('Authorization'),req.path,req.query);
   
   if (req.body.lifecycle == 'CONFIRMATION') {
      let rslt = { };
      res.end(JSON.stringify(rslt));
    }

}



/********************************************************************************/
/*                                                                              */
/*      Express setup                                                           */
/*                                                                              */
/********************************************************************************/

function setup()
{
   const app = express();
   
   app.use(logger('combined'));
   
   app.use(favicons(__dirname + config.STATIC));
   
   app.use(bodyparser.json({ type: "application/json" })); 
   
   app.post('/',handleRequest);
   app.post('/command',handleCommand);
   app.get('/',connector.handleDiscovery);    
   app.use(errorHandler);
}


/********************************************************************************/
/*                                                                              */
/*      Basic handlers                                                          */
/*                                                                              */
/********************************************************************************/

function handleRequest(req,res)
{
   if (accessTokenIsValid(req,res)) {
      connector.handleHttpCallback(req, res)
    }
   else {
      res.status(401).send('Unauthorized');
    }
}

function handleCommand(req,res)
{
   console.log("COMMAND",req.params,req.body);
   
   if (accessTokenIsValid(req,res)) {
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
//       component: 'main',
//          capability: req.body.attribute === 'level' ? 'st.switchLevel' : 'st.switch',
//                attribute: req.body.attribute,
//                value: req.body.value
//     } ] } ];
//    updatereq.updateState(item.callbackUrls,item.callbackAuthentication,devicestate);
      res.send({});
      res.end();
    }
   
}


function accessTokenIsValid(req,res) {
   // Replace with proper validation of issued access token
   if (req.body.authentication && req.body.authentication.token) {
      return true;
    }
   return false;
}


async function validateToken(req,res,next) 
{
   if (req.body.authenticate && req.body.authenticate.token) {
      
    }
   
   res.status(401).send('Unauthorized');
}



/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.handleSmartThings = handleSmartThings;


/* end of module smartthings */
