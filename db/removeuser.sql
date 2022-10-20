#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd



DELETE FROM OauthTokens WHERE userid = $1;
DELETE FROM OauthCodes WHERE userid = $1;
DELETE FROM iQsignRestful WHERE userid = $1;
DELETE FROM iQsignUseCounts WHERE userid = $1;
DELETE FROM iQsignSignCodes WHERE userid = $1;
DELETE FROM iQsignDefines WHERE userid = $1;
DELETE FROM iQsignImages WHERE userid = $1;
DELETE FROM iQsignSigns WHERE userid = $1;
DELETE FROM iQsignValidator WHERE userid = $1;
DELETE FROM iQsignUsers WHERE id = $1;
		






EOF

















