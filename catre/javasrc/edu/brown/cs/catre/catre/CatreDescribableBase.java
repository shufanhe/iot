/********************************************************************************/
/*                                                                              */
/*              CatreDescribableBase.java                                       */
/*                                                                              */
/*      Base class for a describable, sub-savable object                        */
/*                                                                              */
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




package edu.brown.cs.catre.catre;

import java.util.Map;

public class CatreDescribableBase extends CatreSubSavableBase 
        implements CatreDescribable, CatreSubSavable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  base_name;
private String  base_label;
private String  base_description;
private boolean user_description;
 

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatreDescribableBase(String pfx)
{
   super(pfx);
   base_name = null;
   base_label = null;
   base_description = null;
   user_description = false;
}


protected CatreDescribableBase(String pfx,CatreDescribableBase cdb)
{
   super(pfx);
   base_name = cdb.base_name;
   base_label = cdb.base_label;
   base_description = cdb.base_description;
   user_description = cdb.user_description;
}


protected CatreDescribableBase(CatreStore cs,Map<String,Object> map)
{
   super(cs,map);
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public String getName()
{
   return base_name;
}


protected void setName(String nm)
{
   base_name = nm;
}




@Override public String getLabel()
{
   if (base_label == null) {
      String lbl = getName();
      if (lbl == null) return null;
      return lbl.replace("_"," ");
    }
   
   return base_label;
}


public void setLabel(String lbl)
{
   base_label = lbl;
}



@Override public String getDescription()
{
   if (base_description == null) {
      return getLabel();
    }

   return base_description;
}


public void setDescription(String d)
{
   base_description = d;
   user_description = true;
}


public void setGeneratedDescription(String d)
{
   if (user_description && base_description != null) return;
   
   base_description = d;
   user_description = false;
}




/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("NAME",getName());
   rslt.put("LABEL",getLabel());
   rslt.put("DESCRIPTION",getDescription());
   rslt.put("USERDESC",user_description);
     
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   base_name = getSavedString(map,"NAME",base_name);
   base_label = getSavedString(map,"LABEL",base_label);
   base_description = getSavedString(map,"DESCRIPTION",base_description);
   user_description = getSavedBool(map,"USERDESC",false);
}



protected boolean update(CatreDescribableBase cb)
{
   // NAME must be the same
   boolean chng = false;
   if (base_label == null) {
      if (cb.base_label != null) {
         chng = true;
         base_label = cb.base_label;
       }
    } 
   else if (!base_label.equals(cb.base_label)) {
      chng = true;
      base_label = cb.base_label;
    }
   if (base_description == null) {
      if (cb.base_description != null) {
         chng = true;
         base_description = cb.base_description;
       }
    } 
   else if (!base_description.equals(cb.base_description)) {
      chng = true;
      base_description = cb.base_description;
    }
   user_description = cb.user_description;
   
   return chng;
}



}       // end of class CatreDescribableBase




/* end of CatreDescribableBase.java */

