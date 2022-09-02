#! /bin/csh -fx

source setup.sql


$run $host $DEFAULTDB << EOF
DROP DATABASE $db;
EOF

$run $host $DEFAULTDB << EOF
CREATE DATABASE $db $ENCODE;
EOF


ENUM SignDim "( '16by9', '4by3', '16by10', 'other' )"



$run $host $db <<EOF

$runcmd

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

DROP TABLE IF EXISTS iQsignUsers CASCADE;
DROP TABLE IF EXISTS iQsignValidator CASCADE;
DROP TABLE IF EXISTS iQsignSigns CASCADE;
DROP TABLE IF EXISTS iQsignImages CASCADE;
DROP TABLE IF EXISTS iQsignDefines CASCADE;
DROP TABLE IF EXISTS iQsignParameters CASCADE;
DROP TABLE IF EXISTS OauthTokens CASCADE;
DROP TABLE IF EXISTS OauthCodes CASCADE;


CREATE TABLE iQsignUsers (
    id $iddeftype  NOT NULL PRIMARY KEY,
    email text NOT NULL,
    username text,
    password text NOT NULL,
    altpassword text,
    logincode text default NULL,
    maxsigns int DEFAULT 3,
    admin bool DEFAULT false,
    valid bool NOT NULL DEFAULT false
$ENDTABLE;
CREATE INDEX UsersEmail ON iQsignUsers ( email );
CREATE INDEX UsersUsername ON iQsignUsers ( username );


CREATE TABLE iQsignValidator (
   userid $idtype NOT NULL,
   validator text NOT NULL,
   timeout $datetime NOT NULL
$ENDTABLE;
CREATE INDEX ValidUser ON iQsignValidator (userid);


$DEF_SignDim;


CREATE TABLE iQsignSigns (
   id $iddeftype NOT NULL PRIMARY KEY,
   userid $idtype NOT NULL,
   name text NOT NULL,
   namekey text NOT NULL,
   lastsign text DEFAULT NULL,
   lastupdate $datetime DEFAULT CURRENT_TIMESTAMP,
   dimension $SignDim DEFAULT '16by9',
   width int DEFAULT 2048,
   height int DEFAULT 1152,
   displayname text,
   FOREIGN KEY (userid) REFERENCES iQsignUsers(id)
$ENDTABLE;
CREATE INDEX SignsUser ON iQsignSigns(userid);
CREATE INDEX SignsUserName ON iQsignSigns(userid,name);
CREATE INDEX SignsNamekey ON iQsignSigns(namekey);


CREATE TABLE iQsignImages (
   id $iddeftype NOT NULL PRIMARY KEY,
   userid $idtype,
   name text NOT NULL,
   url text,
   file text,
   UNIQUE (userid,name),
   FOREIGN KEY (userid) REFERENCES iqSignUsers(id)
$ENDTABLE;
CREATE INDEX ImageUser on iqSignImages(userid);
CREATE INDEX ImageUserName on iqSignImages(userid,name);


CREATE TABLE iQsignDefines (
   id $iddeftype NOT NULL PRIMARY KEY,
   userid $idtype,
   name text NOT NULL,
   contents text NOT NULL,
   lastupdate $datetime DEFAULT CURRENT_TIMESTAMP,
   UNIQUE (userid,name),
   FOREIGN KEY (userid) REFERENCES iqSignUsers(id)
$ENDTABLE;
CREATE INDEX DefinesUser on iqSignDefines(userid);
CREATE INDEX DefineName on iqSignDefines(name);


CREATE TABLE iQsignParameters (
   defineid $idtype,
   name text NOT NULL,
   description text,
   value text,
   index int NOT_NULL,
   PRIMARY KEY (defineid,name),
   FOREIGN KEY (defineid) REFERENCES iQsignDefines(id)
$ENDTABLE;
CREATE INDEX ParameterDef on iQsignParameters(defineid);


CREATE TABLE OauthTokens (
    access_token text NOT NULL PRIMARY KEY,
    access_expires_on timestamp without time zone NOT NULL,
    refresh_token text,
    refresh_expires_on timestamp without time zone,
    scope text,
    client_id text NOT NULL,
    userid $idtype NOT NULL,
    FOREIGN KEY (userid) REFERENCES iQsignUsers(userid)
$ENDTABLE;
CREATE INDEX TokRefresh on OauthTokens(refresh_token);


CREATE TABLE OauthCodes (
    auth_code text NOT NULL PRIMARY KEY,
    expires_at timestamp without time zone NOT NULL,
    redirect_uri text,
    scope text,
    client text,
    userid $idtype NOT NULL,
    FOREIGN KEY (userid) REFERENCES iQsignUsers(userid)
$ENDTABLE;


EOF

