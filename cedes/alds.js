/********************************************************************************/
/*                                                                              */
/*              alds.js                                                         */
/*                                                                              */
/*      Interface to ALDS location detector                                     */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/

"use strict";

const fs = require('fs');
const config = require("./config");
const catre = require("./catre");


/********************************************************************************/
/*                                                                              */
/*      Local storage                                                           */
/*                                                                              */
/********************************************************************************/

var users = { };
var tokens = { };
var log_stream = null;
var data_stream = null;


/********************************************************************************/
/*                                                                              */
/*      Handle routing                                                          */
/*                                                                              */
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
/*                                                                              */
/*      Authentication for ALDS devices                                         */
/*                                                                              */
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
 *      ADDBRIDGE { authdata{ uid: user_id, pat: encoded PAT }, bridgeid: ID }
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
/*                                                                              */
/*      Attach and authorize                                                    */
/*                                                                              */
/********************************************************************************/

/**
 *      ATTACH { uid : <user> }
 *         -> { status: OK, seed: SEED }
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
 *      AUTHORIZE { uid: <user>, patencoded: H(H(H(pat) + uid) + seed)}
 *         -> { status: OK, token : TOKEN}
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
/*                                                                              */
/*      Handle raw data for testing purposes                                    */
/*                                                                              */
/********************************************************************************/

function handleRawData(req,res)
{
   console.log("ALDS DATA",req.body.aldsdata,JSON.stringify(req.body.aldsdata,null,3));

   if (log_stream == null) {
      log_stream = fs.createWriteStream('alds.log');
      data_stream = fs.createWriteStream('aldsdata.json',{flags: 'a'});
    }

   let data = req.body.aldsdata;
   let adata = JSON.parse(data);
   data = JSON.stringify(adata,null,3);

   if (data != null){
      let typ = adata["type"];
      if (typ == 'LOG') {
         log_stream.write(adata["message"] + "\n");
       }
      else if (typ == 'DATA') {
         data_stream.write(data + "\n");
       }
      else console.log("UNKNOWN TYPE",typ,data);
    }

   config.handleSuccess(req,res);
}


/********************************************************************************/
/*                                                                              */
/*      Command and event handling                                              */
/*                                                                              */
/********************************************************************************/

async function handleCommand(bid,uid,devid,command,values)
{
   // no commands
}


async function handleParameters(bid,uid,devid,parameters)
{
   // not needed
}



/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;
exports.handleParameters = handleParameters;

/* end of module alds */
