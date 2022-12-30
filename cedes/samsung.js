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
      user = { username: username, client: client, bridgeid: bid, 
            devices: [], locations: { }, rooms: { } };
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

async function getDevices(username)
{
   let user = users[username];
   let client = user.client;
   await setupLocations(user);
   
   let devs = await client.devices.list();
   console.log("FOUND DEVICES",devs.length,devs);
   for (let dev of devs) {
      defineDevice(dev);
    }
}


function defineDevice(dev)
{
   let devid = dev.deviceId;
   let devname = dev.name;
   let devlabel = dev.label;
   for (let comp in dev.compnents) {
      console.log("DEVICE ",devid,comp);
      for (let cap in comp.capabilities) {
         console.log("FOUND CAPABILITY",cap);
       }
    }
}



async function setupLocations(user)
{
   let client = user.client;
   let locs = await client.locations.list();
   for (let loc in locs) {
      user.locations[loc.locationId] = loc;
    }
   console.log("FOUND LOCATIONS",locs);
   
   let rooms = await client.rooms.list();
   for (let room in rooms) {
    }
   
   console.log("FOUND ROOMS",locs);
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
