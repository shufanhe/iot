/********************************************************************************/
/*										*/
/*		oauthserver.js							*/
/*										*/
/*	Oauth server using common dbms						*/
/*										*/
/*	Written by spr, based on node-oauth2-server				*/
/*										*/
/********************************************************************************/

const http = require('http');
const https = require('https');

const express = require('express');

const logger = require('morgan');
const bodyparser = require('body-parser');
const OauthServer = require('express-oauth-server');
const exphbs = require("express-handlebars");
const handlebars = exphbs.create( { defaultLayout : 'main'} );
const favicons = require("connect-favicons");

const util = require('util');

const config = require("./config");
const model = require('./modelps');
const auth = require("./auth");

		
		
/********************************************************************************/
/*										*/
/*	Express setup								*/
/*										*/
/********************************************************************************/

function setup()
{
   const app = express();

   app.oauth = new OauthServer({
      model : model,
      grants: ['password'],
      useErrorHandler: true,
      debug: true
    });

   app.engine('handlebars', handlebars.engine);
   app.set('view engine','handlebars');

   app.use(logger('combined'));

   app.use(favicons(__dirname + config.STATIC));

   app.use(bodyparser.urlencoded({extended : true}));
   app.use(bodyparser.json());

   app.use('/static',express.static(__dirname + config.STATIC));
   app.get('/robots.txt',(req1,res1) => { res1.redirect('/static/robots.txt')});

   app.post('/oauth/token',app.oauth.token());
   app.get('/oauth/authorize',handleAuthorizeGet);
   app.post('/oauth/authorize',handleAuthorizePost);
   app.get('/login',handleOauthLoginGet);
   
   app.post('/login',auth.handleLogin);
   app.get('/secret',app.oauth.authenticate(),handleAuthorized);
   app.get('/public',handlePublic);

   const httpsserver = https.createServer(config.getHttpsCredentials(),app);
   httpsserver.listen(config.OAUTH_HTTPS_PORT);
   console.log(`OauthServer HTTPS serverlistening on ${config.OAUTH_HTTPS_PORT}`);
}




/********************************************************************************/
/*										*/
/*	Action functions							*/
/*										*/
/********************************************************************************/

function handleAuthorizeToken(req,res)
{
   let app = req.app;
   
   console.log("AUTHORIZE TOKEN",req.query,req.body,app.oauth);
         
   let fct = app.oauth.token();
   fct(req,res);   
}



function handleAuthorizeGet(req,res)
{
   console.log("GET AUTHORIZE",req.path,req.query,req.body,req.app.locals);

   if (!req.app.locals.user) {
      return res.redirect(util.format('/login?redirect=%s&client_id=%s&redirect_uri=%s',
	    req.path, req.query.client_id,
	    req.query.redirect_uri));
   }
   return res.render('authorize', {
      client_id : req.query.client_id,
      redirect_uri : req.query.redirect_uri
   });
}


function handleAuthorizePost(req,res)
{
   console.log("POST AUTHORIZE",req.path,req.query,req.body,req.app.locals);
   if (!req.app.locals.user) {
      return res.redirect(util.format('/login?redirect=%s&client_id=%s&redirect_uri=%s',
	    req.path,req.body.client_id, req.body.redirect_uri));
   }

   return req.app.oauth.authorize();
}


function handleOauthLoginGet(req,res)
{
   console.log("LOGIN GET",req.query);
   
   return auth.displayOauthLoginPage(req,res)
}


function handleLoginPost(req,res)
{
   console.log("LOGIN CHECK ",req.body);
   
   // @TODO: Insert your own login mechanism.

   if (req.body.username !== 'spr@dummy') {
      return res.render('oauthlogin', {
	 redirect: req.body.redirect,
	 client_id: req.body.client_id,
	 redirect_uri: req.body.redirect_uri
      });
   }
   else {
      req.app.locals.user = 'spr';
    }

   // Successful logins should send the user back to /oauth/authorize.
   var path = req.query.redirect || '/oauth/authorize';
   console.log("SUCCESS",req.body,path,req.query);

   var url = util.format('%s?client_id=%s&redirect_uri=%s',
	 path, req.query.client_id, req.query.redirect_uri);
   console.log("LOGIN SUCCESS. REDIRECT TO ",url);

   return res.redirect(url);
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
