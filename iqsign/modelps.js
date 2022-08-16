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

const pg = pgpromise(config.dbConnect());


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
   
   getAccessToken(bearertoken) { 
      return handleAccessToken(bearertoken);
   }

   getClient(clientid,clientsecret) {
      return handleGetClient(clientid,clientsecret);
   }
   
   getRefreshToken(bearertoken) {
      return handleGetRefresh(bearertoken);
   }
   
   getUser(username,password) {
      return handleGetUser(username,password);
   }
   
   saveAccessToken(token,client,user) {
      return handleSaveAccessToken(token,client,user);
   }
   
}       // end of class ModelPs



/********************************************************************************/
/*                                                                              */
/*      Work methods                                                            */
/*                                                                              */
/********************************************************************************/

function handleAccessToken(bearertoken)
{
   return pg.query(SELECT_ACCESS,[bearertoken])
      .then(handleAccessToken1);
}

function handleAccessToken1(result)
{
   var token = result.rows[0];
   return {
      accessToken: token.access_token,
      client: { id: token.client_id },
      expires: token.expires,
      user: {id: token.userId }, // could be any object
   };
}


function handleGetClient(clientid,clientsecret)
{
   return pg.query(SELECT_CLIENT,[clientid,clientsecret])
      .then(handleGetClient1);
}


function handleGetClient1(result)
{
   var oAuthClient = (result.row_count ? result.rows[0] : false);
   
   if (!oAuthClient) {
      return;
   }
   
   return {
      clientId: oAuthClient.client_id,
      clientSecret: oAuthClient.client_secret,
      grants: ['password'], // the list of OAuth2 grant types that should be allowed
   };
}



function handleGetRefresh(bearertoken)
{
   return pg.query(SELECT_REFRESH,[bearertoken])
      .then(handleGetRefresh1);
}


function handleGetRefresh1(result)
{
   return result.rowCount ? result.rows[0] : false;
}



function handleGetUser(username,password)
{
   return pg.query(SELECT_USER,[username,password])
      .then(handleGetUser1);
}


function handleGetUser1(result)
{
   return result.rowCount ? result.rows[0] : false;
}


function handleSaveAccessToken(token,client,user)
{
   return pg.query(INSERT_ACCESS,[
        token.accessToken, token.accessTokenExpiresOn,
        client.id,
        token.refreshToken,token.refreshTokenExpiresOn,
        user.id ])
      .then(handleSaveAccessToken1);
}


function handleSaveAccessToken1(result)
{
   return result.rowCount ? result.rows[0] : false; // TODO return object with client: {id: clientId} and user: {id: userId} defined
}




/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.Model = ModelPs;
