/********************************************************************************/
/*                                                                              */
/*              CatreHome.java                                                  */
/*                                                                              */
/*      Set of devices/sensors for a single user/home                           */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
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



package edu.brown.cs.catre.catre;

import java.util.Collection;
import java.util.Map;

/**
 *      The home is a set of devices and sensors that are available for a single 
 *      instance (or house).  This defines the program that should be run to 
 *      control these devices.  
 **/

public interface CatreUniverse extends CatreSavable, CatreDescribable
{







/**
 *      Return current world associated with this universe
 **/

CatreWorld getCurrentWorld();

CatreWorld findWorld(String id);

CatreWorld createWorld(CatreWorld base);

CatreWorld removeWorld(CatreWorld world);


/**
 *	Return the set of available devices that can be acted upon.
 **/

Collection<CatreDevice> getDevices();


CatreDevice findDevice(String id);

CatreDevice createVirtualDevice(CatreStore cs,Map<String,Object> map);




/**
 *	Add an event listener for the universe
 **/

void addUniverseListener(CatreUniverseListener l);


/**
 *	Remove an event listener
 **/

void removeUniverseListener(CatreUniverseListener l);



/**
 *      Start the universe running
 **/

void start();


/**
 *      Return global controller
 **/

CatreController getCatre();

CatreUser getUser();

CatreProgram getProgram();


CatreParameterSet createParameterSet();
CatreParameterSet createParameterSet(CatreStore cs,Map<String,Object> map);
CatreParameterSet createSavedParameterSet(CatreStore cs,Map<String,Object> map);
CatrePropertySet createPropertySet();
CatreActionValues createActionValues(CatreParameterSet ps);




CatreParameter createParameter(CatreStore cs,Map<String,Object> map);



CatreBridge findBridge(String name);
void addBridge(String name);

void updateDevices(CatreBridge bridge);
void addDevice(CatreDevice device);
void removeDevice(CatreDevice device);

CatreParameter createDateTimeParameter(String nm);
CatreParameter createBooleanParameter(String name,boolean issensor,String label);
CatreParameter createEnumParameter(String name,Enum<?> e);
CatreParameter createEnumParameter(String name,Iterable<String> vals);
CatreParameter createSetParameter(String name,Iterable<String> vals);
CatreParameter createEnumParameter(String name,String [] v);
CatreParameter createIntParameter(String name,int min,int max);
CatreParameter createRealParameter(String name,double min,double max);
CatreParameter createRealParameter(String name);
CatreParameter createColorParameter(String name);
CatreParameter createStringParameter(String name);
CatreParameter createEventsParameter(String name);

CatreTriggerContext createTriggerContext();
CatreTimeSlotEvent createCalendarEvent(CatreStore cs,Map<String,Object> map);

CatreParameterRef createParameterRef(CatreReferenceListener ref,String device,String parameter);
CatreParameterRef createParameterRef(CatreReferenceListener ref,CatreStore cs,Map<String,Object> map);





}       // end of interface CatreHome




/* end of CatreHome.java */

