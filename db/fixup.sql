#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

ALTER TABLE OauthCodes
ADD signid $idtype;





EOF

















