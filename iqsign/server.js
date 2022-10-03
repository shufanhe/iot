 /********************************************************************************/
/*										*/
/*		server.js							*/
/*										*/
/*	 Main server code of iQsign web service 				*/
/*										*/
/********************************************************************************/
"use strict";

const http = require('http');
const https = require('https');

const express = require('express');

const errorhandler = require('errorhandler');
const logger = require('morgan');
const bodyparser = require('body-parser');
const axios = require("axios");
const cors = require("cors");
const exphbs = require("express-handlebars");
const handlebars = exphbs.create( { defaultLayout : 'main'});
const favicons = require("connect-favicons");
const session = require('express-session');
const cookieparser = require('cookie-parser');
const redis = require('redis');
const RedisStore = require('connect-redis')(session);
const redisClient = redis.createClient();
const uuid = require('node-uuid');

const { StateRefreshResopnse, StateUpdateRequest } = require('st-schema');

const connector = require('./connector');
const config = require("./config");
const auth = require("./auth");
const db = require("./database");
const sign = require("./sign");
const defaultsigns = require("./defaultsigns");
const images = require("./images");
const smartthings = require("./smartthings");
const rest = require("./rest");




/********************************************************************************/
/*										*/
/*	Express setup								*/
/*										*/
/********************************************************************************/

function setup()
{
    const app = express();
    const restful = rest.restRouter(express.Router());

    app.engine('handlebars', handlebars.engine);
    app.set('view engine','handlebars');

    app.use(logger('combined'));

    app.use(favicons(__dirname + config.STATIC));

    app.use(initialize);

    app.use(bodyparser.urlencoded({ extended: true } ));
    app.use(bodyparser.json());

    app.use('/static',express.static(__dirname + config.STATIC));
    app.get('/robots.txt',(req1,res1) => { res1.redirect('/static/robots.txt')});

//  app.use(cors({credentials: true, origin: true}));
    app.use(cors());

    app.post('/smartthings',smartthings.handleSmartInteraction);
//  app.post('/smartapp',smartthings.handleSmartThings);
//  app.get('/smartapp',smartthings.handleSmartThingsGet);
    
    
    app.use(session( { secret : config.SESSION_KEY,
	  store : new RedisStore({ client: redisClient }),
	  saveUninitialized : true,
	  resave : true }));
    app.use(sessionManager);
    
    app.all('/rest/*',restful);
    
    app.get('/login',auth.displayLoginPage);
    app.post('/login',auth.handleLogin);
    app.get('/register',auth.displayRegisterPage);
    app.post('/register',auth.handleRegister);
    app.get('/validate',auth.handleValidate);
    app.get('/forgotpassword',auth.handleForgotPassword);
    app.post('/resetpassword',auth.handleResetPassword);
    app.get('/newpassword',auth.handleGetNewPassword);
    app.post('/newpassword',auth.handleSetNewPassword);
    

    app.get("/",displayRootPage);
    app.get("/instructions",displayInstructionsPage);
    app.get("/about",displayAboutPage);
    app.get("/default",displayDefaultPage);

    app.use(auth.authenticate);

    app.get("/svgimages",images.displaySvgImagePage);
    app.get("/savedimages",images.displaySavedImagePage);
    app.get("/image",images.displayImage);

    app.get("/index",sign.displayIndexPage);
    app.get("/home",sign.displayHomePage);
    app.get("/sign",sign.displaySignPage);
//  app.post("/gencode",sign.displayCodePage);
//  app.post("/createcode",sign.createLoginCode);
   
    app.post("/editsign",sign.handleUpdate);
    app.post("/savesignimage",sign.handleSaveSignImage);
    app.post("/loadsignimage",sign.handleLoadSignImage);
    app.get("/loadimage",images.displayLoadImagePage);
    app.post("/loadimage",images.handleLoadImage);
    
    app.all('*',handle404);
    app.use(errorHandler);
    
//  restful.use(rest.session);
//  restful.get('/rest/login',rest.handlePrelogin);
//  restful.post('/rest/login',rest.handleLogin);
//  restful.post("/rest/register",rest.handleRegister);
//  restful.use(rest.authenticate);
//  restful.get("/rest/signs",rest.handleGetAllSigns);
//  restful.get("/rest/sign/:signid",rest.handleGetSignData);
//  restful.put("/rest/sign/:signid",rest.handleUpdateSignData);    
//  restful.delete("/rest/sign/:signid",rest.handleDeleteSign);
//  restful.post("/rest/update/:signid",rest.handleUpdateSign);    
//  restful.post("/rest/setsign/:signid/:imageid",rest.handleSetSign);
//  restful.get("/rest/segsign",rest.handleGetAllSavedSigns);
//  restful.get("/rest/image/:imageid",rest.handleGetImage);
//  restful.post("/rest/image/:imageid",rest.handleUpdateImage);
//  restful.all('*',handle404);
//  restful.use(errorHandler);
    
    const server = app.listen(config.PORT);
    console.log(`HTTP Server listening on port ${config.PORT}`);

    try {
	const httpsserver = https.createServer(config.getHttpsCredentials(),app);
	httpsserver.listen(config.HTTPS_PORT);
	console.log(`HTTPS Server listening on port ${config.HTTPS_PORT}`);
     }
    catch (error) {
	console.log("Did not launch https server",error);
     }
}



/********************************************************************************/
/*										*/
/*	Static displays 							*/
/*										*/
/********************************************************************************/

function displayRootPage(req,res)
{
   console.log("ROOT PAGE",req.session.user);
   if (req.session.user == null) res.redirect("/login");
   else res.redirect("/home");
}

function displayDefaultPage(req,res)
{
   console.log("DEFAULT PAGE",req.query,req.session,req.oauth);
   res.render("default");
}


function displayAboutPage(req,res)
{
   res.render("about`");
}


function displayInstructionsPage(req,res,what)
{
   res.render("instructions");
}




/********************************************************************************/
/*										*/
/*	Session management							*/
/*										*/
/********************************************************************************/

function sessionManager(req,res,next)
{
   if (req.session.uuid == null) {
      req.session.uuid = uuid.v1();
      req.session.save();
      console.log("START SESSION",req.session);
    }
   next();
}



/********************************************************************************/
/*										*/
/*	Error handling								*/
/*										*/
/********************************************************************************/

function handle404(req,res)
{
   res.status(404);
   if (req.accepts('html')) {
      res.render('error404', { title: 'Page Not Found'});
    }
   else if (req.accepts('json')) {
      let rslt = { status : 'ERROR', reason: 'Invalid URL'} ;
      res.end(JSON.stringify(rslt));
    }
   else {
      res.type('txt').send('Not Found');
    }
}



function errorHandler(err,req,res,next)
{
   console.log("ERROR on request %s %s %s",req.method,req.url,err);
   console.log("STACK",err.stack);

   res.status(500);
   let msg = 'Server Error';
   if (req.accepts('html')) {
      res.render('error500', { title: 'Server error', reason: msg });
    }
   else if (req.accepts('json')) {
      let rslt = { status : 'ERROR' ,reason: msg} ;
      res.end(JSON.stringify(rslt));
    }
   else {
      res.type('txt').send('Server Error');
    }
}

var is_inited = false;

async function initialize(req,res,next)
{
   if (!is_inited) {
      await init();
      is_inited = true;
    }
   next();
}


/********************************************************************************/
/*										*/
/*	Main routine								*/
/*										*/
/********************************************************************************/

async function init()
{
   console.log("START INIT");
   await defaultsigns.update();
   await images.loadImages();
   console.log("DONE INIT");
}

setup();




/* end of server.js */

