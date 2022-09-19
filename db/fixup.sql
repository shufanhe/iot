#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

DROP TABLE IF EXISTS iQsignUseCounts CASCADE;
DROP TABLE IF EXISTS iQsignParameters CASCADE;


CREATE TABLE iQsignUseCounts (
   defineid $idtype NOT NULL,
   userid $idtype NOT NULL,
   count int DEFAULT 1,
   last_used $datetime DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(defineid,userid),
   FOREIGN KEY (userid) REFERENCES iQsignUsers(id),
   FOREIGN KEY (defineid) REFERENCES iQsignDefines(id)
$ENDTABLE;
CREATE INDEX UseUsers on iQsignUseCounts(userid);



EOF

















