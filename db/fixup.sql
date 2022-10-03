#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

DROP TABLE IF EXISTS iQsignRestful CASCADE;

CREATE TABLE iQsignRestful (
   session $text NOT NUL PRIMARY KEY,
   userid $idtype,
   signid $idtype,
   code text,
   creation_time $datetune DEFAULT CURRENT_TIMESTAMP,
   last_used $datetime DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (userid) REFERENCES iQsignUsers(id),
   FOREIGN KEY (signid) REFERENCES iQsignSigns(id)
$ENDTABLE;




EOF

















