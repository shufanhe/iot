/********************************************************************************/
/*										*/
/*		email.js							*/
/*										*/
/*	Code for handling email sending 					*/
/*										*/
/********************************************************************************/
"use strict";


/********************************************************************************/
/*										*/
/*	Imports 								*/
/*										*/
/********************************************************************************/


var nodemailer = require('nodemailer-promise');

var config = require('./config.js');




/********************************************************************************/
/*										*/
/*	Setup mail								*/
/*										*/
/********************************************************************************/

var emaildata = config.emailData();
var configdata = { host : emaildata.host,
      auth: { user: emaildata.user, pass: emaildata.password } };
configdata = { service: "Gmail",
      auth: { user: emaildata.user, pass: emaildata.password } };
var sender = nodemailer.config(configdata);

var defaultOptions = {
   from: emaildata.fromid
};


/********************************************************************************/
/*										*/
/*	Send mail								*/
/*										*/
/********************************************************************************/

async function sendMail(addr,subj,cnts)
{
   console.log("SENDMAIL",addr,cnts);

   var opts = { };
   for (var opt in defaultOptions) {
      opts[opt] = defaultOptions[opt];
    }

   opts.to = addr;
   opts.subject = subj;
   opts.text = cnts;

   console.log("EMAIL ",configdata,opts);
   let rslt = await sender(opts);
   console.log("MAIL RESULT",rslt);

   return rslt;
}


async function sendMailToiQsign(addr,subj,cnts,cb)
{
   console.log("SENDMAILTO",addr,cnts);

   var opts = { to : emaildata.toid, from: addr, subject: subj, text: cnts };

   return await sender(opts,cb);
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.sendMail = sendMail;
exports.sendMailToiQsign = sendMailToiQsign;


/* end of email.js */
