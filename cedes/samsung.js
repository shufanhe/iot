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
{
   let user = users[uid];
   if (user == null) {
      console.log("COMMAND: USER NOT FOUND",uid);
      return;
    }
   for (let dev of user.devices) {
      if (dev.UID == devid) {
         for (let t of dev.TRANSITIONS) {
            if (t.NAME == command) {
               return await processCommand(user,dev,t,command,values);
             }
          }
         break;
       }
    }
}



async function processCommand(user,dev,trans,command,values)
{
   let cmdarg = { component: trans.componentid, capability: trans.capabilityid, 
         command: command, "arguments" : values };
 
   let client = user.client;
   let resp = await client.devices.executeCommand(dev.UID,cmdarg);
   
   console.log("EXECUTE COMMAND",dev,trans,cmdarg,resp);
   
   return resp;
}




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
         console.log("FOUND CAPABILITY",capid,JSON.stringify(cap,null,3));
         if (cap != null) {
            cap.componentid = comp.id;
            await addCapabilityToDevice(catdev,cap);
          }
       }
    }
   
   console.log("RESULT DEVICE",JSON.stringify(catdev,null,3));
   
   if (catdev != null) {
      user.devices.push(catdev);
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
      cap = await client.capabilities.get(capid.id,capid.version);
      try {
         let present = await client.capabilities.getPresentation(capid.id,capid.version);
         cap.presentation = present;
       }
      catch (e) {
         cap.presentation = null;
       }
      capabilities[key] = cap;
      console.log("CREATE CAPABILITY",JSON.stringify(cap,null,3));
    }
      
   if (cap.status != 'live' && cap.status != 'proposed') return null;
   
   return cap;
}


async function addCapabilityToDevice(catdev,cap)
{
   for (let attrname in cap.attributes) {
      let attr = cap.attributes[attrname];
      let param = await getParameter(attrname,attr);
      console.log("WORK ON PARAMETER",attrname,JSON.stringify(attr,null,3),param);
      if (param != null) {
         console.log("ADD PARAMETER",param);
         catdev.PARAMETERS.push(param);
       }
    }
   for (let cmdname in cap.commands) {
      let cmd = cap.commands[cmdname];
      cmd.comopnentid = cap.componentid;
      cmd.capabilityid = cap.id;
      console.log("LOOK AT COMMAND",cmdname,JSON.stringify(cmd,null,3));
      await addCommandToDevice(catdev,cmdname,cmd);
    }
   
}


async function getParameter(pname,attr)
{
   let param = { NAME: pname, ISSENSOR: true };
   let schema = attr.schema;
   let type = schema.type;
   let props = schema.properties;
   if (props == null) return null;
   let value = props.value;
   let enm = value["enum"];
   let vtype = value.type;
   let cmds = attr.enumCommands;
   if (cmds != null && cmds.length > 0) return;
   let unit = props.unit;
   if (unit != null) {
      let units = unit["enum"];
      let dunit = unit.default;
      param.UNITS = units;
      param.DEFAULT_UNIT = dunit;
    }
   
   // need to handle arrays
   // need to handle set/enum
   // need to handle units on numbers
   switch (type) {
      case "object" :
         if (enm != null) {
            param.TYPE = 'ENUM';
            param.VALUES = enm;
          }
         else switch (vtype) {
            case 'integer' :
               let min = value.minimum;
               let max = value.maximum;
               param.TYPE = 'INTEGER';
               if (min != null) param.MIN = min;
               if (max != null) param.MAX = max;
               break;
            case 'number' :
               let min1 = value.minimum;
               let max1 = value.maximum;
               param.TYPE = 'REAL';
               if (min1 != null) param.MIN = min1;
               if (max1 != null) param.MAX = max1;
               break;
            case 'string' :
               if (pname == 'color') {
                  param.TYPE = 'COLOR';
                }
               else {
                  param.TYPE = 'STRING';
                }
               break;
            case 'array' :
               let items = value.items;
               if (items == null) return null;
               if (items.type == 'string' && items["enum"] != null) {
                  param.TYPE= 'SET';
                  param.values = items["enum"];
                }
               else if (items.type == 'string') {
                  param.ISSENSOR = false;
                  param.TYPE = 'STRINGLIST';
                }
               else {
                 // this can be useful as a non-sensor list of supported items (e.g. signs)
                  console.log("UNKNOWN array/set type",items);
                }
               break;
            default :
               console.log("UNKNOWN value type",vtype);
               break;
          }
         break;
      default :
         console.log("UNKNOWN schema type",type);
         break;
    }
   
   if (param.TYPE == null) return null;
   
   return param;
}



/********************************************************************************/
/*                                                                              */
/*      Commands definitions                                                    */
/*                                                                              */
/********************************************************************************/

async function addCommandToDevice(catdev,cmdname,cmd)
{
   let cattrans = {  };
   if (cmd.name != null) cattrans.NAME = cmd.name;
   else cmd.NAME = cmdname;
   
   let params = [ ];
   for (let arg of cmd.arguments) {
      console.log("WORK ON ARG",JSON.stringify(arg,null,3));
      let p = await getCommandParameter(arg);
      console.log("YIELD PARAMTER",JSON.stringify(p,null,3));
      if (p == null) return null;
      params.push(p);   
    }
   
   cattrans.componentid = cmd.componentid;
   cattrans.capabilityid = cmd.capabilityid;
   
   cattrans.DEFAULTS = params;
   catdev.TRANSITIONS.push(cattrans);
   
   console.log("CREATE TRANSITION",cattrans,JSON.stringify(catdev,null,3));
}


async function getCommandParameter(arg)
{
   let param = { NAME : arg.name };
   
   let schema = arg.schema;  
   if (schema == null) return null;
   if (schema.title !=  null) param.LABEL = schema.title;
         
   switch (schema.type) {
      case 'string' :
         if (schema["enum"] != null) {
            let enm = schema["enum"];
            param.TYPE = 'ENUM';
            param.VALUES = enm;
          }
         else {
            // might need to get values from presentation
            param.TYPE = 'STRING';
          }
         break;
      case 'integer' :
         let min = schema.minimum;
         let max = schema.maximum;
         param.TYPE = 'INTEGER';
         if (min != null) param.MIN = min;
         if (max != null) param.MAX = max;
         break;
      default :
         console.log("UNKNOWN command parameter type",JSON.stringify(arg,null,3));
         break;
    }
   
   if (param.TYPE == null) return null;
   
   return param;   
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
