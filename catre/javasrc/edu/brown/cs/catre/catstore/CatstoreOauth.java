/********************************************************************************/
/*                                                                              */
/*              CatstoreOauth.java                                              */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreJson;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreOauth;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUser;

class CatstoreOauth implements CatreOauth, CatstoreConstants, CatreJson
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatstoreMongo   use_store;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatstoreOauth(CatstoreMongo store)
{
   use_store = store;
   use_store.register(new TokensTable());
   use_store.register(new CodesTable());
}


/********************************************************************************/
/*                                                                              */
/*      Token methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public JSONObject getToken(JSONObject data) throws CatreException
{
   String token = data.getString("token");
   OauthTokenData otd = null;
   otd = use_store.findOne("CatreOauthTokens","accessToken",token,otd);
   if (otd == null) throw new CatreException("Token not found");
   
   return otd.getJsonObject();
}


@Override public JSONObject saveToken(JSONObject data)
{
   JSONObject token = data.getJSONObject("token");
   JSONObject client = data.getJSONObject("client");
   JSONObject user = data.getJSONObject("user");
   OauthTokenData otd = new OauthTokenData(token,client,user);
   
   use_store.deleteFrom("CatreOauthTokens","accessToken",token.getString("accessToken"));
   use_store.saveObject(otd);
   
   return otd.getJsonObject();
}


@Override public JSONObject revokeToken(JSONObject data)
{
   JSONObject token = data.getJSONObject("token");
   use_store.deleteFrom("CatreOauthTokens","refreshToken",token.getString("refreshToken"));
   return buildJson("STATUS","OK"); 
}


@Override public JSONObject getRefreshToken(JSONObject data)
{
   OauthTokenData token = null;
   token = use_store.findOne("CatreOauthTokens","refreshToken",
         data.getString("token"),token);
   return token.getRefreshData();
}



/********************************************************************************/
/*                                                                              */
/*      Code methods                                                            */
/*                                                                              */
/********************************************************************************/


@Override public JSONObject saveCode(JSONObject data)
{
   JSONObject code = data.getJSONObject("code");
   JSONObject client = data.getJSONObject("client");
   JSONObject user = data.getJSONObject("user");
    
   use_store.deleteFrom("CatreOauthCodes","authorizationCode",code.getString("authorizationCode"));
   OauthCodeData ocd = new OauthCodeData(code,client,user);
   use_store.saveObject(ocd);
   
   return ocd.getJsonObject();
}


@Override public JSONObject getCode(JSONObject data) throws CatreException
{
   String code = data.getString("code");
   OauthCodeData ocd = null;
   ocd = use_store.findOne("CatreOauthCodes","authorizationCode",code,ocd);
   if (ocd == null) throw new CatreException("Code not found");
   
   return ocd.getJsonObject();
}


@Override public JSONObject revokeCode(JSONObject data)
{
   String code = data.optString("code");
   if (code == null || code.startsWith("{")) {
      JSONObject cobj = data.getJSONObject("code");
      code = cobj.optString("code");
      if (code == null) code = cobj.getString("authorizationCode");
    }
   
   use_store.deleteFrom("CatreOauthCodes","authorizationCode",code);
   
   return buildJson("STATUS","OK"); 
}


@Override public JSONObject verifyScope(JSONObject data)
{
   return buildJson("STATUS","OK"); 
}



/********************************************************************************/
/*                                                                              */
/*      Login methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public JSONObject handleLogin(JSONObject data)
{
   String user = data.getString("username");
   String token = data.getString("accesstoken");
   String pwd = data.getString("password");
   String salt = data.getString("padding");
   
   CatreUser cu = use_store.findUser(user,pwd,salt);
   if (cu == null) return buildJson("STATUS","ERROR","MESSAGE","Bad username/password");
   
   Map<String,String> auth = new HashMap<>();
   auth.put("token",token);
   cu.addAuthorization("smartthings",auth);
   
   CatreLog.logI("CATSTORE","Oauth setup for " + user + " " + cu.getDataUID() + " " +
         token);
   
   return buildJson("id",cu.getDataUID(),"username",user);
}




/********************************************************************************/
/*                                                                              */
/*      Token data object                                                       */
/*                                                                              */
/********************************************************************************/

private static class OauthTokenData extends CatreSavableBase {
   
   private String access_token;
   private Date access_expires_at;
   private String refresh_token;
   private Date refresh_expires_at;
   private String use_scope;
   private String client_id;
   private String user_id;
   
   OauthTokenData(CatreStore store,Map<String,Object> map) {
      super(store,map);
    }
   
   OauthTokenData(JSONObject tok,JSONObject client,JSONObject user) {
      super("OAUTHTOKEN_");
      access_token = tok.getString("accessToken");
      access_expires_at = (Date) tok.get("accessTokenExpiresAt");
      refresh_token = tok.getString("refreshToken");
      refresh_expires_at = (Date) tok.get("refreshTokenExpiresAt");
      use_scope = tok.getString("scope");
      client_id = client.getString("id");
      user_id = user.getString("id");
    }
   
   JSONObject getRefreshData() {
      JSONObject rslt = new JSONObject();
      rslt.put("refreshToken",refresh_token);
      rslt.put("refreshTokenExpiresAt",refresh_expires_at);
      rslt.put("scope",use_scope);
      rslt.put("client", buildJson("id",client_id));
      rslt.put("user",buildJson("id",user_id));
      return rslt;
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("accessToken",access_token);
      rslt.put("accessTokenExpiresAt", access_expires_at);
      rslt.put("refreshToken",refresh_token);
      rslt.put("refreshTokenExpiresAt",refresh_expires_at);
      rslt.put("scope",use_scope);
      rslt.put("client", buildJson("id",client_id));
      rslt.put("user",buildJson("id",user_id));
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      access_token = getSavedString(map,"accessToken",access_token);
      access_expires_at = getSavedDate(map,"accssTokenExpiresAt",access_expires_at); 
      refresh_token = getSavedString(map,"refreshToken",refresh_token);
      refresh_expires_at = getSavedDate(map,"refreshTokenExpiresAt",refresh_expires_at);
      use_scope = getSavedString(map,"scope",use_scope);
      Map<String,Object> c = getSavedJson(map,"client",null);
      if (c == null) client_id = null;
      else client_id = (String) c.get("id");
      Map<String,Object> u = getSavedJson(map,"user",null);
      if (u == null) user_id = null;
      else user_id = (String) u.get("id");
    }
   
}       // end of inner class OauthTokenData


 


/********************************************************************************/
/*                                                                              */
/*      Code data object                                                        */
/*                                                                              */
/********************************************************************************/

private static class OauthCodeData extends CatreSavableBase {

   private String access_code;
   private Date expires_at;
   private String redirect_uri;
   private String use_scope;
   private String client_id;
   private String user_id;
   
   OauthCodeData(CatreStore store,Map<String,Object> map) {
      super(store,map);
    }
   
   OauthCodeData(JSONObject code,JSONObject client,JSONObject user) {
      super("OAUTHCODE_");
      access_code = code.getString("authorizationCode");
      expires_at = (Date) code.get("expiresAt");
      redirect_uri = code.getString("redirectUri");
      use_scope = code.getString("scope");
      client_id = client.getString("id");
      user_id = user.getString("id");
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("authorizationCode",access_code);
      rslt.put("expiresAt",expires_at);
      rslt.put("redirectUri",redirect_uri);
      rslt.put("scope",use_scope);
      rslt.put("client", buildJson("id",client_id));
      rslt.put("user",buildJson("id",user_id));
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      access_code = getSavedString(map,"authorizationCode",access_code);
      expires_at = getSavedDate(map,"expiresAt",expires_at);
      redirect_uri = getSavedString(map,"redirect_uri",redirect_uri);
      use_scope = getSavedString(map,"scope",use_scope);
      Map<String,Object> c = getSavedJson(map,"client",null);
      if (c == null) client_id = null;
      else client_id = (String) c.get("id");
      Map<String,Object> u = getSavedJson(map,"user",null);
      if (u == null) user_id = null;
      else user_id = (String) u.get("id");
    }

}       // end of inner class OauthTokenData




/********************************************************************************/
/*                                                                              */
/*      MONGO table definitions                                                 */
/*                                                                              */
/********************************************************************************/

private static class TokensTable implements CatreTable {

   @Override public String getTableName()       { return "CatreOauthTokens"; }
   
   @Override public String getTablePrefix()     { return "OAUTHTOKEN_"; }
   
   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof OauthTokenData;
    }
   
   @Override public OauthTokenData create(CatreStore store,Map<String,Object> data) {
      return new OauthTokenData(store,data);
    }
   
}       // end of inner class TokensTable




private static class CodesTable implements CatreTable {

@Override public String getTableName()       { return "CatreOauthCodes"; }

@Override public String getTablePrefix()     { return "OAUTHCODE_"; }

@Override public boolean useFor(CatreSavable cs) {
   return cs instanceof OauthCodeData;
}

@Override public OauthCodeData create(CatreStore store,Map<String,Object> data) {
   return new OauthCodeData(store,data);
}

}       // end of inner class TokensTable



}       // end of class CatstoreOauth




/* end of CatstoreOauth.java */

