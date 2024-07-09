HANDL/********************************************************************************/
/*										*/
/*		samsung.js							*/
/*										*/
/*	Interface to Smartthings using core-sdk 				*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

"use strict";

const {SmartThingsClient,BearerTokenAuthenticator} = require('@smartthings/core-sdk');

const config = require("./config");
const catre = require("./catre");


/********************************************************************************/
/*										*/
/*	Global storage								*/
/*										*/
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
   console.log("SAMSUNG ADD BRIDGE",authdata.uid,authdata.token,bid);

   let username = authdata.uid;
   let pat = authdata.token;
   let user = users[username];
   if (user == null) {
      let client = new SmartThingsClient(new BearerTokenAuthenticator(pat));
      user = { username: username, client: client, bridgeid: bid,
	    devices: [], locations: { }, rooms: { } };
      users[username] = user;
    }
   else {
      user.bridgeid = bid;
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


async function handleActiveSensors(bid,uid,active)
{
   let user = users[uid];
   if (user == null) {
      console.log("COMMAND: USER NOT FOUND",uid);
      return;
    }
   for (let param of active) {
      let devid = param.DEVICE;
      let param = param.PARAMETER;
      // TODO: note that this device/parameter is active
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



async function handleParameters(bid,uid,devid,parameters)
{
   let user = users[uid];
   if (user == null) {
      console.log("COMMAND: USER NOT FOUND",uid);
      return;
    }

   let rslt = await getParameterValues(devid,parameters);

   return rslt;
}


async function getParameterValues(user,devid,parameters = null)
{
   let client = user.client;
   let rslt = { };
   let sts = await client.devices.getStatus(devid);
   console.log("DEVICE STATUS",devid,sts);
   for (let compname in sts.components) {
      let compstatus = sts.components[compname];
      for (let attrname in compstatus) {
	 let capstatus = compstatus[attrname];
	 console.log("CAPABILITY STATUS",capstatus);
	 for (let aname in capstatus) {
	    if (parameters == null || parameters.includes(aname)) {
	       let attrstate = capstatus[aname];
	       rslt[aname] = attrstate;
	     }
	  }
       }
    }

   return rslt;
}


async function updateValues(user,devid)
{
   let rslt = await getParameterValues(user,devid);
   for (let p in rslt) {
      let event = {
	    TYPE: "PARAMETER",
	    DEVICE: devid,
	    PARAMETER: p,
	    VALUE: rslt[p].value
       };
      await catre.sendToCatre({ command: "EVENT",
       bid: user.bridgeid,
       event: event });
    }
}



/********************************************************************************/
/*										*/
/*	List devices								*/
/*										*/
/********************************************************************************/

async function getDevices(username)
{
   let user = users[username];
   let client = user.client;
   await setupLocations(user);

   user.devices = [];
   let devs = await client.devices.list();
   console.log("FOUND DEVICES",devs.length,devs);
   for (let dev of devs) {
      console.log("WORK ON DEVICE",dev.deviceId);
      await defineDevice(user,dev);
    }

   console.log("OUTPUT DEVICES",user.devices.length);

   let msg = { command: "DEVICES", uid: user.username, bridge: "samsung",
	 bid: user.bridgeid, devices: user.devices };
   await catre.sendToCatre(msg);

   for (let dev of devs) {
      await updateValues(user,dev.deviceId);
    }
}


async function defineDevice(user,dev)
{
   let catdev = { };
   catdev.UID = dev.deviceId;
   catdev.BRIDGE = "samsung";
   catdev.PARAMETERS = [];
   catdev.TRANSITIONS = [];
   catdev.NAME = dev.name;
   catdev.LABEL = dev.label;
   catdev.DESCRIPTION = dev.name + ":" + dev.label;

   console.log("BEGIN DEVICE",dev.deviceId);

   let client = user.client;
   catdev.presentation = await client.devices.getPresentation(dev.deviceId);
   catdev.capabilities = [];

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

   console.log("RESULT DEVICE",dev.deviceId,JSON.stringify(catdev,null,3));

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
   catdev.capabilities.push(cap);

   for (let attrname in cap.attributes) {
      let attr = cap.attributes[attrname];
      let param = await getParameter(attrname,attr);
      console.log("WORK ON PARAMETER",attrname,JSON.stringify(attr,null,3),param);
      if (param != null) {
	 param.capabilityid = cap.id;
         param.componentid = cap.componentid;
	 console.log("ADD PARAMETER",param);
	 catdev.PARAMETERS.push(param);
       }
    }
   for (let cmdname in cap.commands) {
      let cmd = cap.commands[cmdname];
      cmd.componentid = cap.componentid;
      cmd.capabilityid = cap.id;
      console.log("LOOK AT COMMAND",cmdname,JSON.stringify(cmd,null,3));
      await addCommandToDevice(catdev,cmdname,cmd);
    }

}


async function getParameter(pname,attr)
{
   let param = { NAME: pname, ISSENSOR: true };
   param.attr = attr;
   let schema = attr.schema;
   let type = schema.type;
   let props = schema.properties;
   if (props == null) return null;
   let value = props.value;
   if (value.oneOf != null) {
      value = value.oneOf[0];
    }
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

   param = scanSchema(value,param);

   return param;
}



/********************************************************************************/
/*										*/
/*	Commands definitions							*/
/*										*/
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

   cattrans.DEFAULTS = { PARAMETERS: params };
   catdev.TRANSITIONS.push(cattrans);

   console.log("CREATE TRANSITION",cattrans,JSON.stringify(catdev,null,3));
}


async function getCommandParameter(arg)
{
   let param = { NAME : arg.name };

   let schema = arg.schema;
   if (schema == null) return null;

   let oneof = schema.oneOf;
   if (oneof != null) {
      schema = oneof[0];
    }

   if (schema.title !=	null) param.LABEL = schema.title;
	
   param = scanSchema(schema,param);

   return param;
}


/********************************************************************************/
/*										*/
/*	Schema scanning 							*/
/*										*/
/********************************************************************************/

function scanSchema(value,param)
{
   let evals = value["enum"];
   if (evals != null) {
      param.TYPE = 'ENUM';
      param.VALUES = evals;
      return param;
    }

   let type = value.type;

   switch (type) {
      case 'string' :
	 if (param.NAME == 'color') param.TYPE = 'COLOR'
	 else param.TYPE = 'STRING';
	 break;
      case 'integer' :
	 let min = value.minimum;
	 let max = value.maximum;
	 param.TYPE = 'INTEGER';
	 if (min != null) param.MIN = min;
	 if (max != null) param.MAX = max;
	 break;
      case 'number' :
	 let minr = value.minimum;
	 let maxr = value.maximum;
	 param.TYPE = 'REAL';
	 if (minr != null) param.MIN = minr;
	 if (maxr != null) param.MAX = maxr;
	 break;
      case 'boolean' :
	 param.TYPE = 'BOOLEAN';
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
	    console.log("UNKNOWN array/set type",items,value);
	  }
	 break;
      case 'object' :
	 let flds = [];
	 for (let propname in value.properties) {
	    let prop = value.properties[propname];
	    let pv = { NAME: propname };
	    pv = scanSchema(prop,pv);
	    if (pv != null) flds.push(pv);
	  }
	 param.TYPE = 'OBJECT';
	 param.FIELDS = flds;
	 param.ISSENSOR = false;
	 break;
      default :
	 console.log("Unknown schema type",value);
	 break;
    }

   if (param.TYPE == null) return null;

   return param;
}

/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;



/* end of module samsung */
