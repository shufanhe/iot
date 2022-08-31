#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

ALTER TABLE iQsignSigns
ADD displayname text;

EOF


