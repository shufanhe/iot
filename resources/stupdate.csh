#! /bin/csh -f

set DEV = 9896b537-3f26-4d0c-8094-02566a08e5f6
set CAP = valleyafter35319.iqsignIntelligentSign
set VID = 475d2b23-a680-3d15-99bd-c8cf88a1f1e3

setenv SMARTTHINGS_DEBUG true

smartthings deviceprofiles:update $DEV -i iqsigndev.json -o iqsigndev.new.json
if ($status > 0) exit;
echo DONE DEVICE PROFILE

smartthings capabilities:update $CAP -i iqsigncap.json -o iqsigncap.new.json
if ($status > 0) exit;
echo DONE CAPABILITY

smartthings capabilities:presentation:update $CAP -i iqsigndisp.json -o iqsigndisp.new.json
if ($status > 0) exit;
echo DONE PRESENTATION


foreach i (dev cap disp)
  mv iqsign${i}.json iqsign${i}.save
  mv iqsign${i}.new.json iqsign${i}.json
end



echo DEVICE PROFILE:
smartthings deviceprofiles $DEV
smartthings deviceprofiles -y $DEV

echo DEVICE CAPABILITIES:
smartthings capabilities $CAP

echo DEVICE CAPABILITY PRESENTATION:
smartthings capabilities:presentation $CAP
smartthings capabilities:presentation -y $CAP

echo DEVICE PRESENTATION:
smartthings presentation $VID

echo DEVICE CONFIG PRESENTATION:
smartthings presentation:device-config $VID
smartthings presentation:device-config -y $VID

echo DEVICE PROFILE VIEW:
smartthings deviceprofiles:view $DEV
