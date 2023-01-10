/********************************************************************************/
/*										*/
/*		alds.js 							*/
/*										*/
/*	Interface to ALDS location detector					*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
"use strict";

const fs = require('fs');
const config = require("./config");
const catre = require("./catre");


/********************************************************************************/
/*										*/
/*	Local storage								*/
/*										*/
/********************************************************************************/

var users = { };
var tokens = { };
var log_stream = null;
var data_stream = null;


/********************************************************************************/
/*										*/
/*	Handle routing								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   restful.post("/alds/attach",handleAttach);
   restful.post("/alds/authorize",handleAuthorize);
   restful.post("/alds/data",handleRawData);

   restful.all("*",config.handle404);
   restful.use(config.handleError);

   return restful;
}


/********************************************************************************/
/*										*/
/*	Authentication for ALDS devices 					*/
/*										*/
/********************************************************************************/

function authenticate(req,res,next)
{
   console.log("ALDS AUTHENTICATE",req.token,req.baseurl);

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
   console.log('ALDS ADD BRIDGE',authdata.uid,authdata.pat);

   let uid = authdata.uid;
   let pat = authdata.pat;

   users[uid] = { uid: uid,
	 seed: config.randomString(24),
	 pat : pat,
	 token: config.randomString(24),
	 bridgeid: bid };

   return true;
}



/********************************************************************************/
/*										*/
/*	Attach and authorize							*/
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
   let seed = users[uid].seed;
   if (seed == null) config.handleFail(req,res,"No such user",403);
   else config.handleSuccess(req,res,{ status: "OK", seed: seed });
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
	 user.needdevices = true;
       }
    }
}



/********************************************************************************/
/*										*/
/*	Handle raw data for testing purposes					*/
/*										*/
/********************************************************************************/

function handleRawData(req,res)
{
   console.log("ALDS DATA",JSON.stringify(req.body,null,3));

   if (log_stream == null) {
      log_stream = fs.createWriteStream('aldslog.json');
      data_stream = fs.createWriteStream('aldsraw.json',{flags: 'a'});
    }

   let data = JSON.stringify(req.body.aldsdata,null,3);

   if (data != null){
      let typ = req.body.aldsdata.type;
      if (typ == 'LOG') log_stream.write(data + "\n");
      else if (typ == 'DATA') data_stream.write(data + "\n");
      console.log("UNKNOWN TYPE",typ,data,log_stream);
    }

   config.handleSuccess(req,res);
}


/********************************************************************************/
/*										*/
/*	Command and event handling						*/
/*										*/
/********************************************************************************/

async function handleCommand(bid,uid,devid,command,values)
{

}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;

/* end of module alds */
