/********************************************************************************/
/*										*/
/*		auth.js 							*/
/*										*/
/*	Authorization code							*/
/*										*/
/********************************************************************************/
"use strict";

const crypto = require('crypto');
const config = require("./config");
const db = require("./database");
const emailsender = require("./email");
const sign = require("./sign");



/********************************************************************************/
/*										*/
/*	Middleware								*/
/*										*/
/********************************************************************************/

function authenticate(req,res,next)
{
   // should check for oauth-authentication as well here
   if (req.session.code == null) {
      req.session.code = config.randomString(32);
    }
   if (req.session.user == null) {
      res.redirect("/login?redirect=" + encodeURIComponent(req.originalUrl));
    }
   else {
      req.user = req.session.user;
      req.session.touch();
      next();
    }
}


/********************************************************************************/
/*										*/
/*	Login authentication							*/
/*										*/
/********************************************************************************/

function displayLoginPage(req,res)
{
   if (req.session.code == null) {
      req.session.code = config.randomString(32);
    }
   req.session.touch();

   let data = { padding : req.session.code, redirect : req.query.redirect };
   res.render('login', data);
}


function displayOauthLoginPage(req,res)
{
   let code = config.randomString(32);
   let data = { 
         padding : code, 
         redirect : req.query.redirect || "/oauth/authorize",
         redirect_uri : req.query.redirect_uri,
         client_id : rrq.query.client_id,
    };
   res.render('oauthlogin',data);
}



async function handleLogin(req,res)
{
   req.user = null;			// log user out to being with
   if (req.session) {
      req.session.user = null;
      req.session.save();
    }

   try {
      console.log("HANDLE LOGIN",req.body);
      let rslt = { };
      if (req.body.username == null || req.body.username == '') {
	 return handleError(req,res,"User name must be given");
       }
      else if (req.session && req.body.padding != req.session.code) {
	 return handleError(req,res,"Invalid login attempt");
       }

      let uid = req.body.username;
      uid = uid.toLowerCase();
      let rows = await db.query("SELECT * FROM iQsignUsers WHERE email = $1 OR username = $2 AND valid",
	    [uid , uid]);
      if (rows.length != 1) {
	 return handleError(req,res,"Invalid username or password");
       }
      let row = rows[0];
      let pwd = row.password;
      if (uid != row.email && uid == row.username) {
	 pwd = row.altpassword;
       }
      let s = req.body.padding;
      let pwd1 = hasher(pwd + s);

      if (pwd1 != req.body.password) {
	 return handleError(req,res,"Invalid username or password");
       }
      req.user = row;
      if (req.session) {
         req.session.user = req.user;
         req.session.save();
       }
      req.app.locals.user = req.user;
      
      rslt = { status : "OK" };
      res.end(JSON.stringify(rslt));
    }
   catch (err) {
      handleError(req,res,"Database problem: " + err);
    }
}




/********************************************************************************/
/*										*/
/*	Handle regisgteration							*/
/*										*/
/********************************************************************************/

function displayRegisterPage(req,res)
{
   if (req.session.code == null) {
      req.session.code = config.randomString(32);
    }
   req.session.touch();

   let data = { code : req.session.code };
   res.render('register', data);
}



async function handleRegister(req,res)
{
   let undo = false;
   let email = req.body.email;
   email = email.toLowerCase();
   req.body.email = email;
   let uid = req.body.username;
   uid = uid.toLowerCase();
   req.body.username = uid;
   let pwd = req.body.password;
   let altpwd = req.body.altpassword;
   let valid = config.randomString(48);
   let namekey = config.randomString(8);

   try {
      console.log("HANDLE REGISTER",req.body);

      if (email == null || email == '') {
	 return handleError(req,res,"Email must be given");
       }
      else if (!validateEmail(email)) {
	 return handleError(req,res,"Invalid email address");
       }
      else if (uid == null || uid == '') {
	 uid = email;
       }
      else if (validateEmail(uid) && uid != email) {
	 return handleError(req,res,"User id must not be an email address");
       }
      else if (req.body.signname == null || req.body.signname == '') {
	 return handleError(req,res,"Sign name must be given");
       }

      let rows = await db.query("SELECT * FROM iQsignUsers WHERE email = $1 OR username = $2",
	    [email, uid]);
      console.log("QUERY RESULT",rows);

      if (rows.length != 0) {
	 let msg = "Email or user name already in use";
	 for (let i = 0; i < rows.length; ++i) {
	    let row = rows[i];
	    if (row.email == req.body.email) {
	       msg = "Email already registered";
	       break;
	     }
	    else if (req.username == req.body.username) {
	       msg = "User name already in use";
	       break;
	     }
	  }
	 return handleError(req,res,msg);
       }
      let s = config.INITIAL_SIGN;
      let ss = s.replace(/\n/g,"\\n");
      ss = ss.replace(/\r/g,"");
      ss = ss.replace(/\t/g," ");

      undo = true;

      await db.query("INSERT INTO iQsignUsers " +
	       "( id, email, username, password, altpassword, valid) " +
	       "VALUES ( DEFAULT, $1, $2, $3, $4, DEFAULT )",
	       [email,uid,pwd,altpwd]);

      await db.query("INSERT INTO iQsignValidator " +
	    "( userid, validator, timeout ) " +
	    "VALUES ( ( SELECT id FROM iQsignUsers WHERE email = $1 ), $2, " +
	    " ( CURRENT_TIMESTAMP + INTERVAL '1' DAY ) )",
	    [ req.body.email,valid ]);
      await sign.setupSign(req.body.signname,email);

      let host = req.headers.host;
      let msg = "Thank you for registering with iQsign.\n";
      msg += "To complete the reqistration process, please click on or paste the link:\n";
      msg += "   http://" + host + "/validate?";
      msg += "email=" + email;
      msg += "&code=" + valid;
      msg += "\n";

      await emailsender.sendMail(email,"iQsign account validation",msg);

      let rslt = { status:  "OK" };
      res.end(JSON.stringify(rslt));
    }
   catch (err) {
      if (undo) await unregister(email);
      console.log("Register error",err);
      return handleError(req,res,"Database/email problem: " + err);
    }
}


async function unregister(email)
{
   try {
      let rows0 = await db.query("SELECT id FROM iQsignUsers WHERE email = $1",
	    [ email ]);
      if (rows0.length != 1) return
      let uid = rows0[0].id;
      await db.query0("DELETE FROM iQsignValidator WHERE userid = $1",[uid]);
      await db.query0("DELETE FROM iQsignSigns WHERE userid = $1",[uid]);
      await db.query0("DELETE FROM iQsignUsers WHERE id = $1",[uid]);
    }
   catch (err) { }
}



/********************************************************************************/
/*										*/
/*	Handle email validation 						*/
/*										*/
/********************************************************************************/

async function handleValidate(req,res)
{
   try {
      console.log("VALIDATE",req.query);
      let email = req.query.email;
      email = email.toLowerCase();
      let rows = await db.query("SELECT U.id,V.validator FROM iQsignValidator V,iQsignUsers U " +
	    "WHERE U.email = $1 AND V.userid = U.id AND V.timeout > CURRENT_TIMESTAMP",
	    [email]);
      if (rows.length != 1) {
	 console.log("ROWS ERROR ",rows,req.query.code);
	 return res.redirect("/invalid/validationerror");
       }
      else {
	 let code = req.query.code;
	 let id = null;
	 for (let i = 0; i < rows.length; ++i) {
	    let row = rows[i];
	    let vid = row.validator;
	    if (vid == code) {
	       id = row.id;
	       break;
	     }
	  }
	 if (id != null) {
	    await db.query("UPDATE iQsignUsers SET valid = 'true' WHERE id = $1",[ id ]);
	    await db.query("DELETE FROM iQSignValidator WHERE (userid = $1 AND validator = $2) OR timeout < CURRENT_TIMESTAMP",
		  [id, code ]);
	    let row1 = await db.query("SELECT * FROM iQsignUsers WHERE id = $1",[id]);
	    req.user = row1[0];
	    req.session.user = req.user;
	    req.session.save();
	  }
	 res.redirect("/index");
       }
    }
   catch (err) {
      console.log("VALIDATION ERROR",err);
      res.redirect("/invalid/validationerror");
    }
}



/********************************************************************************/
/*										*/
/*	Handle forgot password							*/
/*										*/
/********************************************************************************/

function handleForgotPassword(req,res)
{
    res.render("forgotpassword");
}


async function handleResetPassword(req,res)
{
   try {
      console.log("HANDLE RESET PASSWORD",req.body);
      let email = req.body.email;
      email = email.toLowerCase();
      req.body.email = email;

      if (email == null || email == '') {
	 return handleError(req,res,"Email must be given");
       }
      else if (!validateEmail(email)) {
	 return handleError(req,res,"Invalid email address");
       }

      let data = await db.query("SELECT * FROM iQsignUsers WHERE email = $1",
	    [email]);
      if (data.rows.length == 1) {
	 let row = data.rows[0];
	 let id = row.id;
	 let code = config.randomString(24);
	 await db.query("INSERT INTO iQsignValidator " +
	       "( id, validator, timeout ) " +
	       "VALUES ( $1, $2, " +
	       " ( CURRENT_TIMESTAMP + INTERVAL '1' DAY ) )",
	       [ email,code ]);
	 let host = req.headers.host;
	 let msg = "Here is the password reset link for iQsign you requested.\n";
	 msg += "To reset your password, please click on or paste the link:\n";
	 msg += "   http://" + host + "/newpassword?";
	 msg += "email=" + email;
	 msg += "&code=" + code;
	 msg += "\n";
	 await emailsender.sendMail(email,"iQsign account validation",msg);
	 let rslt = { status:  "OK" };
	 res.end(JSON.stringify(rslt));
       }
    }
   catch (err) {
      handleError(req,res,"Database/email problem: " + err);
    }
}



async function handleGetNewPassword(req,res)
{
   try {
      let email = req.query.email;
      email = email.toLowerCase();
      let code = req.query.code;
      let rows = await db.query("SELECT U.email, U.username,U.idFROM iQsignValidator V,iQsignUsers U " +
	    "WHERE U.email = $1 AND V.userid = U.id AND V.code = $2 " +
	    "AND V.timeout > CURRENT_TIMESTAMP AND U.valid",
	    [email,code]);
      if (rows.length != 1) {
	 res.redirect("/invalid/validationerror");
       }
      let row = rows[0];
      let d = { email : row.email, code: req.body.code, uid: row.username };
      res.render('newpassword',d);
    }
   catch (err) {
      res.redirect("/invalid/validationerror");
    }
}



async function handleSetNewPassword(req,res)
{
   try {
      let email = req.body.email;
      email = email.toLowerCase();
      let code = req.body.code;
      let uid = req.body.username;
      uid = uid.toLowerCase();
      req.body.username = uid;
      let pwd = req.body.password;
      let altpwd = req.body.altpassword;

      let rows = await db.query("SELECT V.userid, V.validator FROM iQsignValidator V,iQsignUsers U " +
	    "WHERE U.email = $1 AND U.username = $2 AND U.id = V.userid " +
	    " AND V.validator = $3 AND V.timeout > CURRENT_TIMESTAMP AND U.valid",
	    [email,uid,code]);

      if (rows.length == 1) {
	 let row = rows[0];
	 await db.query("DELETE FROM iQSignValidator WHERE (userid = $1 AND validator = $2) OR timeout < CURRENT_TIMESTAMP",
	       [ row.id,row.validator ]);
       }

      await db.query("UPDATE iQsignUsers SET password = $1, altpassword = $2 " +
	    "WHERE email = $3 AND username = $4",
	    [pwd,altpwd,req.body.email,req.body.username]);

      let rslt = { status:  "OK" };
      res.end(JSON.stringify(rslt));
    }
   catch (err) {
      handleError(req,res,"Database problem: " + err);
    }
}



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

function hasher(msg)
{
    const hash = crypto.createHash('sha512');
    const data = hash.update(msg,'utf-8');
    const gen = data.digest('base64');
    return gen;
}




function validateEmail(data)
{
    const res = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return res.test(String(data).toLowerCase());
}



function handleError(req,res,msg)
{
    let rslt = { status : 'ERROR', message: msg };
    res.end(JSON.stringify(rslt));
}




/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.authenticate = authenticate;
exports.displayLoginPage = displayLoginPage;
exports.handleLogin = handleLogin;
exports.displayRegisterPage = displayRegisterPage;
exports.handleRegister = handleRegister;
exports.handleValidate = handleValidate;
exports.handleForgotPassword = handleForgotPassword;
exports.handleResetPassword = handleResetPassword;
exports.handleGetNewPassword = handleGetNewPassword;
exports.handleSetNewPassword = handleSetNewPassword;




/* end of auth.js */

