/********************************************************************************/
/*										*/
/*		iqsign.js							*/
/*										*/
/*	Handle RESTful interface for IQSIGN and CATRE				*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
"use strict";


const config = require("./config");





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



function addBridge(authdata)
{
   return false;
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;




/* end of iqsign.js */
