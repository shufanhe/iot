/********************************************************************************/
/*                                                                              */
/*              samsung.js                                                      */
/*                                                                              */
/*      Interface to Smartthings using core-sdk                                 */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/
"use strict";
   
const {SmartThingsClient,BearerTokenAuthenticator} = require('@smartthings/core-sdk');   

const config = require("./config");



/********************************************************************************/
/*                                                                              */
/*      Global storage                                                          */
/*                                                                              */
/********************************************************************************/

var users = { };



/********************************************************************************/
/*										*/
/*	Setup Router								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   restful.use(authenticate);
   
   restful.all("*",config.handle404)
         restful.use(config.handleError);
   
   return restful;
}


/********************************************************************************/
/*										*/
/*	Authentication for iqsign						*/
/*										*/
/********************************************************************************/

function authenticate(req,res,next)
{
   next();
}



async function addBridge(authdata,bid)
{
   console.log("SAMSUNG ADD BRIDGE",authdata.uid,authdata.token);
   
   let username = authdata.uid;
   let pat = authdata.token;
   let user = users[username];
   if (user == null) {
      let client = new SmartThingsClient(new BearerTokenAuthenticator(pat));
      user = { username: username, client: client, bridgeid: bid, devices: [] };
      users[username] = user;
    }   
 
   getDevices(username);
   
   return false;
}


async function handleCommand(bid,uid,devid,command,values)
{}


/********************************************************************************/
/*                                                                              */
/*      List devices                                                            */
/*                                                                              */
/********************************************************************************/

async function getDevices(user)
{
   let client = users[user].client;
   client.devices().list().then(handleDevices);
}


function handleDevices(list)
{
   console.log("FOUND DEVICES",list,list.length);
}


/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;



/* end of module samsung */
