/********************************************************************************/
/*										*/
/*		server.js							*/
/*										*/
/*	 Main server code of Catre External Device Environment Server		*/
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

const http = require('http');
const https = require('https');

const express = require('express');

const logger = require('morgan');
const bodyparser = require('body-parser');
const bearerToken = require('express-bearer-token');
const exphbs = require("express-handlebars");
const handlebars = exphbs.create( { defaultLayout : 'main'});

const config = require("./config");
const catre = require("./catre");
const generic = require("./generic");
const iqsign = require("./iqsign");
const smartthings = require("./smartthings");
const samsung = require("./samsung");
const alds = require("./alds");
const oauth = require("./oauth");



/********************************************************************************/
/*										*/
/*	Module storage								*/
/*										*/
/********************************************************************************/

const bearer_token = config.randomString(24);


/********************************************************************************/
/*										*/
/*	Express setup								*/
/*										*/
/********************************************************************************/

function setup()
{
    const app = express();
    
    app.engine('handlebars', handlebars.engine);
    app.set('view engine','handlebars');
    
    const iqsignrouter = iqsign.getRouter(express.Router());
    const genericrouter = generic.getRouter(express.Router());
    const smartthingsrouter = smartthings.getRouter(express.Router());
    const oauthrouter = oauth.getRouter(express.Router());
    const samsungrouter = samsung.getRouter(express.Router());
    const aldsrouter = alds.getRouter(express.Router());
    
    app.use(logger('combined'));

    app.use(bodyparser.urlencoded({ extended: true } ));
    app.use(bodyparser.json());
    app.use(bearerToken());
    
    app.all('/iqsign/*',iqsignrouter);
    app.all('/generic/*',genericrouter);
    app.all('/smartthings',smartthingsrouter);
    app.all('/smartthings/*',smartthingsrouter);
    app.all("/samsung/*",samsungrouter);
    app.all("/alds/*",aldsrouter);
    app.all('/oauth/*',oauthrouter);
    
    app.all("/catre/setup",handleSetup);

    app.all("/catre/*",authenticate);
    app.post("/catre/addbridge",addBridge);
    app.post("/catre/command",bridgeCommand);

    app.all('*',config.handle404);
    app.use(config.handleError);

    try {
	const httpsserver = https.createServer(config.getHttpsCredentials(),app);
	httpsserver.listen(config.HTTPS_PORT);
	console.log(`HTTPS Server listening on port ${config.HTTPS_PORT}`);
     }
    catch (error) {
	console.log("Did not launch https server",error);
     }
}



/********************************************************************************/
/*										*/
/*	Authenticate for general messages from CATRE server			*/
/*										*/
/********************************************************************************/

function authenticate(req,res,next)
{
   next();
}


/********************************************************************************/
/*										*/
/*	Catre commands								*/
/*										*/
/********************************************************************************/

/**
 *	addBridge { bridge: NAME, id: ID, authdata: {...} }
 **/

async function addBridge(req,res)
{
   console.log("CATRE ADD BRIDGE",req.body);

   let succ = false;

   switch (req.body.bridge) {
      case "generic" :
	 succ = generic.addBridge(req.body.authdata,req.body.bridgeid);
	 break;
      case "smarrthings" :
	 succ = smartthings.addBridge(req.body.authdata,req.body.bridgeid);
	 break;
      case "iqsign" :
	 succ = iqsign.addBridge(req.body.authdata,req.body.bridgeid);
	 break
      case "samsung" :
         succ = samsung.addBridge(req.body.authdata,req.body.bridgeid);
         break;
      default :
         console.log("NO SUCH BRIDGE: ",req.body.bridge);
	 config.handleFail(req,res,"No such bridge");
	 return;
    }

   if (succ) config.handleSuccess(req,res);
   else config.handleFail(req,res,"Bad authentication");
}



/**
 *	COMMAND { bridge: NAME, id: ID, uid: USER, command: {..} }
 **/

async function bridgeCommand(req,res)
{
   console.log("CATRE BRIDGE COMMAND",req.body);

   let succ = null;

   switch (req.body.bridge) {
      case "generic" :
	 succ = await generic.handleCommand(req.body.bridgeid,req.body.uid,req.body.deviceid,
               req.body.command,req.body.values);
	 break;
      case "smartthings" :
	 succ = await smartthings.handleCommand(req.body.bridgeid,req.body.uid,req.body.deviceid,
               req.body.command,req.body.values);
	 break;
      case "iqsign" :
	 succ = await iqsign.handleCommand(req.body.bridgeid,req.body.uid,req.body.deviceid,
               req.body.command,req.body.values);
	 break
      case "samsung" :
         succ = await samsung.handleCommand(req.body.bridgeid,req.body.uid,req.body.deviceid,
               req.body.command,req.body.values);
         break;
      case "alds" :
         succ = await alds.handleCommand(req.body.bridgeid,req.body.uid,req.body.deviceid,
               req.body.command,req.body.values);
         break;
      default :
	 config.handleFail(req,res,"No such bridge");
	 return;
    }

   config.handleSuccess(req,res,succ);
}



/********************************************************************************/
/*										*/
/*	Handle setup								*/
/*										*/
/********************************************************************************/

async function handleSetup(req,res)
{
   setupCatre();

   config.handleSuccess(req,res);
}


async function setupCatre()
{
   console.log("RECEIVED SETUP REQUEST FROM CATRE");
   await catre.sendToCatre({command: "INITIALIZE", auth: bearer_token });
}



/********************************************************************************/
/*										*/
/*	Main routine								*/
/*										*/
/********************************************************************************/

setup();

setupCatre();



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/



/* end of server.js */

