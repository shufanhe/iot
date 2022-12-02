/********************************************************************************/
/*										*/
/*		generic.js							*/
/*										*/
/*	Handle RESTful interface for generic devices and CATRE			*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
"use strict";




const config = require("./config");
const catre = require("./catre");



/********************************************************************************/
/*										*/
/*	Module storage								*/
/*										*/
/********************************************************************************/

let users = { };
let tokens = { };
let queues = { };


/********************************************************************************/
/*										*/
/*	Setup Router								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   restful.post("/generic/attach",handleAttach);
   restful.post("/generic/authorize",handleAuthorize);

   restful.use(authenticate);

   restful.post("/generic/devices",handleDevices);
   restful.post("/generic/ping",handlePing);
   restful.post("/generic/event",handleEvent);

   restful.all("*",config.handle404)
   restful.use(config.handleError);

   return restful;
}




/********************************************************************************/
/*										*/
/*	Authentication for generic devices					*/
/*										*/
/********************************************************************************/

function authenticate(req,res,next)
{
   console.log("GENERIC AUTHENTICATE",req.token,req.baseurl);
   
   let tok = req.token;
   if (tokens[tok] == null) config.handleFail(req,res,"Unauthorized");
   else {
      req.body.user = tokens[tok];
      next();
    }
}

/**
 *	ADDBRIDGE { authdata{ uid: user_id, pat: encoded PAT }, bridgeid: ID }
 **/

function addBridge(authdata,bid)
{
   console.log('GENERIC ADD BRIDGE',authdata.uid,authdata.pat);

   let uid = authdata.uid;
   let pat = authdata.pat;

   users[uid] = { uid: uid, seed: config.randomString(24), pat : pat, token: config.randomString(24), bridgeid: bid, needdevices: true };
   queues[uid] = [];

   return true;
}


/********************************************************************************/
/*										*/
/*	Request handlers							*/
/*										*/
/********************************************************************************/

/**
 *	ATTACH { uid : <user> }
 *	   -> { status: OK, seed: SEED }
 **/

function handleAttach(req,res)
{
   console.log("GENERIC ATTACH",req.body);

   let uid = req.body.uid;
   let seed = users[uid];
   if (seed == null) config.handleFail(req,res,"No such user",403);
   else config.handleSuccess({ status: "OK", seed: seed });
}


/**
 *	AUTHORIZE { uid: <user>, patencoded: H(H(H(pat) + uid) + seed)}
 *	   -> { status: OK, token : TOKEN}
 **/

function handleAuthorize(req,res)
{
   console.log("GENERIC AUTHORIZE",req.body);

   let patencode = req.body.patencoded;
   let uid = req.body.uid;
   let user = users[uid];
   if (user == null) config.handleFail(req,res,"No such user",403);
   else {
      let p1 = user.pat;
      let p2 = config.hasher(p1 + user.seed);
      if (p2 != patencode) {
	 config.handleFail(req,res,"Bad uid or password",403);
       }
      else {
	 let rslt = { status: "OK", token : user.token };
	 config.handleSuccess(req,res,rslt);
	 tokens[user.token] = user;
       }
    }
}



/**
 *	DEVICES { devices: [ {JSON} ] }
 **/

async function handleDevices(req,res)
{
   console.log("GENERIC DEVICES",req.body);

   let user = req.body.user;
   let msg = { command: "DEVICES", uid: user.uid,
	 bridge: "generic",
	 bid: user.bridgeid,
	 devices: req.body.devices };
   await catre.sendToCatre(msg);
   config.handleSuccess(req,res);
   user.needdevices = false;
}


/**
 *	PING { }
 *	   -> { status: OK } or { status: COMMAND, command : {..} } or { status: DEVICES }
 **/

function handlePing(req,res)
{
   console.log("GENERIC PING",req.body);

   let user = req.body.user;
   let c = queues[user.uid].shift();
   let rslt = null;
   if (c != null) {
      rslt = { status: "COMMAND", command: c };
    }
   else if (user.needdevices) {
      rslt = { status: "DEVICES" }
    }

   config.handleSucces(req,res,rslt);
}


/**
 *	EVENT { event: <json> }
 **/

async function handleEvent(req,res)
{
   console.log("GENERIC EVENT",req.body);

   let user = req.body.user;
   let event = req.body.event;
   let msg = { command: "EVENT", uid : user.uid, bridge: "generic",
	 bid: user.bridgeid,
	 event: event };
   await catre.sendToCatre(msg);
   config.handleSuccess(req,res);
}



function handleCommand(bid,uid,command)
{
   let x = queues[uid];
   if (x == null) queues[uid] = [];
   queues[uid].push(command);
}



function handleSetup(req,res)
{
   // server restarted -- handle any cleanup/setup here
   
   config.handleSuccess(req,res);
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;



/* end of generic.js */

