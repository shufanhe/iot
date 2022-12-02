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
   let rslt = null;
   
   console.log("SEND TO CATRE",json);
   
   try {
      let sock = new net.socket( { allowHalfOpen: true, readable: true, writable: true });
      let psock = new PromiseSocket(sock);
      await psock.connect(config.SOCKET_PORT);
      await psock.writeAll(JSON.stringify(json));
      await psock.end();
      let data = await psock.readAll();
      rslt = JSON.parse(data);
      console.log("RECIEVED BACK",data);
      sock.destroy();
    }
   catch (e) {
      console.log("ERROR SENDING",e);
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
