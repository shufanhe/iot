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
var capabilities = { };
var presentations = { };

var skip_capabilities = new Set();

skip_capabilities.add("ocf");
skip_capabilities.add("custom.disabledCapabilities");
skip_capabilities.add("execute");
skip_capabilities.add("healthCheck");


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
      await defineDevice(user,dev);
    }
}


async function defineDevice(user,dev)
{
   let catdev = { };
   catdev.UID = dev.deviceId;
   catdev.BRIDGE = "samsung";
   catdev.PARAMETERS = [];
   catdev.TRANSITIONS = [];
   
   let devid = dev.deviceId;
   let devname = dev.name;
   let devlabel = dev.label;
   for (let comp of dev.components) {
      console.log("DEVICE ",devid,"COMPONENT",comp);
      if (comp.id != 'main') continue;
      for (let capid of comp.capabilities) {
         let cap = await findCapability(user,capid);
         console.log("FOUND CAPABILITY",capid,cap);
         if (cap != null) {
            addCapabilityToDevice(catdev,cap);
          }
       }
    }
}



async function setupLocations(user)
{
   let client = user.client;
   let locs = await client.locations.list();
   
   for (let loc of locs) {
      user.locations[loc.locationId] = loc;
      let rooms = await client.rooms.list(loc.locationId);
      for (let room of rooms) {
         room.locationName = loc.name;
         user.rooms[room.roomId] = room;
       }
    }
}



async function findCapability(user,capid)
{
   if (skip_capabilities.has(capid.id)) return null;
   
   let key = capid.id + "_" + capid.version;
   let cap = capabilities[key];
   
   if (cap == null) {
      let client = user.client;
      let cap = await client.capabilities.get(capid.id,capid.version);
      let present = await client.capabilities.getPresentation(capid.id,capid.version);
      cap.presentation = present;
      capabilities[key] = cap;
    }
      
   if (cap.status != 'live') return null;
   
   return cap;
}


async function addCapabilityToDevice(catdev,cap)
{
   for (let attrname in cap.attributes) {
      let attr = cap.attributes[attrname];
      console.log("ATTR",attr,attr.schema,attr.presentation);
    }
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
