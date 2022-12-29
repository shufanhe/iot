/****************************************************************************************/
/*											*/
/*		cedes.js								*/
/*											*/
/*	Internal JavaScript for iQsign							*/
/*											*/
/****************************************************************************************/



/****************************************************************************************/
/*											*/
/*	Login methods									*/
/*											*/
/****************************************************************************************/

function handleLoginEnter(evt) {
   if (evt.keyCode == 13 || evt.keyCode == 9) {
      evt.preventDefault();
      $('#login-submit').click();
   }
}

function handleLoginSubmit(evt) {
   evt.preventDefault();
   hideLoginError();
   let x = $('#login-username').val();
   if (x == null || x == "") return setLoginError('Must specify user name');
   x = x.toLowerCase();
   let p = $('#login-password').val();
   if (p == null || p == '') return setLoginError('Must specify password');
   let s = $('#login-padding').val();
   if (s == null || s == '') return setLoginError('Page not loaded correctly');

   let p1 = hasher(p);
   let p2 = hasher(p1 + x);
   let p3 = hasher(p2 + s);

   let data = { username: x, password: p3, padding: s };
   fetch('/login', {
	 method: 'POST',
	 headers: {
	    'Content-Type': 'application/json'
	 },
	 body: JSON.stringify(data)
      })
      .then((resp) => { let v = resp.json(); return v; })
      .then(handleLoginStatus)
      .catch((e) => { setLoginError(e) });
}


function handleLoginStatus(sts) {
   let redir = $("#login-redirect").val();
   console.log("LOGIN STATUS",sts,redir)
   if (sts.status == 'OK') {
      if (redir == null || redir == '') redir = '/index';
      window.location.href = redir;
   }
   else if (sts.status == 'ERROR') {
      setLoginError(sts.message);
   }
   else setLoginError("Problem logging in");
}



function setLoginError(msg) {
   setErrorField(msg, 'login-error');
}


function hideLoginError(msg) {
   hideErrorField('login-error');
}




/****************************************************************************************/
/*											*/
/*	login/register utility methods							*/
/*											*/
/****************************************************************************************/

function setErrorField(msg, fld) {
   console.log("SET ERROR", msg, fld);

   let fldid = "#" + fld;
   $(fldid).text(msg);
   $(fldid).show();
}



function hideErrorField(fld) {
   console.log("HIDE ERROR", fld);

   let fldid = "#" + fld;
   $(fldid).hide();
}



function hasher(msg) {
   let bits = sjcl.hash.sha512.hash(msg);
   let str = sjcl.codec.base64.fromBits(bits);
   return str;
}



function validatePassword(pwd) {
   if (pwd == null || pwd == '') return false;

   // check length, contents

   return true;
}


/* end of iqsign.js */
