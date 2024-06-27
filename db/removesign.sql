#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd


DELETE FROM iQsignSignCodes WHERE signid = $1;
DELETE FROM iQsignLoginCodes WHERE signid = $1;
DELETE FROM OauthTokens WHERE signid = $1;
DELETE FROM OauthCodes WHERE signid = $1;
DELETE FROM iQsignSigns WHERE id = $1;

EOF
