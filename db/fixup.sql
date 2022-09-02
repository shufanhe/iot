#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

-- ALTER TABLE iQsignParameters DROP COLUMN index;

ALTER TABLE iQsignParameters
ADD index int DEFAULT 0;

-- UPDATE iQsignParameters SET index = 0;

ALTER TABLE iQsignParameters
ALTER COLUMN index SET NOT NULL;

EOF

















