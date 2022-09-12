#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

DROP TABLE IF EXISTS iQsignLoginCodes CASCADE;
DROP TABLE IF EXISTS iQsignSignCodes CASCADE;

CREATE TABLE iQsignLoginCodes (
   code text NOT NULL PRIMARY KEY,
   userid $idtype NOT NULL,
   signid $idtype NOT NULL,
   lastused $datetime,
   creation_time  $datetime DEFAULT CURRENT_TIMESTAMP,
   outsideid text,
   FOREIGN KEY (userid) REFERENCES iQsignUsers(id),
   FOREIGN KEY (signid) REFERENCES iQsignSigns(id)
$ENDTABLE;


CREATE TABLE iQsignSignCodes (
   code text NOT NULL PRIMARY KEY,
   userid $idtype NOT NULL,
   signid $idtype NOT NULL,
   callback_url text,
   creation_time $datetime DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (userid) REFERENCES iQsignUsers(id),
   FOREIGN KEY (signid) REFERENCES iQsignSigns(id)
$ENDTABLE;
CREATE INDEX SignCodes on iQsignSignCodes(signid);







EOF

















