#! /bin/csh -fx

source setup.sql


$run $host $db <<EOF

$runcmd

ALTER TABLE iQsignSigns
   DROP CONSTRAINT IF EXISTS iqsignsigns_userid_fkey;
ALTER TABLE iQsignSigns
   ADD CONSTRAINT iqsignsigns_userid_fkey FOREIGN KEY (userid) REFERENCES iQsignUsers(id) ON DELETE CASCADE;


ALTER TABLE iQsignImages
   DROP CONSTRAINT IF EXISTS iqsignimages_userid_fkey;
ALTER TABLE iQsignImages
   ADD CONSTRAINT iqsignimages_userid_fkey FOREIGN KEY (userid) REFERENCES iqSignUsers(id) ON DELETE CASCADE;


ALTER TABLE iQsignLoginCodes
   DROP CONSTRAINT IF EXISTS iqsignlogincodes_userid_fkey;
ALTER TABLE iQsignLoginCodes
   ADD CONSTRAINT iqsignlogincodes_userid_fkey FOREIGN KEY (userid) REFERENCES iqSignUsers(id) ON DELETE CASCADE;

ALTER TABLE iQsignLoginCodes
   DROP CONSTRAINT IF EXISTS iqsignlogincodes_signid_fkey;
ALTER TABLE iQsignLoginCodes
   ADD CONSTRAINT iqsignlogincodes_signid_fkey FOREIGN KEY (signid) REFERENCES iqSignSigns(id) ON DELETE CASCADE;


ALTER TABLE iQsignSignCodes
   DROP CONSTRAINT IF EXISTS iqsignsigncodes_userid_fkey;
ALTER TABLE iQsignSignCodes
   ADD CONSTRAINT iqsignsigncodes_userid_fkey FOREIGN KEY (userid) REFERENCES iqSignUsers(id) ON DELETE CASCADE;

ALTER TABLE iQsignSignCodes
   DROP CONSTRAINT IF EXISTS iqsignsigncodes_signid_fkey;
ALTER TABLE iQsignSignCodes
   ADD CONSTRAINT iqsignsigncodes_signid_fkey FOREIGN KEY (signid) REFERENCES iqSignSigns(id) ON DELETE CASCADE;


ALTER TABLE iQsignUseCounts
   DROP CONSTRAINT IF EXISTS iqsignusecounts_userid_fkey;
ALTER TABLE iQsignUseCounts
   ADD CONSTRAINT iqsignusecounts_userid_fkey FOREIGN KEY (userid) REFERENCES iqSignUsers(id) ON DELETE CASCADE;

ALTER TABLE iQsignUseCounts
   DROP CONSTRAINT IF EXISTS iqsignusecounts_defineid_fkey;
ALTER TABLE iQsignUseCounts
   ADD CONSTRAINT iqsignusecounts_defineid_fkey FOREIGN KEY (defineid) REFERENCES iqSignDefines(id) ON DELETE CASCADE;


ALTER TABLE iQsignRestful
   DROP CONSTRAINT IF EXISTS iqsignrestful_userid_fkey;
ALTER TABLE iQsignRestful
   ADD CONSTRAINT iqsignrestful_userid_fkey FOREIGN KEY (userid) REFERENCES iQsignUsers(id) ON DELETE CASCADE;


ALTER TABLE OauthTokens
   DROP CONSTRAINT IF EXISTS oauthtokens_userid_fkey;
ALTER TABLE OauthTokens
   ADD CONSTRAINT oauthtokens_userid_fkey FOREIGN KEY (userid) REFERENCES iqSignUsers(id) ON DELETE CASCADE;

ALTER TABLE OauthTokens
   DROP CONSTRAINT IF EXISTS oauthtokens_signid_fkey;
ALTER TABLE OauthTokens
   ADD CONSTRAINT oauthtokens_signid_fkey FOREIGN KEY (signid) REFERENCES iqSignSigns(id) ON DELETE CASCADE;


ALTER TABLE OauthCodes
   DROP CONSTRAINT IF EXISTS oauthcodes_userid_fkey;
ALTER TABLE OauthCodes
   ADD CONSTRAINT oauthcodes_userid_fkey FOREIGN KEY (userid) REFERENCES iqSignUsers(id) ON DELETE CASCADE;

ALTER TABLE OauthCodes
   DROP CONSTRAINT IF EXISTS oauthcodes_signid_fkey;
ALTER TABLE OauthCodes
   ADD CONSTRAINT oauthcodes_signid_fkey FOREIGN KEY (signid) REFERENCES iqSignSigns(id) ON DELETE CASCADE;




EOF
