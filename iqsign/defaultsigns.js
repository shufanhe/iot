/********************************************************************************/
/*										*/
/*		defaultsigns.js 						*/
/*										*/
/*	Load/update default sign definitions					*/
/*										*/
/********************************************************************************/

const db = require("./database");
const config = require("./config");
const fs = require("fs/promises");


/********************************************************************************/
/*										*/
/*	Local variables 							*/
/*										*/
/********************************************************************************/

var last_update = 0;


/********************************************************************************/
/*										*/
/*	Update default signs on startup 					*/
/*										*/
/********************************************************************************/

async function update()
{
   await doUpdate();
}



async function doUpdate()
{
   let fn = config.getDefaultSignsFile();
   let stat = await fs.stat(fn);
   let dlm = stat.mtimeMs;
   if (last_update > dlm) return;
   let cnts = await fs.readFile(fn,'utf8');
   let cnts1 = cnts.toString();
   let lines = cnts1.split("\n");
   let name = null;
   let body = null;
   for (let i = 0; i < lines.length; ++i) {
      let line = lines[i].trim();
      if (line == '') continue;
      if (line.startsWith('=')) {
	 if (body != null) {
	    await saveSign(name,body,dlm);
	    body = null;
	  }
	 name = line.substring(1);
       }
      else {
	 if (body == null) body = "";
	 body += line + "\n";
       }
    }
   if (body != null) {
      await saveSign(name,body,dlm);
      body = null;
    }
}


async function saveSign(name,body,dlm)
{
   console.log("SAVE SIGN",name,body,dlm);

   let rows0 = await db.query("SELECT * FROM iQsignDefines WHERE name = $1 and userid IS NULL",
	 [name]);
   if (rows0.length == 0) {
      await db.query("INSERT INTO iQsignDefines (id, userid, name, contents) " +
	    "VALUES ( DEFAULT, NULL, $1, $2 )",
	    [ name, body ]);
      rows0 = await db.query("SELECT * FROM iQsignDefines WHERE name = $1 and userid IS NULL",
	       [name]);
      console.log("SELECT",rows0);
    }
   else {
      let r = rows0[0];
      console.log("DATE COMPARE ",dlm,r.lastupdate);
      await db.query("UPDATE iQsignDefines SET contents = $1 WHERE id = $2",
	    [ body.trim(), r.id ]);
    }
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.update = update;




/* end of module defaultsigns */
