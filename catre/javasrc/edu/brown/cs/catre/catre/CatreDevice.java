/********************************************************************************/
/*                                                                              */
/*              CatreDevice.java                                                */
/*                                                                              */
/*      Representation of a device (sensor, entity or both)                     */
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

public interface CatreDevice extends CatreDescribable, CatreIdentifiable, CatreSubSavable 
{



/**
 *	Add a trigger that is called when device changes state.
 **/

void addDeviceListener(CatreDeviceListener hdlr);


/**
 *	Remove a trigger.
 **/

void removeDeviceListener(CatreDeviceListener hdlr);



/**
 *	Return the set of parameters that can be displayed to show the
 *	state of this entity.  Parameters are used here because they are
 *	typed.	The actual valu8es are in the property set of the world.
 **/

Collection<CatreParameter> getParameters();


/**
 *	Find a parameter by name
 **/

CatreParameter findParameter(String id);

/**
 *	Get the value of a parameter in the given world.  If the world is curernt
 *	this needs to get the current state of the parameter.
 **/

Object getValueInWorld(CatreParameter p,CatreWorld w);


/**
 *	Set the value of a parameter in the given world.  If the world is current,
 *	this will actually affect the device.
 **/

void setValueInWorld(CatreParameter p,Object val,CatreWorld w) throws CatreActionException;






/**
 *	Return the set of all transitions for this device
 **/

Collection<CatreTransition> getTransitions();


/**
 *      Find a transition by name
 **/
CatreTransition findTransition(String name);

/**
 *	Indicates if there are any transitions for the device
 **/

boolean hasTransitions();


/**
 *	Find a transition by name
 **/

// CatreTransition findTransition(String name);




/**
 *	Actually apply a transition to the entity in the given world
 **/

void apply(CatreTransition t,Map<String,Object> props,CatreWorld w) throws CatreActionException;








/**
 *	Check if the device is enabled
 **/

boolean isEnabled();



/**
 *      Return the universe associated with the device
 **/ 

CatreUniverse getUniverse();



/**
 *      Return the contoller
 **/

public default CatreController getCatre()       { return getUniverse().getCatre(); }



/**
 *      Check if this device is dependent on another
 **/

public boolean isDependentOn(CatreDevice device);



/**
 *      Return bridge if this is a basic device.  Otherwise return null.
 **/

public CatreBridge getBridge();


public String getDeviceId();

CatreTransition createTransition(CatreStore cs,Map<String,Object> map);

void setEnabled(boolean fg);

/**
 *	Start running the device (after it has been added to universe)
 **/

void startDevice();

}       // end of interface CatreDevice




/* end of CatreDevice.java */

