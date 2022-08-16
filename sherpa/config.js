/********************************************************************************/
/*										*/
/*		config.js							*/
/*										*/
/*	Constants for upod front end						*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Imports 								*/
/*										*/
/********************************************************************************/

const fs = require('fs');



/********************************************************************************/
/*										*/
/*	Connection constants							*/
/*										*/
/********************************************************************************/

const PORT = 3333;
const HTTPS_PORT = 3334;

const DATABASE = "postgres://spr:XXXXXX@db.cs.brown.edu/smartsign";
const PWD_FILE = "/.dbpassword";

const STATIC = '/web/';


function dbConnect()
{
   let pwd = fs.readFileSync(__dirname + PWD_FILE);
   pwd = pwd.toString().trim();
   let conn = DATABASE.replace("XXXXXX",pwd);
   return conn;
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.PORT = PORT;
exports.HTTPS_PORT = HTTPS_PORT;
exports.STATIC = STATIC;
exports.dbConnect = dbConnect;



/* end of module config */
