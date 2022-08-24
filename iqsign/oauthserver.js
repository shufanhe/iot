/********************************************************************************/
/*										*/
/*		oauthserver.js							*/
/*										*/
/*	Oauth server using common dbms						*/
/*										*/
/*	Written by spr, based on node-oauth2-server				*/
/*										*/
/********************************************************************************/
"use strict";


const http = require('http');
const https = require('https');

const util = require('util');

const xmlbuilder = require("xmlbuilder");

const config = require("./config");
const oauthmodel = require('./modelps').Model;
const model = new oauthmodel();
const auth = require("./auth");
const db = require("./database");

const creds = config.getOauthCredentials();

		
		
/********************************************************************************/
/*										*/
/*	Action functions							*/
/*										*/
/********************************************************************************/

async function handleAuthorizeToken(req,res)
{
   let app = req.app;
   
   console.log("AUTHORIZE TOKEN",req.query,req.body,app.oauth);

   let opts = { };
   let tok = await app.oauth.token(req,res,opts);
   
   console.log("TOKEN",tok);
   res.locals.oauth = { token : tok };
   
   
   let fct = app.oauth.token(req,res);
   let tok1 = fct(req,res);   
   
   console.log("TOKEN1",tok1);
}



/********************************************************************************/
/*                                                                              */
/*      Handle authorize requests                                               */
/*                                                                              */
/********************************************************************************/

async function handleAuthorizeGet(req,res)
{
   console.log("AUTHORIZE",req.originalUrl);
   
   console.log("GET AUTHORIZE",req.path,req.query,req.body,req.app.locals.user,req.session);
   console.log("REQUEST",req.headers);
   if (req.app.locals.original == null) {
      req.app.locals.original = "https://" + req.headers.host + req.originalUrl;
    }
   
   let user = req.app.locals.user;
   
   user = await db.query1("SELECT * FROM iQsignUsers WHERE username = 'spr'");
         
   if (!user) {
      let cinfo = await model.getClient(req.query.client_id,null);
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
   
   res.append("Referer",req.app.locals.original);
   res.append("User-Agent","IqSign-Oauth");
   
   let client = await model.getClient(req.body.client_id);
   if (client == null) throw "Unknown client";
   
   let d1 = new Date().getTime();
   d1 += 5*60*1000;
   let d2 = new Date(d1);
   let code = { authorizationCode: config.randomString(32), 
         expiresAt : d2,
         redirectURI : req.body.redirect_uri,
         scope : req.body.scope,
    };
   
   let code1 = await model.saveAuthorizationCode(code,client,user);
   let rslt = { code : code, state : req.body.state };
   let xrslt = xmlbuilder.create("oauth")
         .ele("code",code.authorizationCode).up()
         .ele("state",req.body.state).up()
         .end({ pretty : true });
   let tgt = req.body.redirect_uri;
   tgt += "?code=" + code.authorizationCode + "&state=" + req.body.state;
   res.location(tgt);
   res.status(302);
   res.type("xml");
   res.send(xrslt);
   res.end();
   
   console.log("RETURN",res._header,xrslt);
   return;
   
   req.app.locals.original = null;
   
   console.log("PRESEND",res._header);
   
   let opts = { model : model,
         authenticateHandler : authorizeAuthenticator(user) }; 
   let x = oauthcode.authorize( opts );
   let x1 = await x(req,res);
   
   console.log("AUTHORIZE DONE",user,res._header);
}


function authorizeAuthenticator(user) 
{
   return { handle : 
      function(req,res) {
         return { id : user.id, email : user.email, username : user.username };
    }
    }
}
   
   

function handleAuthorizePost(req,res)
{
   console.log("POST AUTHORIZE",req.path,req.query,req.body,req.app.locals);
   req.query = req.body;
   return handleAuthorizeGet(req,res);
}


function handleOauthLoginGet(req,res)
{
   return auth.displayOauthLoginPage(req,res)
}



/********************************************************************************/
/*										*/
/*	Handle authorization complete						*/
/*										*/
/********************************************************************************/

function handleAuthorized(req,res)
{
   res.send("Secret area");
}


function handlePublic(req,res)
{
   res.send("Public area");
}


function handleOauthGet(req,res)
{
   console.log("OAUTH GET",req.body,req.query,req.path);
}


function handleOauthPost(req,res)
{
   console.log("OAUTH POST",req.body,req.path);
}

/********************************************************************************/
/*										*/
/*	Exports                 						*/
/*										*/
/********************************************************************************/

exports.handleAuthorizeToken = handleAuthorizeToken;
exports.handleAuthorizeGet = handleAuthorizeGet;
exports.handleAuthorizePost = handleAuthorizePost;
exports.handleOauthLoginGet = handleOauthLoginGet;
exports.handleOauthGet = handleOauthGet;
exports.handleOauthPost = handleOauthPost;







/* end of module oauthserver */
