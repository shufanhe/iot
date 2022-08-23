/********************************************************************************/
/*										*/
/*		database.js							*/
/*										*/
/*	Database access for iQsign						*/
/*										*/
/********************************************************************************/
"use strict";

/********************************************************************************/
/*										*/
/*	Imports 								*/
/*										*/
/********************************************************************************/

var pgpromise = require("pg-promise")();
var os = require("os");
var util = require("util");

var config = require("./config.js");




/********************************************************************************/
/*										*/
/*	Variables								*/
/*										*/
/********************************************************************************/

var connect_string = config.dbConnect();

const pg = pgpromise(connect_string);

console.log("CONNECTING TO " + connect_string);


/********************************************************************************/
/*										*/
/*	Local callback function generator					*/
/*										*/
/********************************************************************************/

function logDbQuery(q,prms)
{
   if (prms == undefined || prms instanceof Function) console.log("DB","QUERY0",q);
   else console.log("DB","QUERY",q,prms);
}



/********************************************************************************/
/*										*/
/*	Query access								*/
/*										*/
/********************************************************************************/

async function query1(q,prms)
{
   return query(q,prms,"one");
}


async function query0(q,prms)
{
   return query(q,prms,"none");
}


async function query01(q,prms)
{
   return query(q,prms,"one?");
}


async function queryN(q,prms)
{
   return query(q,prms,"many");
}


async function query(q,prms,typ)
{
   logDbQuery(q,prms);
   
   q = fixQuery(q);
   
   if (typ != null) {
      switch (typ) {
         case "one" :
            typ = pgpromise.queryResult.one;
            break;
         case "many" :
            typ = pgpromise.queryResult.many;
            break;
         case "none" :
            typ = pgpromise.queryResult.none;
         case "any" :
            typ = pgpromise.queryResult.any;
            break;
         case "one?" :
            typ = pgpromise.queryResult.none + pgpromise.queryResult.one;
            break;
       }
    }

   let rslt = pg.query(q,prms,typ);
  
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Handle mysql - postgresql differences on parameters			*/
/*										*/
/********************************************************************************/

function fixQuery(q)
{
   if (connect_string.substring(0,5) == "mysql") {
      q = q.replace(/\$\d+/g,"?");
    }

   return q;
}




/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.query = query;
exports.query0 = query0;
exports.query1 = query1;
exports.query01 = query01;
exports.queryN = queryN;




/* end of database.js */
