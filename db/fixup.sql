#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

DROP TABLE IF EXISTS OauthClients CASCADE;
DROP TABLE IF EXISTS OauthTokens CASCADE;
DROP TABLE IF EXISTS OauthCodes CASCADE;


CREATE TABLE OauthTokens (
    access_token text NOT NULL PRIMARY KEY,
    access_expires_on timestamp without time zone NOT NULL,
    refresh_token text,
    refresh_expires_on timestamp without time zone,
    scope text,
    client_id text NOT NULL,
    userid $idtype NOT NULL,
    FOREIGN KEY (userid) REFERENCES iQsignUsers(id)
$ENDTABLE;
CREATE INDEX TokRefresh on OauthTokens(refresh_token);


CREATE TABLE OauthCodes (
    auth_code text NOT NULL PRIMARY KEY,
    expires_at timestamp without time zone NOT NULL,
    redirect_uri text,
    scope text,
    client text,
    userid $idtype NOT NULL,
    FOREIGN KEY (userid) REFERENCES iQsignUsers(id)
$ENDTABLE;


EOF


