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
const sign = require("./sign");


/********************************************************************************/
/*                                                                              */
/*      Setup Router                                                            */
/*                                                                              */
/********************************************************************************/

function restRouter(restful)
{
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
   restful.get("/rest/namedsigns",handleGetAllSavedSigns);
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
   console.log("REST SESSION",req.session,req.query,req.body,req.params);
   if (req.session == null) {
      let sid = (req.body ? req.body.session : null);
      if (sid == null && req.query) sid = req.query.session;
      if (sid != null) {
         let row = await db.query01("SELECT * from iQsignRestful WHERE session = $1",
               [ sid ]);
         if (row != null) req.session = row;
         else sid = null;
       }
      if (sid == null) {
         sid = uuid.v1();
         let code = config.randomString(32);
         await db.query("INSERT INTO iQsignRestful (session,code) " + 
               "VALUES ($1,$2)",
               [ sid,code ]);
         req.session = { 
               session : sid,
               userid : null,
               code : code,
          };
       }
    }
   req.session.uuid = req.session.session;
   
   next();
}


async function updateSession(req)
{
   console.log("REST UPDATE SESSION",req.session);
   if (req.session != null) {
      await db.query("UPDATE iQsignRestful "+
            "SET userid = $1, last_used = CURRENT_TIMESTAMP " +
            "WHERE session = $2",
            [req.session.userid,req.session.session]);
    }
}




async function authenticate(req,res,next)
{
   console.log("REST AUTH",req.session,req.body,req.query);
   
   if (req.session.userid == null) {
      let rslt = { status: "ERROR", message: "Unauthorized" };
      res.status(400);
      res.json(rslt);
    }
   else {
      await updateSession(req);
      if (req.session.user == null) {
         let row = await db.query1("SELECT * FROM iQsignUsers WHERE id = $1",
               [req.session.userid]);
         row.password = null;
         row.altpassword = null;
         req.session.user = row;
         req.user = row;
       }
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
   let rslt = {
      session : req.session.session,
      code : req.session.code
    }
   
   console.log("REST PRE LOGIN",req.session,rslt);
   
   res.end(JSON.stringify(rslt));
}


async function handleLogin(req,res)
{
   console.log("REST LOGIN",req.session);
   
   req.session.userid = null;
   
   await auth.handleLogin(req,res,true);
   
   if (req.session.user != null) {
      req.session.userid = req.session.user.id;
    }
   await updateSession(req);
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
   console.log("REST LIST SIGNS",req.session);
   
   let rows = db.query("SELECT * FROM iQsignSigns WHERE userid = $1",
         [ req.session.userid ]);
   console.log("SIGN LIST ",rows);
   let data = [];
   for (let i = 0; i < rows.length; ++i) {
      let row = rows[i];
      let dname = await sign.getDisplayName(row);
      let wurl = sign.getWebUrl(row.namekey);
      let iurl = sign.getImageUrl(row.namekey);
      let sd = { 
         name : row.name,
         displayname : dname,
         width : row.width,
         height : row.height,
         namekey : row.namekey,
         dim : row.dimension,
         signurl : wurl,
         imageurl : iurl,
         signbody : row.lastsign,
         interval: row.interval,
         signid : row.id,
       };
      data.push(sd);
    }
   let rslt = { status: "OK", data: data };
   res.status(200);
   res.json(rslt);
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
   console.log("REST GET ALL SAVED SIGNS",req.session);
   
   let q = "SELECT * FROM iqSignDefines D " +
         "LEFT OUTER JOIN iQsignUseCounts C ON D.id = C.defineid " +
         "WHERE D.userid = $1 OR D.userid IS NULL " +
         "ORDER BY C.count DESC, C.last_used DESC, D.id";
   let rows = await db.query(q,[req.session.userid]);
 
   let data = { };
   for (let row of rows) {
      let dname = await sign.getDisplayName(row);
      let wurl = sign.getWebUrl(row.namekey);
      let iurl = sign.getImageUrl(row.namekey);
      let sd = { 
            name : row.name,
            contents : row.contents,
            defid : row.id,
            lastupdate : row.lastupdate,
       };
      data.push(sd);
    }
   
   let rslt = { status: "OK", data: data };
   res.status(200);
   res.json(rslt);
}


function defSort(d1,d2)
{
   return d1.lastupdate.getTime() - d2.lastupdate.getTime();
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
