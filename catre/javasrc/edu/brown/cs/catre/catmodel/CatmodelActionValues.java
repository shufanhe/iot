/********************************************************************************/
/*                                                                              */
/*              CatmodelActionValues.java                                       */
/*                                                                              */
/*      Set of values to be passed to a transition                              */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2022 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2022, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.catmodel;

import java.util.HashMap;

import edu.brown.cs.catre.catre.CatreActionValues;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;

class CatmodelActionValues extends HashMap<String, Object> implements CatreActionValues
{


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelActionValues(CatreParameterSet params)
{
   if (params != null) {
      for (CatreParameter cp : params.getValidParameters()) {
         put(cp.getName(),params.getValue(cp));
       }
    }
}


}       // end of class CatmodelActionValues




/* end of CatmodelActionValues.java */

