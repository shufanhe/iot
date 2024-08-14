/********************************************************************************/
/*                                                                              */
/*              oauth.js                                                        */
/*                                                                              */
/*      Oauth authentication using catre credentials                            */
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
   
const oauthserver = require('express-oauth-server');
   
const config = require("./config");
const catre = require("./catre");
   

/********************************************************************************/
/*                                                                              */
/*      Module storage                                                          */
/*                                                                              */
/********************************************************************************/


const oauthconfig = config.getOauthCredentials();

const CLIENTS = { };

CLIENTS["testing"] =  {
      client : "Testing",
      secret : "XXXXXX",
      redirects: [ "https://localhost:3336/default",
      "http://localhost:3335/default",
      "https://sherpa.cs.brown.edu:3336/default" ],
      grants: [ "authorization_code", "device_code",
      "refresh_token", "code"  ],
};


CLIENTS[oauthconfig.id]  = {
      client : "SmartThings",
      secret : oauthconfig.secret,
      redirects : [ "https://c2c-us.smartthings.com/oauth/callback",
      "https://c2c-eu.smartthings.com/oauth/callback",
      "https://c2c-ap.smartthings.com/oauth/callback",
      "https://oauthdebugger.com/debug" ],
      grants: [ "authorization_code", "device_code",
      "refresh_token",  "code" ],
};



/********************************************************************************/
/*                                                                              */
/*      Setup Router                                                            */
/*                                                                              */
/********************************************************************************/

function getRouter(restful)
{
   restful.all("/oauth/token",handleAuthorizeToken);
   restful.get("/oauth/authorize",handleAuthorizeGet);
   restful.post("/oauth/authorize",handleAuthorizePost);
   restful.get("/oauth/login",handleCatreLoginGet);
   restful.post("/oauth/login",handleCatreLogin);
   
   restful.all("*",config.handle404);
   restful.use(config.handleError);
   
   return restful;
}




/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/


async function handleAuthorizeToken(req,res)
{
   let oauth = req.app.oauth;
   
   console.log("AUTHORIZE TOKEN",req.query,req.body,oauth);
   
   let opts = { };
   
   let fct = oauth.token(req,res,opts);
   
   if (req.session) {
      await req.session.destroy();
    }
   else {
      console.log("CHECK SESSION",req);
    }
   
   let tok1 = await fct(req,res);
   
   console.log("TOKEN DONE",tok1,res._header,res);
}


/********************************************************************************/
/*                                                                              */
/*      Handle authorize requests                                               */
/*                                                                              */
/********************************************************************************/


async function handleAuthorizePost(req,res)
{
   console.log("POST AUTHORIZE",req.path,req.query,req.body,req.app.locals);
   req.query = req.body;
   return handleAuthorizeGet(req,res);
}


async function handleAuthorizeGet(req,res)
{
   console.log("AUTHORIZE",req.originalUrl);
   
   console.log("GET AUTHORIZE",req.path,req.query,req.body,req.app.locals.user,req.session);
   
   let user = req.app.locals.user;
   
   if (!user) {
      let cinfo = await omodel.getClient(req.query.client_id,null);
      console.log("CINFO",cinfo);
      let who = "client_id=" + req.query.client_id;
      who += "&response_type=" + req.query.response_type;
      if (cinfo != null) who += "&client=" + cinfo.client;
      let redir = req.path;
      redir += "?client_id=" + req.query.client_id;
      redir +=  "&redirect_uri=" + req.query.redirect_uri;
      redir += "&response_type=" + req.query.response_type;
      redir += "&scope=" + req.query.scope;
      redir += "&state=" + req.query.state;
      redir = encodeURIComponent(redir);
      let rslt = '/oauth/login?' + who + '&redirect=' + redir;
      console.log("AUTHORIZE TO " + rslt);
      return res.redirect(rslt);
    }
   
   if (!user.valid) req.query.allowed = 'false';
   
   let oauthcode = req.app.oauth;
   req.body = req.query;
   
   req.app.locals.user = null;
   if (req.session) {
      await req.session.destroy();
    }
   else {
      console.log("CHECK SESSION",req);
    }
   
   console.log("PRESEND",res._header);
   
   let opts = { model : omodel,
         authenticateHandler : authorizeAuthenticator(user) };
   let x = catreoauth.authorize( opts );
   let x1 = await x(req,res);
   
   console.log("AUTHORIZE DONE",user,res._header);
}


function authorizeAuthenticator(user)
{
   return { handle :
      function(req,res) {
         return { id : user.id,
            username : user.username,
            signid : user.signid };
       }
    }
}


function handleCatreLoginGet(req,res)
{ 
   return catre.displayCatreLoginPage(req,res);
}




/********************************************************************************/
/*                                                                              */
/*      CatreModel                                                              */
/*                                                                              */
/********************************************************************************/

class CatreModel {

   constructor() { }

   async getAccessToken(bearertoken) {
      console.log("OAUTH GETTOKEN",bearertoken);
      
      let msg = { command: "OAUTH_GETTOKEN",token: bearertoken };
      let rslt = await catre.sendToCatre(msg);
      
      return rslt;
    }
   
   async getClient(clientid,clientsecret) {
      console.log("OAUTH GETCLIENT",clientid,clientsecret);
      
      let cinfo = CLIENTS[clientid];
      if (cinfo == null) return;
      if (clientsecret != null && cinfo.secret != clientsecret) return;
      
      let rslt = {
            id : clientid,
            client : cinfo.client,
            redirectUris : cinfo.redirects,
            grants : cinfo.grants
       }
      
      return rslt;
    }
   
   async saveAccessToken(token,client,user) {
      return handleSaveToken(token,client,user);
    }
   
   async saveToken(token,client,user) {
      return handleSaveToken(token,client,user);
    }
   
   async revokeToken(token) {
      console.log("OAUTH REVOKETOKEN",token);
      
      let msg = { command: "OAUTH_REVOKETOKEN", token: token };
      let rslt = await catre.sendToCatre(msg);
      
      return true;
    }
   
   async saveAuthorizationCode(code,client,user) {
      console.log("OAUTH SAVEAUTHORIZATIONCODE",code,client,user);
      
      let msg = { command: "OAUTH_SAVECODE", code: code, client: client, user: user};
      let rslt = await catre.sendToCatre(msg);
            
      return rslt;
    }
   
   async getAuthorizationCode(code) {
      console.log("OAUTH GETAUTHORIZATIONCODE",code);
      
      let msg = { command: "OAUTH_GETCODE", code: code };
      let rslt = await catre.sendToCatre(msg);
      
      return rslt;
    }
   
   async revokeAuthorizationCode(code) {
      console.log("OAUTH REVOKEAUTHORIZATIONCODE",code);
      
      let msg = { command: "OAUTH_REVOKE", code: code };
      let rslt = await catre.sendToCatre(msg);
      
      console.log("OAUTH REVOKE CHECK",rslt);
      
      return true;
    }
   
   async getRefreshToken(bearertoken) {
      console.log("OAUTH GETREFRESH",bearertoken);
      
      let msg = { command: "OAUTH_GETREFRESH", token : bearertoken };
      let rslt = await catre.sendToCatre(msg);
      
      return rslt;
    }
   
   async verifyScope(token,scope) {
      console.log("OAUTH VERIFYSCOPE",token,scope);
      
      let msg = { command: "OAUTH_VERIFYSCOPE", token : token, scope: scope };
      let rslt = await catre.sendToCatre(msg);
      
      return true;
    }
   
}       // end of class ModelCatre



async function handleSaveToken(token,client,user) 
{
   console.log("OAUTH SAVETOKEN",token,client,user);
   
   let msg = { command: "OAUTH_SAVETOKEN", token: token, client: client, user: user };
   let rslt = await catre.sendToCatre(msg);
   
   return rslt;
}





/********************************************************************************/
/*                                                                              */
/*      Login methods                                                           */
/*                                                                              */
/********************************************************************************/


function displayCatreLoginPage(req,res)
{
   console.log("CATRE LOGIN",req.query);
   
   let code = config.randomString(32);
   if (req.session != null) {
      if (req.session.code == null) {
         req.session.code = config.randomString(32);
       }
      code = req.session.code;
    }
   let rdir = req.query.redirect || "/catre/authorize";
   
   let data = {
         padding : code,
         redirect : rdir,
         client_id : req.query.client_id,
         client_name : req.query.client,
         response_type : req.query.response_type
    };
   
   console.log("OAUTH DATA",data);
   
   res.render('oauthlogin',data);
}




async function handleCatreLogin(req,res)
{ 
   let msg = { command: "OAUTH_LOGIN", username: req.body.username,
         accesstoken : req.body.accesstoken, 
         password : req.body.password,
         padding : req.body.padding
    };
   let rslt = await catre.sendToCatre(msg); 
   
   res.end(JSON.stringify(rslt));
}



/******node **************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

const omodel = new CatreModel();
const catreoauth = new oauthserver( {
   model: omodel,
   debug: true
});



/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.getRouter = getRouter;



/* end of module oauth */
