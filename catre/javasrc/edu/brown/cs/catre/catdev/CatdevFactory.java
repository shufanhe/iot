/********************************************************************************/
/*                                                                              */
/*              CatdevFactory.java                                              */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.catre.catdev;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;

public class CatdevFactory implements CatdevConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreUniverse for_universe;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatdevFactory(CatreUniverse cu)
{
   for_universe = cu;
}


/********************************************************************************/
/*                                                                              */
/*      Methods to create virtual devices                                       */
/*                                                                              */
/********************************************************************************/

public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   CatreDevice device = null;
   
   String typ = map.get("VTYPE").toString();
   if (typ != null) {
      switch (typ) {
         case "Duration" :
            return new CatdevSensorDuration(for_universe,cs,map);
         case "Debouncer" :
            break;
         case "Latch" :
            return new CatdevSensorLatch(for_universe,cs,map);
         case "Or" :
            return new CatdevSensorOr(for_universe,cs,map);
         case "Weather" :
            return new CatdevWeatherSensor(for_universe,cs,map);
       }
    }
   
   return device;
}


}       // end of class CatdevFactory




/* end of CatdevFactory.java */

