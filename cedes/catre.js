/********************************************************************************/
/*                                                                              */
/*              catre.js                                                        */
/*                                                                              */
/*      Handle RESTful interface for CATRE requests                             */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/


"use strict";


const net = require('net');

const config = require("./config");
const { PromiseSocket } = require("promise-socket");


/********************************************************************************/
/*                                                                              */
/*      Socket to CATRE                                                         */
/*                                                                              */
/********************************************************************************/

async function sendToCatre(json)
{
   let rslt = null;
   
   console.log("SEND TO CATRE",json);
   
   try {
      let sock = new net.Socket( { allowHalfOpen: true, readable: true, writable: true });
      let psock = new PromiseSocket(sock);
      await psock.connect(config.SOCKET_PORT);
      await psock.writeAll(JSON.stringify(json));
      await psock.end();
      let data = await psock.readAll();
      rslt = JSON.parse(data);
      console.log("RECEIVED BACK FROM CATRE: ",rslt);
      sock.destroy();
    }
   catch (e) {
      console.log("ERROR SENDING",e);
      rslt = null;
    }
   
   return rslt;
}





/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.sendToCatre = sendToCatre;



/* end of catre.js */
