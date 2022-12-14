/********************************************************************************/
/*										*/
/*		iqsign.js							*/
/*										*/
/*	Handle RESTful interface for IQSIGN and CATRE				*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
"use strict";


const fetch = require("node-fetch");
const crypto = require('crypto');

const config = require("./config");
const catre = require("./catre");



/********************************************************************************/
/*										*/
/*	Global storage								*/
/*										*/
/********************************************************************************/

var users = { };

let iqsign_url = "https://sherpa.cs.brown.edu:3336/rest/";


/********************************************************************************/
/*										*/
/*	Setup Router								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   restful.use(authenticate);

   restful.all("*",config.handle404)
   restful.use(config.handleError);

   setInterval(periodicChecker,10*60*1000);

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



async function addBridge(authdata,bid)
{
   console.log("IQSIGN ADD BRIDGE",authdata.username,authdata.token);

   let username = authdata.username;
   let pat = authdata.token;

   let user = users[username];
   if (user == null) {
      user = { username : username, session: null, bridgeid: bid, devices : [], saved : [] };
      users[username] = user;
    }

   let login = { username : username, accesstoken : pat };
   let resp1 = await sendToIQsign("POST","login",login);
   if (resp1.status != 'OK') return false;
   user.session = resp1.session;

   getDevices(user);

   return false;
}


async function getDevices(user)
{
   getSavedSigns(user);

   let resp = await sendToIQsign("POST","signs",{ session : user.session });
   if (resp.status != 'OK') return;

   let update = false;

   for (let newdev of resp.data) {
      let signid = newdev.signid;
      let fdev = null;
      for (let dev of user.devices) {
	 if (dev.signid == signid) {
	    fdev = dev;
	    break;
	  }
       }
      if (fdev == null) {
	 let catdev = {
	       UID : fdev.signid,
	       BRIDGE : "iqsign",
	       NAME : fdev.name,
	       PARAMETERS :  [
	       { NAME : "currentSign", TYPE: "STRING", ISSENSOR : false },
	       ],
	       TRANSITIONS: [
	       { NAME : "setSign",
		  PARAMETERS : [
		  { NAME: "setTo", TYPE: "ENUM", VALUES: user.saved }
		  ]
		}
	       ]
	  }
	 user.devices.push(catdev);
	 update = true;
	 // note that devices need updating or send devices to CATRE
       }
    }

   if (update) {
      let msg = { command: "DEVICES", uid : user.uid, bridge: "iqsign",
		  bid : user.bridgeid, devices : user.devices };
      await catre.sendToCatre(msg);
    }
}


async function getSavedSigns(user)
{
   let resp = await sendToIQsign("POST","namedsigns",{ session : user.session });
   if (resp.status != 'OK') return;
   user.saved = resp.data;
}



/********************************************************************************/
/*										*/
/*	Command handling							*/
/*										*/
/********************************************************************************/

async function handleCommand(bid,uid,deHid,command,values)
{
   let user = users[uid];
   if (user == null) return;

   for (let dev of user.devices) {
      if (dev.UID == devid) {
	  switch (command) {
	     case "setSign" :
		await sendToIQsign("POST","/sign/setto",{
		   session: user.session, signid: dev.UID, value: values.setTo});
		break;
	   }
	  break;
       }
    }
}



/********************************************************************************/
/*										*/
/*	Periodic checker to keep up to date					*/
/*										*/
/********************************************************************************/

async function periodicChecker()
{
   for (let uid in users) {
      let user = users[uid];
      getDevices(user);
    }
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

async function sendToIQsign(method,path,data)
{
   let url = iqsign_url + path;
   let body = null;
   let hdrs = { 'Accept' : "application/json" };
   if (data != null && method != 'GET') {
      hdrs['Content-Type'] = 'application/json';
      body = JSON.stringify(data);
    }
   else if (data != null && method == 'GET') {
      let sep = "?";
      for (let k in data) {
	 url += sep + k + "=" + data[k];
	 sep = "&";
       }
    }

   let response = await fetch(url, {
	 method: method,
	 body : body,
	 headers: hdrs });
   let rslt = await response.json();

   return rslt;
}



function hasher(msg)
{
   const hash = crypto.createHash('sha512');
   const data = hash.update(msg,'utf-8');
   const gen = data.digest('base64');
   return gen;
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;




/* end of iqsign.js */
