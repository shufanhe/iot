/********************************************************************************/
/*										*/
/*		sign.js 							*/
/*										*/
/*	Code for managing the sign						*/
/*										*/
/********************************************************************************/
"use strict";

const db = require('./database');
const config = require("./config");
const server = require("./server");
const auth = require("./auth");

const handlebars = require("handlebars");

const fs = require("fs/promises");
const util = require("util");
const { exec, spawn } = require('child-process-async');
const tmp = require('tmp-promise');
tmp.setGracefulCleanup();

const net = require("net");
const { PromiseSocket } = require("promise-socket");

let qropts = {
      errorCorrectionLevel: 'H',
      type: 'image/png',
};





/********************************************************************************/
/*										*/
/*	Display home page							*/
/*										*/
/********************************************************************************/

async function displayIndexPage(req,res)
{
   return displayHome(req,res,false);
}


async function displayHomePage(req,res)
{
   return displayHome(req,res,true);
}



async function displaySignPage(req,res)
{
   displayHome(req,res,false,req.query.sign);
}



async function displayHome(req,res,home,sid)
{
   if (req.session.user == null) {
      res.redirect("/login");
      return;
    }
   
   try {
      let rows = null;
      if (sid == null) {
	 rows = await db.query("SELECT * FROM iQsignSigns WHERE userid = $1",
	       [req.session.user.id]);
       }
      else {
	 rows = await db.query("SELECT * FROM iQsignSigns WHERE userid = $1 AND id = $2",
	       [req.session.user.id,sid]);
       }
      if (rows.length == 0) {
         rows = await db.query("SELECT * FROM iQsignUsers WHERE id = $1",
               [req.session.user.id]);
         if (rows.length == 0) res.redirect('/login');
	 else res.redirect('default');
       }
      else if (rows.length == 1 && !home) {
	 renderSignPage(req,res,rows[0]);
       }
      else {
	 renderHomePage(req,res,rows);
       }
    }
   catch (err) {
      server.errorHandler(err,req,res);
    }
}



/********************************************************************************/
/*										*/
/*	Home page rendering							*/
/*										*/
/********************************************************************************/

function renderHomePage(req,res,signsdata)
{
   let data = { signs : signsdata, user : req.session.user, code : req.session.code };

   if (signsdata.length == 1) {
      data.signwidth = 6;
    }
   else {
      data.signwidth = 5;
      data.remove = true;
    }
   data.urlpfx = "http://" + config.getWebHost() + "/iqsign";

   res.render("home" ,data);
}



async function renderSignPage(req,res,signdata)
{
   let rows0 = await db.query("SELECT id, name FROM iQsignDefines " +
         "WHERE userid = $1 OR userid IS NULL", [req.session.user.id ]);
   let rows1 = await db.query("SELECT id,name FROM iQsignImages " +
         "WHERE userid = $1 OR userid IS NULL", [req.session.user.id ]);
   let data = {
         sign : signdata, 
         user : req.session.user, 
         code: req.session.code,
         urlpfx : "http://" + config.getWebHost() + "/iqsign",
         savedsigns : rows0,
         savedimages : rows1,
         anysavedimages : rows1.length > 0,
    };
   data["dim" + signdata.dimension] = "selected";

   res.render("sign" ,data);
}


/********************************************************************************/
/*                                                                              */
/*      Handle update request                                                   */
/*                                                                              */
/********************************************************************************/

async function handleUpdate(req,res)
{
   console.log("Sign UPDATE",req.body);
   let s = req.body.signdata.trim();
   let ss = s;
   ss = ss.replace(/\r/g,"");
   ss = ss.replace(/\t/g," ");
   
   let uid = req.body.signuser;
   
   if (req.body.signuser != req.user.id) throw "Invalid user";
   
   console.log("UPDATE DATA",ss);
   
   let rows = await db.query("SELECT * FROM iQsignSigns WHERE id = $1 " +
         " AND userid = $2 AND namekey = $3",
         [ req.body.signid, uid, req.body.signkey ]);
   if (rows.length == 0) throw "Invalid sign id";
   
   await db.query("UPDATE iQsignSigns " +
         "SET name = $1, lastsign = $2, lastupdate = CURRENT_TIMESTAMP, " +
         "  dimension = $3, width = $4, height = $5 " +
         "WHERE id = $6",
         [req.body.signname, ss, req.body.signdim,
               req.body.signwidth, req.body.signheight, req.body.signid]);
   
   rows = await db.query("SELECT * FROM iQsignSigns WHERE id = $1 " +
         " AND userid = $2 AND namekey = $3",
         [ req.body.signid, req.body.signuser, req.body.signkey ]);
   if (rows.length == 0) throw "Invalid sign id";
   let signdata = rows[0];
   
   await setupWebPage(signdata);
   await updateSign(signdata,req.user.id);    
   
   console.log("SIGN SHOULD BE READY");
   
   renderSignPage(req,res,signdata);
}


/********************************************************************************/
/*                                                                              */
/*      Handle save sign                                                        */
/*                                                                              */
/********************************************************************************/

async function handleSaveSignImage(req,res)
{
   console.log("SAVE SIGN IMAGE",req.body);
   
   if (req.body.signid == null) return handleError(req,res,"No sign id");
   if (req.body.name == '') return handleError(req,res,"No name given");
   if (req.session.code != req.body.code) return handleError(req,res,"Invalid code");
   if (req.body.signuser != req.user.id) handleError(req,res,"Invalid user");
   let uid = req.user.id;
   if (req.user.admin) uid = null;
   
   let rows = await db.query("SELECT * FROM iQsignSigns WHERE id = $1 ", 
         [ req.body.signid ]);
   if (rows.length == 0) return handleError(req,res,"Invalid sign id");
   let signdata = rows[0];
   if (signdata.userid != req.body.signuser) return handleError(req,res,"Invalid user");
   if (signdata.namekey != req.body.signnamekey) return handleError(req,res,"Invalid namekey");
   
   let rows1 = await db.query("SELECT * FROM iQsignDefines " +
         "WHERE userid = $1 AND name = $2",
         [ uid, req.body.name ]);
   if (rows1.length == 0) {
      let rows2 = await db.query("INSERT INTO iQsignDefines(userid,name,contents) " +
            "VALUES ( $1, $2, $3 )",
            [ uid, req.body.name, signdata.lastsign ]);
    }
   else {
      let rows3 = await db.query("UPDATE iQsignDefines SET contents = $1 "+
            "WHERE userid = $2 AND name = $3",
            [ signdata.lastsign, uid, req.body.name ]);
    }
   
   handleOk(req,res);
}



/********************************************************************************/
/*                                                                              */
/*      Handle load sign                                                        */
/*                                                                              */
/********************************************************************************/

async function handleLoadSignImage(req,res)
{
   console.log("LOAD SIGN IMAGE",req.body);
   
   if (req.body.signid == null) return handleError(req,res,"No sign id");
   if (req.body.name == '') return handleError(req,res,"No name given");
   if (req.session.code != req.body.code) return handleError(req,res,"Invalid code");
   
   let cnts = null;
   let name = null;
   if (req.body.name == "*Current*") {  
      let rows = await db.query("SELECT * FROM iQsignSigns WHERE id = $1", 
            [ req.body.signid ]);
      if (rows.length == 0) return handleError(req,res,"Invalid sign id");
      let signdata = rows[0];
      if (signdata.userid != req.user.id && signdata.userid != null) 
         return handleError(req,res,"Invalid user");
      if (signdata.namekey != req.body.signnamekey) return handleError(req,res,"Invalid namekey");
      cnts = signdata.lastsign;
    }
   else {
      let rows = await db.query("SELECT * FROM iQsignDefines WHERE id = $1",
            [ req.body.name ]);
      if (rows.length == 0) return handleError(req,res,"Bad define id");
      let defdata = rows[0];
      if (defdata.userid != null && defdata.userid != req.user.id) 
         return handleError(req,res,"Bad user define id");
      cnts = defdata.cnts;
      name = defdata.name;
    }
   
   let data = {
      name : name,
      contents : cnts,
    };
   handleOk(req,res,data);
}


/********************************************************************************/
/*                                                                              */
/*      Setup a sign for a user                                                 */
/*                                                                              */
/********************************************************************************/

async function setupSign(name,email)
{
   let namekey = config.randomString(8);
   let s = config.INITIAL_SIGN.trim();
   let ss = s;
   ss = ss.replace(/\r/g,"");
   ss = ss.replace(/\t/g," ");
   
   console.log("SIGN",name,email,namekey,ss);
   let rows0 = await db.query("SELECT id FROM iQsignUsers WHERE email = $1",
         [ email ]);
   if (rows0.length != 1) {
      console.log("SETUP SIGN: Bad user email",email);
      return;
    }
   let uid = rows0[0].id;
   
   await db.query("INSERT INTO iQsignSigns (id, userid, name, namekey, lastsign) " +
         "VALUES ( DEFAULT, $1, $2, $3, $4 )",
         [ uid, name, namekey, ss ]);
   
   let rows = await db.query("SELECT * FROM iQsignSigns WHERE namekey = $1",
         [namekey]);
   let signdata = rows[0];
   
   await setupWebPage(signdata);
   await updateSign(signdata,uid);
}



async function setupWebPage(signdata)
{
   var txt = await fs.readFile(config.TEMPLATE_FILE,'utf8');
   txt = txt.toString().trim() + "\n";
   var template = handlebars.compile(txt);
   var data = { namekey : signdata.namekey };
   var result = template(data);
   console.log("SETUP PAGE",getHtmlFile(signdata.namekey));
   await fs.writeFile(getHtmlFile(signdata.namekey),result);
}




/********************************************************************************/
/*										*/
/*	Update sign								*/
/*										*/
/********************************************************************************/

async function updateSign(signdata,uid)
{
   await updateSignSocket(signdata,uid);
}


async function updateSignSocket(signdata,uid)
{
   let pass = { width : signdata.width, 
         height : signdata.height,
         userid : uid,
         contents : signdata.lastsign,
         outfile : getImageFile(signdata.namekey),
    };
   
   console.log("START SIGN SOCKET UPDATE");
   
   let sock = new net.Socket({ allowHalfOpen : true, readable : true, writable : true });
   let psock = new PromiseSocket(sock);
   
   await psock.connect(config.MAKER_PORT);
   await psock.writeAll(JSON.stringify(pass));
   await psock.end();
   let data = await psock.readAll();
   let rslt = JSON.parse(data);
   sock.destroy();
   console.log("DONE SOCKET UPDATE",rslt);
}


async function updateSignExec(signdata)
{
   let data = signdata.lastsign;
   
   let w = signdata.width;
   let h = signdata.height;
   let tmpobj = await tmp.file();
   await fs.writeFile(tmpobj.path,data);
   
   console.log("UPDATE CONTENTS",data);
   
   let args = [ "-w", w, "-h", h, "-i", tmpobj.path, "-o", getImageFile(signdata.namekey) ];
   console.log("UPDATE SIGN",args);
   
   const child = spawn(config.getSignBuilder(), args, { } );
   child.stderr.on('data', (data2) => {
      console.log('signmaker:',data2);
    });
   child.stdout.on('data', (data1) => {
      console.log('signmaker:',data1);
    });
   const { exitCode } = await child;
   
   console.log("DONE EXEC",exitCode);
}



function getImageUrl(req,namekey)
{
   return "http://" + config.getWebHost() + "/iqsign/signs/image" + namekey  + ".png";
}



function getImageFile(key)
{
   let f = config.getWebDirectory() + "/signs/image" + key + ".png";
   return f;
}


function getHtmlFile(key)
{
   let f= config.getWebDirectory() + "/signs/sign" + key + ".html";
   return f;
}


function handleError(req,res,msg)
{
   console.log("Request failed",msg);
   
   let rslt = { status : 'ERROR', message: msg };
   res.end(JSON.stringify(rslt));
}


function handleOk(req,res,rslt)
{
   console.log("Request succeeded");
   
   if (rslt == null) {
      rslt = { status : 'OK' };
    }
   else {
      rslt.status = "OK";
    }
   
   res.end(JSON.stringify(rslt));
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.displayHomePage = displayHomePage;
exports.displayIndexPage = displayIndexPage;
exports.displaySignPage = displaySignPage;
exports.setupSign = setupSign;
exports.setupWebPage = setupWebPage;
exports.updateSign = updateSign;
exports.handleUpdate = handleUpdate;
exports.handleSaveSignImage = handleSaveSignImage;
exports.handleLoadSignImage = handleLoadSignImage;


/* end of module sign */
