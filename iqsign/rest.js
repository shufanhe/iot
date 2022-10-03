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
/*      Setup Router                                                            */
/*                                                                              */
/********************************************************************************/

function restRouter(restful)
{
   console.log("RESTFUL");
   restful.use(session);
   restful.get('/rest/login',handlePrelogin);
   restful.post('/rest/login',handleLogin);
   restful.post("/rest/register",handleRegister);
   restful.use(authenticate);
   restful.get("/rest/signs",handleGetAllSigns);
   restful.get("/rest/sign/:signid",handleGetSignData);
   restful.put("/rest/sign/:signid",handleUpdateSignData);    
   restful.delete("/rest/sign/:signid",handleDeleteSign);
   restful.post("/rest/update/:signid",handleUpdateSign);    
   restful.post("/rest/setsign/:signid/:imageid",handleSetSign);
   restful.get("/rest/segsign",handleGetAllSavedSigns);
   restful.get("/rest/image/:imageid",handleGetImage);
   restful.post("/rest/image/:imageid",handleUpdateImage);
   restful.all("*",badUrl);
   restful.use(errorHandler);
   
   return restful;
}



function badUrl(req,res)
{
   res.status(404);
   let rslt = { status : 'ERROR', reason: 'Invalid URL'} ;
   res.end(JSON.stringify(rslt));
}


function errorHandler(err,req,res,next)
{
   console.log("ERROR on request %s %s %s",req.method,req.url,err);
   console.log("STACK",err.stack);
   res.status(500);
   
   res.end();
}



/********************************************************************************/
/*                                                                              */
/*      Restful session management                                              */
/*                                                                              */
/********************************************************************************/

async function session(req,res,next)
{
   console.log("REST SESSION",req.session,req.query,req.body);
   
   next();
}


async function updateSession(req)
{
   req.session.touch();
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
   
   if (req.session.code == null) {
      req.session.code = config.randomString(32);
      req.session.touch();
    }
   
   let rslt = {
      session : req.session.uuid,
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

exports.restRouter = restRouter;

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
