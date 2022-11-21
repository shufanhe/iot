/********************************************************************************/
/*                                                                              */
/*              CatmodelParameterSet.java                                       */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.catmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;

class CatmodelParameterSet extends HashMap<CatreParameter, Object> implements CatreParameterSet, CatmodelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<CatreParameter>     valid_parameters;
private CatmodelUniverse        for_universe;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelParameterSet(CatreUniverse cu)
{
   valid_parameters = new HashSet<CatreParameter>();
   for_universe = (CatmodelUniverse) cu;
}

CatmodelParameterSet(CatmodelUniverse cu,Collection<CatreParameter> valids)
{
   this(cu);
   
   if (valids != null) valid_parameters.addAll(valids);
}

CatmodelParameterSet(CatmodelUniverse cu,CatreParameterSet ps)
{
   this(cu);
   
   if (ps != null) {
      putAll(ps);
      valid_parameters = new HashSet<CatreParameter>(ps.getValidParameters());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Collection<CatreParameter> getValidParameters()
{
   return valid_parameters;
}


@Override public void addParameter(CatreParameter up)
{
   valid_parameters.add(up);
}


public void addParameters(Collection<CatreParameter> ups) 
{
   valid_parameters.addAll(ups);
}


@Override public Object put(CatreParameter up,Object o)
{
   addParameter(up);
   o = up.normalize(o);
   return super.put(up,o);
}


@Override public void putAll(Map<? extends CatreParameter,? extends Object> vals)
{
   super.putAll(vals);
   for (CatreParameter up : vals.keySet()) addParameter(up);
}


@Override public void setParameter(String nm,Object val)
{
   CatreParameter parm = null;
   for (CatreParameter up : getValidParameters()) {
      if (up.getName().equals(nm)) {
         parm = up;
         break;
       }
    }
   if (parm == null) return;
   
   if (val == null) {
      remove(parm);
      return;
    }
   
   Object rvl = parm.normalize(val);
   put(parm,rvl);
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = new HashMap<>();
   List<Object> plst = new ArrayList<>();
   for (CatreParameter up : valid_parameters) {
      Object val = get(up);
      Map<String,Object> pval = up.toJson();
      String sval = up.unnormalize(val);
      pval.put("VALUE",sval);
      plst.add(pval);
    }
   
   rslt.put("PARAMETERS",plst);

   return rslt;
}




@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   valid_parameters = getSavedSubobjectSet(cs,map,"PARAMETERS",
         for_universe::createParameter,valid_parameters);
}


}       // end of class CatmodelParameterSet




/* end of CatmodelParameterSet.java */

