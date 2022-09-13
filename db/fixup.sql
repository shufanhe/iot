#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

ALTER TABLE OauthTokens
ADD signid $idtype;





EOF

















