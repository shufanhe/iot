/********************************************************************************/
/*                                                                              */
/*              modelps.js                                                      */
/*                                                                              */
/*      Oauth server model using postgres                                       */
/*                                                                              */
/*      Written by spr, based on express-oauth-server                           */
/*                                                                              */
/********************************************************************************/

const pgpromise = require('pg-promise')();

const config = require('./config');

const db = require("./database");



/********************************************************************************/
/*                                                                              */
/*      Constants                                                               */
/*                                                                              */
/********************************************************************************/

const SELECT_ACCESS = 'SELECT access_token, access_token_expires_on, client_id, refresh_token, refresh_token_expires_on, user_id ' +
      'FROM OauthTokens WHERE access_token = $1';

const SELECT_CLIENT = 'SELECT client_id, client_secret, redirect_uri FROM OauthClients WHERE client_id = $1 AND client_secret = $2';

const SELECT_REFRESH = 'SELECT access_token, access_token_expires_on, client_id, refresh_token, refresh_token_expires_on, user_id ' + 
      ' FROM OauthTokens WHERE refresh_token = $1';
  
const SELECT_USER = 'SELECT id FROM OauthUsers WHERE username = $1 AND password = $2';

const INSERT_ACCESS = 'INSERT INTO OauthTokens(access_token, access_token_expires_on, client_id, refresh_token, refresh_token_expires_on, user_id) ' + 
      'VALUES ($1, $2, $3, $4)';


/********************************************************************************/
/*                                                                              */
/*      Set up class                                                            */
/*                                                                              */
/********************************************************************************/

class ModelPs {

   constructor() { }
   
   async getAccessToken(bearertoken) { 
      return handleAccessToken(bearertoken);
   }

   async getClient(clientid,clientsecret) {
      return handleGetClient(clientid,clientsecret);
   }
   
   async getRefreshToken(bearertoken) {
      return handleGetRefresh(bearertoken);
   }
   
   async getUser(username,password) {
      return handleGetUser(username,password);
   }
   
   async saveAccessToken(token,client,user) {
      return handleSaveAccessToken(token,client,user);
   }
   
}       // end of class ModelPs



/********************************************************************************/
/*                                                                              */
/*      Work methods                                                            */
/*                                                                              */
/********************************************************************************/

async function handleAccessToken(bearertoken)
{
   let token = db.query1(SELECT_ACCESS,[bearertoken]);
   return {
      accessToken: token.access_token,
      client: { id: token.client_id },
      expires: token.expires,
      user: {id: token.userId }, // could be any object
   };
}


async function handleGetClient(clientid,clientsecret)
{
   let rows = await db.query(SELECT_CLIENT,[clientid,clientsecret]);
   if (rows.length == 0) return;
   let oauthclient = rows[0];
   return {
      clientId: oauthcient.client_id,
      clientSecret: oauthclient.client_secret,
      grants: ['password'], // the list of OAuth2 grant types that should be allowed
   };
}



async function handleGetRefresh(bearertoken)
{
   let rows = await db.query(SELECT_REFRESH,[bearertoken]);
   if (rows.length == 0) return false;
   return rows[0];
}



async function handleGetUser(username,password)
{
   let rows = await db.query(SELECT_USER,[username,password]);
   if (rows.length == 0) return false;
   return rows[0];
}


async function handleSaveAccessToken(token,client,user)
{
   let rows = await db.query(INSERT_ACCESS,[
        token.accessToken, token.accessTokenExpiresOn,
        client.id,
        token.refreshToken,token.refreshTokenExpiresOn,
        user.id ]);
   if (rows.length == 0) return false;
   return rows[0];
  // TODO return object with client: {id: clientId} and user: {id: userId} defined
}




/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.Model = ModelPs;
