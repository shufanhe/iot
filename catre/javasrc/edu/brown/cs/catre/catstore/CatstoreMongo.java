/********************************************************************************/
/*										*/
/*		CatstoreMongo.java						*/
/*										*/
/*	Database interface using MongoDB					*/
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




package edu.brown.cs.catre.catstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.bson.BsonArray;
import org.bson.BsonDateTime;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.ClientSession;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreOauth;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;

public class CatstoreMongo implements CatstoreConstants, CatreStore
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreController catre_control;
private CatstoreOauth	oauth_control;
private MongoClient	mongo_client;
private MongoDatabase	catre_database;
private Map<String,CatreTable> known_tables;

private Map<String,CatreSavable> object_cache;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatstoreMongo(CatreController cc)
{
   catre_control = cc;

   String con = "mongodb://USER:PASS@HOST:PORT/catre?maxPoolSize=20&w=majority";

   Properties p = new Properties();
   p.put("mongohost","localhost");
   p.put("mongoport","27017");
   p.put("mongouser","sherpa");
   p.put("mongopass","XXX");

   File f1 = cc.findBaseDirectory();
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"catre.props");
   setProperties(p,f3);

   con = con.replace("USER",p.getProperty("mongouser"));
   con = con.replace("PASS",p.getProperty("mongopass"));
   con = con.replace("HOST",p.getProperty("mongohost"));
   con = con.replace("PORT",p.getProperty("mongoport"));

   mongo_client = MongoClients.create(con);
   catre_database = mongo_client.getDatabase("catre");

   object_cache = new WeakHashMap<>();

   known_tables = new HashMap<>();

   oauth_control = new CatstoreOauth(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public CatreController getCatre()	{ return catre_control; }

@Override public CatreOauth getOauth()		{ return oauth_control; }



/********************************************************************************/
/*										*/
/*	User database operations						*/
/*										*/
/********************************************************************************/

@Override public CatreUser createUser(String name,String email,String pwd)
	throws CatreException
{
   CatreLog.logD("CATSTORE","CREATE USER " + name + " " + email + " " + pwd);

   ClientSession sess = mongo_client.startSession();

   try {
      MongoCollection<Document> uc = catre_database.getCollection("CatreUsers");
      Document userdoc = new Document();
      userdoc.put("USERNAME",name);    // has to be unique
      CatreUser u1 = findUserByEmail(email);
      if (u1 != null) {
         throw new CatreException("Duplicate user/email/universe");
       }
      for (Document doc : uc.find(sess,(Bson) userdoc)) {
         CatreLog.logD("CATSTORE","Duplicate user found " + doc.getString("_id"));
	 throw new CatreException("Duplicate user/email/universe");
       }

      CatreUser user = new CatstoreUser(this,name,email,pwd);

      return user;
    }
   catch (CatreException e) {
      throw e;
    }
   catch (Throwable t) {
      CatreLog.logE("CATSTORE","Problem creating user",t);
      throw new CatreException("Problem creating user",t);
    }
   finally {
      sess.close();
    }
}


@Override public CatreUser findUser(String name,String pwd,String salt)
{
   MongoCollection<Document> uc = catre_database.getCollection("CatreUsers");
   Document userdoc = new Document();
   
   ClientSession sess = mongo_client.startSession();
   try {
      userdoc.put("USERNAME",name);
      for (Document doc : uc.find(sess,userdoc)) {
         String p0 = doc.getString("PASSWORD");
         p0 = p0.replace(' ','+');
         String p1 = p0 + salt;
         String p2 = CatreUtil.secureHash(p1);
         CatreLog.logD("CATSTORE","Password check: " + p0 + " " + p1 + " " +
               p2 + " " + salt);
         CatreLog.logD("CATSTORE","MATCH " + pwd + " " + p2);
         if (p2.equals(pwd)) {
            CatreUser cu = (CatreUser) loadObject(sess,doc.getString("_id"));
            cu.setTemporary(false);
            return cu;
          }
         p0 = doc.getString("TEMP_PASSWORD");
         if (p0 != null) {
            p1 = p0 + salt;
            p2 = CatreUtil.secureHash(p1);
            if (p2.equals(pwd)) {
               CatreUser cu = (CatreUser) loadObject(sess,doc.getString("_id"));
               cu.setTemporary(true);
               return cu;
             }
          }
       }
    }
   finally {
      sess.close();
    }

   return null;
}



@Override public CatreUser findUserByEmail(String email)
{
   MongoCollection<Document> uc = catre_database.getCollection("CatreUsers");
   Document userdoc = new Document();
   
   ClientSession sess = mongo_client.startSession();
   try {
      userdoc.put("EMAIL",email);
      for (Document doc : uc.find(sess,userdoc)) {
         CatreUser cu = (CatreUser) loadObject(sess,doc.getString("_id"));
         return cu;
       }
    }
   finally {
      sess.close();
    }
   
   return null;
}


@SuppressWarnings("unchecked")
<T extends CatreSavable> T findOne(String collection,String fld,String val,T dflt)
{
   MongoCollection<Document> uc = catre_database.getCollection(collection);
   Document querydoc = new Document();
   ClientSession sess = mongo_client.startSession();
   T rslt = dflt;

   querydoc.put(fld,val);
   for (Document doc : uc.find(sess,querydoc)) {
      rslt = (T) loadObject(sess,doc.getString("_id"));
      break;
    }

   sess.close();

   return rslt;
}


void deleteFrom(String collection,String fld,String val)
{
   MongoCollection<Document> uc = catre_database.getCollection(collection);
   Document querydoc = new Document();
   ClientSession sess = mongo_client.startSession();

   querydoc.put(fld,val);
   uc.deleteMany(sess,querydoc);

   sess.close();
}

@Override public List<CatreUser> findAllUsers()
{
   List<CatreUser> rslt = new ArrayList<>();

   MongoCollection<Document> uc = catre_database.getCollection("CatreUsers");
   ClientSession sess = mongo_client.startSession();

   for (Document doc : uc.find(sess)) {
      String id = doc.getString("_id");
      if (id.contains("XXXXXXXX")) continue;
      CatreUser cu = (CatreUser) loadObject(sess,id);
      if (cu.getUniverse() == null) continue;
      rslt.add(cu);
    }

   sess.close();

   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Calendar methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public Boolean validateCalendar(CatreUser cu,String id,String pwd) 
{
   MongoCollection<Document> cc = catre_database.getCollection("CatreCalendars");
   Document userdoc = new Document();
   ClientSession sess = mongo_client.startSession();
   
   try {
      userdoc.put("ID",id);
      for (Document doc : cc.find(sess,userdoc)) {
         Boolean fg = validateKey(cu,id,pwd,doc);
         if (fg == Boolean.TRUE) return true;
         if (fg == Boolean.FALSE) return false;
       }
      // entry for user not found in database 
      
      userdoc.put("KEY",computeKeyPasscode(cu,id,pwd));
      userdoc.put("USERNAME",cu.getUserName());
      userdoc.put("USER",cu.getDataUID());
      cc.insertOne(sess,userdoc);
    }
   finally {
      sess.close();
    }

   return true;
}


private Boolean validateKey(CatreUser cu,String id,String pwd,Document ent) 
{
   String pw = computeKeyPasscode(cu,id,pwd);
   
   String key = ent.getString("KEY");
   
   if (key != null) {
      if (key.equals("*")) return true;
      if (key.equals(pw)) return true;
      return false;
    }
   
   return true;
}



private String computeKeyPasscode(CatreUser cu,String id,String pwd)
{
   String k = CatreUtil.secureHash(CatreUtil.secureHash(pwd) + id);
   
   return k;
}


/********************************************************************************/
/*                                                                              */
/*      Generic methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public CatreSavable loadObject(String uid)
{
   CatreSavable rslt = object_cache.get(uid);
   if (rslt != null) return rslt;

   ClientSession sess = mongo_client.startSession();
   rslt = loadObject(sess,uid);
   sess.close();

   return rslt;
}




@Override public String saveObject(CatreSavable obj)
{
   ClientSession sess = mongo_client.startSession();
   String rslt = saveObject(sess,(CatreSavableBase) obj);
   sess.close();

   return rslt;
}



private String saveObject(ClientSession sess,CatreSavable obj0)
{
    String uid = obj0.getDataUID();
    CatreSavableBase obj = (CatreSavableBase) obj0;
    CatreTable tbl = getTableForObject(obj0);
    if (tbl == null) return null;

    MongoCollection<Document> uc = catre_database.getCollection(tbl.getTableName());
    Document userdoc = createDocument(obj);

    if (obj.isStored()) {
       Document finddoc = new Document();
       finddoc.put("_id",uid);
       uc.replaceOne(sess,finddoc,userdoc);
     }
    else {
       uc.insertOne(sess,userdoc);
       obj.setStored();
     }

    recordObject(obj);

    return uid;
}



private CatreSavable loadObject(ClientSession sess,String uid)
{
   CatreSavable rslt = object_cache.get(uid);
   if (rslt != null) return rslt;

   CatreTable tbl = getTableForUID(uid);
   if (tbl == null) return null;

   Document finddoc = new Document();
   finddoc.put("_id",uid);

   MongoCollection<Document> uc = catre_database.getCollection(tbl.getTableName());
   for (Document doc : uc.find(sess,(Bson) finddoc)) {
      CatreSavable obj = tbl.create(this,doc);
      recordObject(obj);
      if (obj != null) return obj;
    }

   return null;
}


@Override public void recordObject(CatreSavable obj)
{
   String uid = obj.getDataUID();
   object_cache.put(uid,obj);
}


@Override public void removeObject(String uid)
{
   if (uid == null) return;
   CatreSavableBase os = (CatreSavableBase) object_cache.remove(uid);
   if (os != null && !os.isStored()) return;

   ClientSession sess = mongo_client.startSession();

   CatreTable tbl = getTableForUID(uid);
   if (tbl == null) return;

   MongoCollection<Document> uc = catre_database.getCollection(tbl.getTableName());
   Document finddoc = new Document();
   finddoc.put("_id",uid);
   uc.deleteOne(sess,finddoc);

   sess.close();
}



/********************************************************************************/
/*										*/
/*	Table management methods						*/
/*										*/
/********************************************************************************/

@Override public void register(CatreTable tbl)
{
   known_tables.put(tbl.getTablePrefix(),tbl);
}


private CatreTable getTableForObject(CatreSavable cs)
{
   String uid = cs.getDataUID();
   int idx = uid.indexOf("_");
   if (idx > 0) {
      String pfx = uid.substring(0,idx+1);
      CatreTable tbl = known_tables.get(pfx);
      if (tbl != null) return tbl;
    }

   for (CatreTable ct : known_tables.values()) {
      if (ct.useFor(cs)) return ct;
    }

   for (CatreTable ct : known_tables.values()) {
      if (uid.startsWith(ct.getTablePrefix())) return ct;
    }

   return null;
}


private CatreTable getTableForUID(String uid)
{
   int idx = uid.indexOf("_");
   if (idx > 0) {
      String pfx = uid.substring(0,idx+1);
      CatreTable tbl = known_tables.get(pfx);
      if (tbl != null) return tbl;
    }

   for (CatreTable ct : known_tables.values()) {
      if (uid.startsWith(ct.getTablePrefix())) return ct;
    }														

   return null;
}





/********************************************************************************/
/*										*/
/*	Property management							*/
/*										*/
/********************************************************************************/

private void setProperties(Properties p,File dbf)
{
   if (dbf.exists()) {
      try (FileInputStream fis = new FileInputStream(dbf)) {
	 p.loadFromXML(fis);
       }
      catch (IOException e) {
	 // handle or ignore error
       }
    }
}


private Document createDocument(CatreSavableBase obj)
{
   Map<String,Object> jobj = obj.toJson();
   String uid = obj.getDataUID();
   if (uid == null) return null;
   for (Map.Entry<String,Object> ent : jobj.entrySet()) {
      Object val = ent.getValue();
      if (val instanceof Long) {
	 Long lval = (Long) val;
	 Object val1 = new BsonInt64(lval.longValue());
	 ent.setValue(val1);
       }
      else if (val instanceof Date) {
	 Date dval = (Date) val;
	 Object val1 = new BsonDateTime(dval.getTime());
	 ent.setValue(val1);
       }
      else if (val instanceof String []) {
	 String [] vala = (String []) val;
	 BsonArray barr = new BsonArray(vala.length);
	 for (String s : vala) {
	    barr.add(new BsonString(s));
	  }
	 ent.setValue(barr);
       }
    }

   jobj.put("_id",uid);
   Document doc = new Document(jobj);

   return doc;
}





}	// end of class CatstoreMongo




/* end of CatstoreMongo.java */

