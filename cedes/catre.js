/********************************************************************************/
/*										*/
/*		catre.js							*/
/*										*/
/*	Handle RESTful interface for CATRE requests				*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
"use strict";


const net = require('net');

const config = require("./config");
const { PromiseSocket } = require("promise-socket");


/********************************************************************************/
/*										*/
/*	Socket to CATRE 							*/
/*										*/
/********************************************************************************/

async function sendToCatre(json)
{
   let psock = null;
   let rslt = null;
   
   try {
      let sock = new net.socket( { allowHalfOpen: true, readable: true, writable: true });
      psock = new PromiseSocket(sock);
      await psock.connet(config.SOCKET_PORT,config.SOCKET_HOST);
      await psock.writeAll(JSON.stringify(json));
      let data = await psock.readAll();
      rslt = JSON.parse(data);
      sock.destroy();
    }
   catch (e) {
      rslt = null;
    }
   
   return rslt;
}





/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.sendToCatre = sendToCatre;



/* end of catre.js */
