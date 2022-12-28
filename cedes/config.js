/********************************************************************************/
/*										*/
/*		config.js							*/
/*										*/
/*	Constants cor Catre External Device Environment Server			*/
/*										*/
/*										*/
/********************************************************************************/
"use strict";


/********************************************************************************/
/*										*/
/*	Imports 								*/
/*										*/
/********************************************************************************/

const fs = require('fs');
const crypto = require('crypto');



/********************************************************************************/
/*										*/
/*	Connection constants							*/
/*										*/
/********************************************************************************/

const HTTPS_PORT = 3333;

const SESSION_KEY = "cedes-Catre-9467";
const OAUTH_SESSION_KEY = "iot-cedes-oauth-9467";
const APP_SESSION_KEY = "iot-cedes-app-9467";

const SERVER_KEY_FILE = "serverkey";
const SERVER_CERT_FILE = "servercert";
const SMARTTHINGS_FILE = "cedes-st.json";

const OAUTH_FILE = "stoauthtokens";

const PASSWORD_DIR = __dirname + "/../secret/";
const RESOURCE_DIR = __dirname + "/../resources/";

const SOCKET_PORT = 3661;


function getHttpsCredentials()
{
    let keydata = fs.readFileSync(PASSWORD_DIR + SERVER_KEY_FILE,'utf8');
    let certdata = fs.readFileSync(PASSWORD_DIR + SERVER_CERT_FILE,'utf8');
    const creds = {
	  key : keydata,
	  cert: certdata,
     };
    return creds;
}


function getOauthCredentials()
{
   let data = fs.readFileSync(PASSWORD_DIR + OAUTH_FILE,'utf8');
   data = data.toString().trim();
   return JSON.parse(data);
}


function getSmartThingsCredentials()
{
   let data = fs.readFileSync(PASSWORD_DIR + SMARTTHINGS_FILE,'utf8');
   data = data.toString().trim();
   return JSON.parse(data);
}


/********************************************************************************/
/*										*/
/*	Utility functions							*/
/*										*/
/********************************************************************************/

function randomString(length = 48,chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789' )
{
   // Pick characers randomly
   let str = '';
   for (let i = 0; i < length; i++) {
      str += chars.charAt(Math.floor(Math.random() * chars.length));
    }
   return str;
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
/*	Output methods								*/
/*										*/
/********************************************************************************/

function handle404(req,res)
{
   res.status(404);
   let rslt = { status : 'ERROR', reason: 'Invalid URL'} ;
   res.end(JSON.stringify(rslt));
}



function handleError(err,req,res,next)
{
   console.log("ERROR on request %s %s %s",req.method,req.url,err);
   console.log("STACK",err.stack);

   res.status(500);
   let msg = 'Server Error';
   let rslt = { status : 'ERROR' ,reason: msg} ;
   res.end(JSON.stringify(rslt));
}


function handleFail(req,res,msg,sts)
{
   if (sts == null) sts = 200;
   if (msg == null) msg = "Error";
   
   res.status(sts);
   let rslt = { status: 'ERROR', reason: msg };
   res.end(JSON.stringify(rslt));
}


function handleSuccess(req,res,rslt)
{
   if (rslt == null) rslt = { };
   
   res.status(200);
   if (rslt['status'] == undefined) rslt.status = "OK";
   
   res.end(JSON.stringify(rslt));
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.HTTPS_PORT = HTTPS_PORT;
exports.SESSION_KEY = SESSION_KEY;
exports.OAUTH_SESSION_KEY = OAUTH_SESSION_KEY;
exports.APP_SESSION_KEY = APP_SESSION_KEY;
exports.SOCKET_PORT = SOCKET_PORT;

exports.randomString = randomString;
exports.getHttpsCredentials = getHttpsCredentials;
exports.getOauthCredentials = getOauthCredentials;
exports.getSmartThingsCredentials = getSmartThingsCredentials;
exports.hasher = hasher;

exports.handle404 = handle404;
exports.handleError = handleError;
exports.handleFail = handleFail;
exports.handleSuccess = handleSuccess;




/* end of module config */

