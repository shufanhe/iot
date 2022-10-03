/********************************************************************************/
/*                                                                              */
/*              rest.js                                                         */
/*                                                                              */
/*      Handle RESTful interface for iQsign                                     */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/
"use strict";


const uuid = require('node-uuid');

const db = require("./database");
const config = require("./config");
const auth = require("./auth");



/********************************************************************************/
/*                                                                              */
/*      Restful session management                                              */
/*                                                                              */
/********************************************************************************/

async function session(req,res,next)
{
   console.log("SESSION",req.session,req.query,req.body);
   
   if (req.query != null && req.body == null) req.body = req.query;
   if (req.session != null) return;
   
   let row = await db.query01("SELECT * FROM iQsignRestful WHERE session = $1",
        [ req.body.session]);
   // should check for timeout
   if (row == null) {
      let session = uuid.v1();
      let code = config.randomString(32);
      await db.query("INSERT INTO iQsignRestful (session,code) VALUES ($1,$2)",
         [ session, code ]);
      row = { session : session, userid : null, signid : null, code: code };
    }
   
   req.session = row;
}


async function updateSession(req)
{
   await db.query("UPDATE iQsignRestful (userid, signid, code, last_used) " +
      "VALUES ( $1, $2, $3, DEFAULT ) WHERE session = $4",
      [req.session.userid,req.session.signid,req.session.code,req.session.session]);
}




async function authenticate(req,res,next)
{
   if (req.session.user == null) {
      throw "Unauthorized";
    }
   else {
      await updateSession(req);
      next();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle login/register                                                   */
/*                                                                              */
/********************************************************************************/

async function handlePrelogin(req,res)
{
   console.log("REST PRE LOGIN");
   
   let rslt = {
      session : req.session.session,
      code : req.session.code
    }
   
   res.end(JSON.stringify(rslt));
}


async function handleLogin(req,res)
{
   console.log("REST LOGIN");
   
   req.session.userid = null;
   
   await auth.handleLogin(req,res,true);
   
   if (req.session.user != null) {
      req.session.userid = req.user.id;
    }
   updateSession();
}


async function handleRegister(req,res)
{
   console.log("REST REGISTER");
   
   await auth.handleRegister(req,res,true);
}



/********************************************************************************/
/*                                                                              */
/*      Handle sign requests                                                    */
/*                                                                              */
/********************************************************************************/

async function handleGetAllSigns(req,res)
{
   console.log("REST LIST SIGNS")
}


async function handleGetSignData(req,res)
{
   console.log("REST GET SIGN DATA");
}



async function handleUpdateSignData(req,res)
{
 console.log("REST UPDATE SIGN DATA");
}


async function handleDeleteSign(req,res)
{
   console.log("REST DELETE SIGN");
}

async function handleUpdateSign(req,res)
{
   console.log("REST UPDATE SIGN");
}


async function handleSetSign(req,res)
{
   console.log("REST SET SIGN");
}


async function handleGetAllSavedSigns(req,res)
{
   console.log("REST GET ALL SAVED SIGNS");
}


async function handleGetImage(req,res)
{
   console.log("REST GET IMAGE");
}


async function handleUpdateImage(req,res)
{
   console.log("REST UPDATE IMAGE");
}


/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.session = session;
exports.authenticate = authenticate;
exports.handlePrelogin = handlePrelogin;
exports.handleLogin = handleLogin;
exports.handleRegister = handleRegister;
exports.handleGetAllSigns = handleGetAllSigns;
exports.handleGetSignData = handleGetSignData;
exports.handleUpdateSignData = handleUpdateSignData;
exports.handleDeleteSign = handleDeleteSign;
exports.handleUpdateSign = handleUpdateSign;
exports.handleSetSign = handleSetSign;
exports.handleGetAllSavedSigns = handleGetAllSavedSigns;
exports.handleGetImage = handleGetImage;
exports.handleUpdateImage = handleUpdateImage;





/* end of module rest */
