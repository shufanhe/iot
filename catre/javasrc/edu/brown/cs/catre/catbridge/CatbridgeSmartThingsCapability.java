/********************************************************************************/
/*                                                                              */
/*              CatbridgeSmartThingsCapability.java                             */
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



package edu.brown.cs.catre.catbridge;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catdev.CatdevTransition;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter; 
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreTransitionType;
import edu.brown.cs.catre.catre.CatreWorld;

abstract class CatbridgeSmartThingsCapability implements CatbridgeConstants
{



/********************************************************************************/
/*										*/
/*	Static methods								*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	access_name;
private String  capability_name;

private static Object command_lock = new Object();

private static Map<String,CatbridgeSmartThingsCapability> all_caps;

static {
   all_caps = new HashMap<>();
   addCapability(new AccelerationSensor());
   addCapability(new Actuator());
   addCapability(new Alarm());
   addCapability(new Battery());
   addCapability(new Beacon());
   addCapability(new Button());
   addCapability(new CO2Detector());
   addCapability(new ColorControl());
   addCapability(new Configuration());
   addCapability(new ContactSensor());
   addCapability(new DoorControl());
   addCapability(new EnergyMeter());
   addCapability(new IlluminanceMeasurement());
   addCapability(new ImageCapture());
   addCapability(new Lock());
   addCapability(new Momentary());
   addCapability(new MotionSensor());
   addCapability(new MusicPlayer());
   addCapability(new Notification());
   addCapability(new Polling());
   addCapability(new PowerMeter());
   addCapability(new PresenceSensor());
   addCapability(new Refresh());
   addCapability(new RelativeHumidity());
   addCapability(new RelaySwitch());
   addCapability(new Sensor());
   addCapability(new SignalStrength());
   addCapability(new SleepSensor());
   addCapability(new SmokeDetector());
   addCapability(new SpeechSynthesis());
   addCapability(new StepSensor());
   addCapability(new Switch());
   addCapability(new SwitchLevel());
   addCapability(new Temperature());
   addCapability(new Thermostat());
   addCapability(new ThermostatCoolingSetpoint());
   addCapability(new ThermostatFanMode());
   addCapability(new ThermostatHeatingSetpoint());
   addCapability(new ThermostatMode());
   addCapability(new ThermostatOperatingState());
   addCapability(new ThermostatSetpoint());
   addCapability(new ThreeAxis());
   addCapability(new Tone());
   addCapability(new TouchSensor());
   addCapability(new Valve());
   addCapability(new WaterSensor());
}


static CatbridgeSmartThingsCapability findCapability(String name)
{
   return all_caps.get(name);
}



private static void addCapability(CatbridgeSmartThingsCapability c)
{
   all_caps.put(c.getAccessName(),c);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatbridgeSmartThingsCapability(String name,String access)
{
   capability_name = name;
   
   access_name = access;
}




/********************************************************************************/
/*										*/
/*	Local access methods							*/
/*										*/
/********************************************************************************/

String getAccessName()			{ return access_name; }



/********************************************************************************/
/*										*/
/*	Default setting methods 						*/
/*										*/
/********************************************************************************/

void handleSmartThingsValue(CatbridgeSmartThingsDevice std,Object v)
{
   CatreParameter up = std.findParameter(getParameterName());
   if (up != null) {
      std.setValueInWorld(up,v,null);
    }
   else {
      CatreLog.logD("CATBRIDGE","SET VALUE FROM SMARTTHINGS: " + getAccessName() + " " +
	    v.getClass().getName() + " " + std.getName() + " " + v);
    }
}



String getParameterName()		{ return getAccessName(); }

void addToDevice(CatreDevice d)         { }

void addSensor(CatreDevice d,CatreParameter p)
{
   p.setIsSensor(true);
   addParameter(d,p);
}


void addTarget(CatreDevice d,CatreParameter p)
{
   p.setIsTarget(true);
   addParameter(d,p);
}


void addParameter(CatreDevice d,CatreParameter p)
{
   d.addParameter(p);
}



/********************************************************************************/
/*										*/
/*	Acceleration Sensor Capability						*/
/*										*/
/********************************************************************************/

private static class AccelerationSensor extends CatbridgeSmartThingsCapability {

private static enum AccelerationState { ACTIVE, INACTIVE };

AccelerationSensor() {
   super("Acceleration Sensor","acceleration");
}

@Override public void addToDevice(CatreDevice d) {
   CatreParameter bp = d.getUniverse().createEnumParameter("acceleration",AccelerationState.INACTIVE);
   bp.setLabel(d.getLabel() + " Acceleration");
   addSensor(d,bp);
}

}	// end of inner class AccelerationSensor



/********************************************************************************/
/*										*/
/*	Actuator capability							*/
/*										*/
/********************************************************************************/

private static class Actuator extends CatbridgeSmartThingsCapability {
   
   Actuator() {
      super("Actuator","actuator");
    }
   
   @Override public void addToDevice(CatreDevice e) { }
   
}	// end of inner class Actuator






/********************************************************************************/
/*										*/
/*	Alarm Capability							*/
/*										*/
/********************************************************************************/

private static class Alarm extends CatbridgeSmartThingsCapability {
   
   private static enum AlarmState { OFF, SIREN, STROBE, BOTH };
   
   Alarm() {
      super("Alarm","alarm");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("alarm",AlarmState.OFF);
      bp.setDescription(d.getLabel() + " State");
      bp.setLabel(d.getLabel());
      addSensor(d,bp);
      addSmartThingsTransition(d,bp,AlarmState.STROBE,"Set Strobe","strobe");
      addSmartThingsTransition(d,bp,AlarmState.SIREN,"Set Siren","siren");
      addSmartThingsTransition(d,bp,AlarmState.OFF,"Set Off","off");
      addSmartThingsTransition(d,bp,AlarmState.BOTH,"Set Strobe and Siren","both");
    }
   
}	// end of inner class Alarm



/********************************************************************************/
/*										*/
/*	Battery Capability							*/
/*										*/
/********************************************************************************/

private static class Battery extends CatbridgeSmartThingsCapability {
   
   Battery() {
      super("Battery","battery");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("battery",0,100);
      bp.setLabel(d.getLabel() + " Battery");
      addSensor(d,bp);
    }

}	// end of inner class Battery




/********************************************************************************/
/*										*/
/*	Beacon capability							*/
/*										*/
/********************************************************************************/

private static class Beacon extends CatbridgeSmartThingsCapability {
   
   enum BEACON_STATE { PRESENT, NOT_PRESENT };
   
   Beacon() {
      super("Beacon","beacon");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("presence",BEACON_STATE.PRESENT);
      bp.setLabel(d.getLabel() + " Presence");
      addSensor(d,bp);
    }
   
   @Override String getParameterName()		{ return "presence"; }
   
}	// end of inner class Beacon




/********************************************************************************/
/*										*/
/*	Button Capability							*/
/*										*/
/********************************************************************************/

private static class Button extends CatbridgeSmartThingsCapability {
   
   private static enum ButtonState { NONE, HELD, PUSHED };
   
   Button() {
      super("Button","button");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("button",ButtonState.NONE);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }

}	// end of inner class Button



/********************************************************************************/
/*										*/
/*	CO2 Detector Capability 						*/
/*										*/
/********************************************************************************/

private static class CO2Detector extends CatbridgeSmartThingsCapability {
   
   private static enum C02State { TESTED, CLEAR, DETECTED };
   
   CO2Detector() {
      super("Carbon Monoxide Detector","carbonMonoxideDetector");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("carbonMonoxide",
            C02State.CLEAR);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }
   
   @Override String getParameterName()		{ return "carbonMonoxide"; }

}	// end of inner class C02Detector



/********************************************************************************/
/*										*/
/*	Color Control capability						*/
/*										*/
/********************************************************************************/

private static class ColorControl extends CatbridgeSmartThingsCapability {
   
   ColorControl() {
      super("Color Control","colorControl");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bphue = d.getUniverse().createRealParameter("hue",0,100);
      bphue.setLabel(d.getLabel() + " Hue");
      CatreParameter bpsat = d.getUniverse().createRealParameter("saturation",0,100);
      bpsat.setLabel(d.getLabel() + " Saturation");
      CatreParameter bpcol = d.getUniverse().createColorParameter("color");
      bpcol.setLabel(d.getLabel() + " Color");
      addTarget(d,bphue);
      addTarget(d,bpsat);
      addTarget(d,bpcol);
      CatreParameter sethue = d.getUniverse().createRealParameter("hue",0,100);
      addSmartThingsTransition(d,bphue,null,"Set Hue","setHue",sethue,0,true);
      CatreParameter setsat = d.getUniverse().createRealParameter("saturation",0,100);
      addSmartThingsTransition(d,bpsat,null,"Set Saturation","setSaturation",setsat,100,true);
      CatreParameter setcol = d.getUniverse().createColorParameter("color");
      addSmartThingsTransition(d,bpcol,null,"Set Color","setColor",setcol,Color.WHITE,true);
    }

}	// end of inner class ColorControl




/********************************************************************************/
/*										*/
/*	Configure Capability							*/
/*										*/
/********************************************************************************/

private static class Configuration extends CatbridgeSmartThingsCapability {
   
   Configuration() {
      super("Configuration","configuration");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      addSmartThingsTransition(d,null,null,"Configure","configure");
    }

}	// end of inner class Configuration



/********************************************************************************/
/*										*/
/*	Contact Sensor Capability						*/
/*										*/
/********************************************************************************/

private static class ContactSensor extends CatbridgeSmartThingsCapability {
   
   private static String [] CONTACT_STATE = { "open", "closed" };
   
   ContactSensor() {
      super("Contact Sensor","contact");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("contact",CONTACT_STATE);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }
   
}       // end of inner class ContactSensor





/********************************************************************************/
/*										*/
/*	Door Control Capability 						*/
/*										*/
/********************************************************************************/

private static class DoorControl extends CatbridgeSmartThingsCapability
{
   
   private enum DoorState { UNKNOWN, CLOSED, OPEN, CLOSING, OPENING };
   
   DoorControl() {
      super("Door Control","doorControl");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("door",DoorState.UNKNOWN);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
      addSmartThingsTransition(d,bp,DoorState.OPEN,"Open","open");
      addSmartThingsTransition(d,bp,DoorState.CLOSED,"Close","close");
    }
   
   String getParameterName()			{ return "door"; }
   
}	// end of inner class DoorControl



/********************************************************************************/
/*										*/
/*	Energy Meter Capability 						*/
/*										*/
/********************************************************************************/

private static class EnergyMeter extends CatbridgeSmartThingsCapability
{
   EnergyMeter() {
      super("Energy Meter","energyMeter");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createRealParameter("energy",0,1000000);
      bp.setLabel(d.getLabel() + " Reading");
      addSensor(d,bp);
    }
   
   String getParameterName()			{ return "energy"; }

}	// end of inner class EnergyMeter




/********************************************************************************/
/*										*/
/*	Illuminance Measurement Capability					*/
/*										*/
/********************************************************************************/

private static class IlluminanceMeasurement extends CatbridgeSmartThingsCapability {
   
   IlluminanceMeasurement() {
      super("Illuminance Measurement","illuminanceMeasurement");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createRealParameter("illuminance",0,100);
      bp.setLabel(d.getLabel() + " Illuminance");
      addSensor(d,bp);
    }
   
   @Override String getParameterName()		{ return "illuminance"; }

}


/********************************************************************************/
/*										*/
/*	Image Capture Capability						*/
/*										*/
/********************************************************************************/

private static class ImageCapture extends CatbridgeSmartThingsCapability {
   
   ImageCapture() {
      super("Image Capture",null);
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createStringParameter("image");
      bp.setLabel(d.getLabel() + " Image");
      addSensor(d,bp);
      addSmartThingsTransition(d,null,null,"Capture Image","take");
    }
   
}	// end of inner class ImageCapture



/********************************************************************************/
/*										*/
/*	Lock Capability 							*/
/*										*/
/********************************************************************************/

private static class Lock extends CatbridgeSmartThingsCapability {
   
   private enum LockState { LOCKED, UNLOCKED };
   
   Lock() {
      super("Lock","lock");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("lock",LockState.LOCKED);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
      addSmartThingsTransition(d,bp,LockState.LOCKED,"Lock","lock");
      addSmartThingsTransition(d,bp,LockState.UNLOCKED,"Unlock","unlock");
    }

}	// end of inner class Lock



/********************************************************************************/
/*										*/
/*	Momentary Capability							*/
/*										*/
/********************************************************************************/

private static class Momentary extends CatbridgeSmartThingsCapability {
   
   Momentary() {
      super("Momentary","momentary");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      addSmartThingsTransition(d,null,null,"Push","push");
    }

}	// end of inner class Momentary




/********************************************************************************/
/*										*/
/*	Motion Sensor Capability						*/
/*										*/
/********************************************************************************/

private static class MotionSensor extends CatbridgeSmartThingsCapability {
   
   private static enum MotionState { ACTIVE, INACTIVE };
   
   MotionSensor() {
      super("Motion Sensor","motion");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("motion",MotionState.INACTIVE);
      bp.setLabel(d.getLabel() + " Motion");
      addSensor(d,bp);
    }

}	// end of inner class MotionSensor



/********************************************************************************/
/*										*/
/*	Music Player Capability 						*/
/*										*/
/********************************************************************************/

private static class MusicPlayer extends CatbridgeSmartThingsCapability {
   
   private static enum MuteState { MUTED, UNMUTED };
   
   MusicPlayer() {
      super("Music Player",null);
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp1 = d.getUniverse().createStringParameter("status");
      bp1.setLabel(d.getLabel() + " Status");
      addSensor(d,bp1);
      CatreParameter bp2 = d.getUniverse().createIntParameter("level",0,100);
      bp2.setLabel(d.getLabel() + " Level");
      addSensor(d,bp2);
      CatreParameter bp3 = d.getUniverse().createStringParameter("trackDescription");
      bp3.setLabel(d.getLabel() + " Track Description");
      addSensor(d,bp3);
      CatreParameter bp4 = d.getUniverse().createJSONParameter("trackData");
      bp4.setLabel(d.getLabel() + " Track Data");
      addSensor(d,bp4);
      CatreParameter bp5 = d.getUniverse().createEnumParameter("mute",MuteState.UNMUTED);
      addSensor(d,bp5);
      addSmartThingsTransition(d,null,null,"Play","play");
      addSmartThingsTransition(d,null,null,"Pause","pause");
      addSmartThingsTransition(d,null,null,"Stop","stop");
      addSmartThingsTransition(d,null,null,"Next Track","nextTrack");
      CatreParameter setlvl = d.getUniverse().createIntParameter("level",0,100);
      addSmartThingsTransition(d,bp2,null,"Set Level","setLevel",setlvl,50);
      CatreParameter settxt = d.getUniverse().createStringParameter("text");
      addSmartThingsTransition(d,null,null,"Speak","playText",settxt,null);
      addSmartThingsTransition(d,bp5,MuteState.MUTED,"Mute","mute");
      addSmartThingsTransition(d,null,null,"Previous Track","previousTrack");
      addSmartThingsTransition(d,bp5,MuteState.UNMUTED,"Unmute","unmute");
      // addCallTransition(d,"playTrack",new Class [] { String.class });
      // addCallTransition(d,"setTrack",new Class [] { String.class });
      // addCallTransition(d,"resumeTrack",new Class [] { Map.class });
      // addCallTransition(d,"restoreTrack",new Class [] { Map.class });
    }
   
}	// end of inner class MusicPlayer





/********************************************************************************/
/*										*/
/*	Notification Capability 						*/
/*										*/
/********************************************************************************/

private static class Notification extends CatbridgeSmartThingsCapability {
   
   Notification() {
      super("Notification","notification");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter nottxt = d.getUniverse().createStringParameter("text");
      nottxt.setLabel(d.getLabel() + " Text");
      addSmartThingsTransition(d,null,null,"Send Notification","deviceNotification",nottxt,null);
    }

}	// end of inner class Notification




/********************************************************************************/
/*										*/
/*	Polling Capbaility							*/
/*										*/
/********************************************************************************/

private static class Polling extends CatbridgeSmartThingsCapability {
   
   Polling() {
      super("Polling","polling");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      addSmartThingsTransition(d,null,null,"Poll","poll");
    }
   
}	// end of inner class Polling



/********************************************************************************/
/*										*/
/*	Power Meter Capability							*/
/*										*/
/********************************************************************************/

private static class PowerMeter extends CatbridgeSmartThingsCapability {
   
   PowerMeter() {
      super("Power Meter","powerMeter");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("power",0,10000000);
      bp.setLabel(d.getLabel() + " Reading");
      addSensor(d,bp);
    }
   
   @Override String getParameterName()		{ return "power"; }
   
}	// end of inner class PowerMeter




/********************************************************************************/
/*										*/
/*	Presense Sensor Capabilty						*/
/*										*/
/********************************************************************************/

private static class PresenceSensor extends CatbridgeSmartThingsCapability {
   
   static private String [] PresenceState = { "present", "not present" };
   
   PresenceSensor() {
      super("Presence Sensor","presence");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("presence",PresenceState);
      bp.setLabel(d.getLabel() + " Presence");
      addSensor(d,bp);
    }
   
}	// end of inner class PresenceSensor




/********************************************************************************/
/*										*/
/*	Refresh Capabilty							*/
/*										*/
/********************************************************************************/

private static class Refresh extends CatbridgeSmartThingsCapability {
   
   Refresh() {
      super("Refresh","refresh");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      addSmartThingsTransition(d,null,null,"Refresh","refresh");
    }

}	// end of inner class Refresh




/********************************************************************************/
/*										*/
/*	Relative Humidity Measurement Capabilty 				*/
/*										*/
/********************************************************************************/

private static class RelativeHumidity extends CatbridgeSmartThingsCapability {
   
   RelativeHumidity() {
      super("Relative Humidity Measurement","relativeHumidityMeasurement");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("humidity",0,100);
      bp.setLabel(d.getLabel() + " Humidity");
      addSensor(d,bp);
    }
   
   @Override String getParameterName()		{ return "humidity"; }

}	// end of inner class RelativeHumidity




/********************************************************************************/
/*										*/
/*	Relay Switch Capabilty							*/
/*										*/
/********************************************************************************/

private static class RelaySwitch extends CatbridgeSmartThingsCapability {
   
   private static enum SWITCH_STATE { OFF, ON };
   
   RelaySwitch() {
      super("Relay Switch","relaySwitch");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("switch",SWITCH_STATE.OFF);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
      addSmartThingsTransition(d,bp,SWITCH_STATE.OFF,"Turn Off","off");
      addSmartThingsTransition(d,bp,SWITCH_STATE.ON,"Turn On","on");
    }

}	// end of inner class RelaySwitch




/********************************************************************************/
/*										*/
/*	Relay Switch Capabilty							*/
/*										*/
/********************************************************************************/

private static class Sensor extends CatbridgeSmartThingsCapability {
   
   Sensor() {
      super("Sensor","sensor");
    }
   
   @Override public void addToDevice(CatreDevice d) { }
   
}	// end of inner class Sensor




/********************************************************************************/
/*										*/
/*	Signal Strength Capabilty						*/
/*										*/
/********************************************************************************/

private static class SignalStrength extends CatbridgeSmartThingsCapability {
   
   SignalStrength() {
      super("Signal Strength","signalStrength");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp1 = d.getUniverse().createRealParameter("lqi",0,1000000);
      bp1.setLabel(d.getLabel() + " Link Quality");
      addSensor(d,bp1);
      CatreParameter bp2 = d.getUniverse().createRealParameter("rssi",0,1000000);
      bp2.setLabel(d.getLabel() + " Signal Strength");
      addSensor(d,bp2);
    }

}	// end of inner class SignalStrength




/********************************************************************************/
/*										*/
/*	Sleep Sensor								*/
/*										*/
/********************************************************************************/


private static class SleepSensor extends CatbridgeSmartThingsCapability {
   
   static private String [] SleepState = { "not sleeping", "sleeping" };
   
   SleepSensor() {
      super("Sleep Sensor","sleepSensor");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("sleepSensor",SleepState);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }

}	// end of inner class SleepSensor




/********************************************************************************/
/*										*/
/*	Smoke Detector Capability						*/
/*										*/
/********************************************************************************/


private static class SmokeDetector extends CatbridgeSmartThingsCapability {
   
   static private String [] SMOKE_STATE = { "detected", "clear", "tested" };
   
   SmokeDetector() {
      super("Smoke Detector","smokeDetector");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("smokeDetector",SMOKE_STATE);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }

}	// end of inner class SmokeDetector




/********************************************************************************/
/*										*/
/*	Speech Synthesis Capability						*/
/*										*/
/********************************************************************************/

private static class SpeechSynthesis extends CatbridgeSmartThingsCapability {
   
   SpeechSynthesis() {
      super("Speech Synthesis",null);
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter spk = d.getUniverse().createStringParameter("text");
      spk.setLabel(d.getLabel() + " Text");
      addSmartThingsTransition(d,null,null,"Speak","speak",spk,null);
    }

}	// end of inner class SpeechSynthesis




/********************************************************************************/
/*										*/
/*	Step Sensor Capabilty							*/
/*										*/
/********************************************************************************/

private static class StepSensor extends CatbridgeSmartThingsCapability {
   
   StepSensor() {
      super("Step Sensor","stepSensor");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp1 = d.getUniverse().createIntParameter("steps",0,1000000);
      bp1.setLabel(d.getLabel() + " Steps");
      addSensor(d,bp1);
      CatreParameter bp2 = d.getUniverse().createRealParameter("goal",0,1000000);
      bp2.setLabel(d.getLabel() + " Goal");
      addSensor(d,bp2);
    }

}	// end of inner class StepSensor




/********************************************************************************/
/*										*/
/*	Switch Capabilty							*/
/*										*/
/********************************************************************************/

private static class Switch extends CatbridgeSmartThingsCapability {
   
   private enum SWITCH_STATE { OFF, ON };
   
   Switch() {
      super("Switch","switch");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("switch",SWITCH_STATE.OFF);
      bp.setDescription(d.getLabel() + " state");
      bp.setLabel(d.getLabel() + " switch");
      bp.setIsTarget(true);
      addSensor(d,bp);
      addSmartThingsTransition(d,bp,SWITCH_STATE.ON,"Turn On","on");
      addSmartThingsTransition(d,bp,SWITCH_STATE.OFF,"Turn Off","off");
    }

}	// end of inner class Switch




/********************************************************************************/
/*										*/
/*	Switch Level Capabilty							*/
/*										*/
/********************************************************************************/

private static class SwitchLevel extends CatbridgeSmartThingsCapability {
   
   SwitchLevel() {
      super("Switch Level","switchLevel");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("switch",0,100);
      bp.setLabel(d.getLabel() + " Level");
      bp.setIsTarget(true);
      addSensor(d,bp);
      CatreParameter lvlset = d.getUniverse().createIntParameter("level",0,100);
      addSmartThingsTransition(d,bp,null,"Set Level","setLevel",lvlset,50);
    }

}	// end of inner class SwitchLevel




/********************************************************************************/
/*										*/
/*	Temperature Measurement Capabilty					*/
/*										*/
/********************************************************************************/

private static class Temperature extends CatbridgeSmartThingsCapability {
   
   Temperature() {
      super("Temperature Measurement","temperature");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("temperature",0,10000);
      bp.setLabel(d.getLabel() + " Temperature");
      addSensor(d,bp);
    }

}	// end of inner class Temperature




/********************************************************************************/
/*										*/
/*	Thermostat Capability							*/
/*										*/
/********************************************************************************/


private static enum THERMOSTAT_MODE { AUTO, EMERGENCY_HEAT, HEAT, OFF, COOL };
private static enum FAN_MODE { AUTO, ON, CIRCULATE };
private static String [] THERMOSTAT_STATE = { "heating", "idle", "pending cool",
   "vent economizer", "cooling", "pending heat", "fan only" };



private static class Thermostat extends CatbridgeSmartThingsCapability {
   
   Thermostat() {
      super("Thermostat","thermostat");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp1 = d.getUniverse().createIntParameter("temperatore",0,100);
      bp1.setLabel(d.getLabel() + " Temperature");
      addSensor(d,bp1);
      CatreParameter bp2 = d.getUniverse().createIntParameter("heatingSetPoint",0,100);
      bp2.setLabel(d.getLabel() + " Heating Set Point");
      addTarget(d,bp2);
      CatreParameter bp3 = d.getUniverse().createIntParameter("coolingSetPoint",0,100);
      bp3.setLabel(d.getLabel() + " Cooling Set Point");
      addTarget(d,bp3);
      CatreParameter bp4 = d.getUniverse().createIntParameter("thermostatSetPoint",0,100);
      bp4.setLabel(d.getLabel() + " Set Point");
      addTarget(d,bp4);
      CatreParameter bp5 = d.getUniverse().createEnumParameter("thermostatMode",THERMOSTAT_MODE.AUTO);
      bp5.setLabel(d.getLabel() + " Mode");
      addSensor(d,bp5);
      CatreParameter bp6 = d.getUniverse().createEnumParameter("thermostatFanMode",FAN_MODE.AUTO);
      bp6.setLabel(d.getLabel() + " Fan Mode");
      addSensor(d,bp6);
      CatreParameter bp7 = d.getUniverse().createEnumParameter("thermostatOperatingState",THERMOSTAT_STATE);
      bp7.setLabel(d.getLabel() + " Operating State");
      addSensor(d,bp7);
      CatreParameter setpt= d.getUniverse().createIntParameter("Set Point",40,100);
      addSmartThingsTransition(d,bp2,null,"Set Heating Set Point","setHeatingSetpoint",setpt,68);
      addSmartThingsTransition(d,bp2,null,"Set Cooling Set Point","setCoolingSetpoint",setpt,68);
      CatreParameter modset = d.getUniverse().createEnumParameter("Mode",THERMOSTAT_MODE.AUTO);
      addSmartThingsTransition(d,bp5,null,"Set Mode","setThermostateMode",modset,THERMOSTAT_MODE.AUTO);
      CatreParameter fanset = d.getUniverse().createEnumParameter("Fan Mode",FAN_MODE.AUTO);
      addSmartThingsTransition(d,bp6,null,"Set Fan Mode","setThermostatFanMode",fanset,FAN_MODE.AUTO);
    }

}	// end of inner class Thermostat




/********************************************************************************/
/*										*/
/*	Thermostat Cooling Setpoint Capabilty					*/
/*										*/
/********************************************************************************/

private static class ThermostatCoolingSetpoint extends CatbridgeSmartThingsCapability {
   
   ThermostatCoolingSetpoint() {
      super("Thermostat Cooling Setpoint","thermostatCoolingSetpoint");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("coolingSetpoint",0,100);
      bp.setLabel(d.getLabel() + " Cooling Set Point");
      addSensor(d,bp);
      CatreParameter setpt = d.getUniverse().createIntParameter("setpt",40,100);
      addSmartThingsTransition(d,bp,null,"Set Cooling Set Point","setCoolingSetpoint",setpt,78);
    }
   
}	// end of inner class ThermostatCoolingSetpoint





/********************************************************************************/
/*										*/
/*	Thermostat Fan Mode Capabilty						*/
/*										*/
/********************************************************************************/

private static class ThermostatFanMode extends CatbridgeSmartThingsCapability {
   
   ThermostatFanMode() {
      super("Thermostat Fan Mode","thermostatFanMode");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("thermostatFanMode",FAN_MODE.ON);
      addSensor(d,bp);
      CatreParameter fanmode = d.getUniverse().createEnumParameter("Fan Mode",FAN_MODE.AUTO);
      addSmartThingsTransition(d,bp,null,"Fan Mode","setThermostatFanMode",fanmode,FAN_MODE.AUTO);
    }
   
}	// end of inner class ThermostatFanMode



/********************************************************************************/
/*										*/
/*	Thermostat Heating Setpoint Capabilty					*/
/*										*/
/********************************************************************************/

private static class ThermostatHeatingSetpoint extends CatbridgeSmartThingsCapability {

ThermostatHeatingSetpoint() {
   super("Thermostat Heating Setpoint","thermostatHeadingSetpoint");
}

@Override public void addToDevice(CatreDevice d) {
   CatreParameter bp = d.getUniverse().createIntParameter("heatingSetpoint",0,100);
   bp.setLabel(d.getLabel() + " Heating Set Point");
   addSensor(d,bp);
   CatreParameter setpt = d.getUniverse().createIntParameter("setpt",40,100);
   addSmartThingsTransition(d,bp,null,"Set Heating Set Point","setHeatingSetpoint",setpt,68);
}

}	// end of inner class ThermostatHeatingSetpoint




/********************************************************************************/
/*										*/
/*	Thermostat Mode Capabilty						*/
/*										*/
/********************************************************************************/

private static class ThermostatMode extends CatbridgeSmartThingsCapability {

ThermostatMode() {
   super("Thermostat Mode","thermostatMode");
}

@Override public void addToDevice(CatreDevice d) {
   CatreParameter bp = d.getUniverse().createEnumParameter("thermostatMode",THERMOSTAT_MODE.OFF);
   bp.setLabel(d.getLabel() + " Mode");
   addTarget(d,bp);
   CatreParameter tmp = d.getUniverse().createEnumParameter("Mode",THERMOSTAT_MODE.AUTO);
   addSmartThingsTransition(d,bp,null,"Set Mode","setThermostatMode",tmp,THERMOSTAT_MODE.AUTO);
}

}	// end of inner class ThermostatFanMode




/********************************************************************************/
/*										*/
/*	Thermostat Operating State Capability					*/
/*										*/
/********************************************************************************/

private static class ThermostatOperatingState extends CatbridgeSmartThingsCapability {

   ThermostatOperatingState() {
      super("Thermostat Operating State","thermostatOperatingState");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp7 = d.getUniverse().createEnumParameter("thermostatOperatingState",THERMOSTAT_STATE);
      bp7.setLabel(d.getLabel() + " Operating State");
      addSensor(d,bp7);
    }

}	// end of inner class ThermostatOperatingState




/********************************************************************************/
/*										*/
/*	Thermostat Setpoint Capabilty						*/
/*										*/
/********************************************************************************/

private static class ThermostatSetpoint extends CatbridgeSmartThingsCapability {
   
   ThermostatSetpoint() {
      super("Thermostat Setpoint","thermostatSetpoint");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createIntParameter("thermostatSetpoint",0,100);
      bp.setLabel(d.getLabel() + " Set Point");
      addSensor(d,bp);
    }

}	// end of inner class ThermostatSetpoint


/********************************************************************************/
/*										*/
/*	ThreeAxis Sensor							*/
/*										*/
/********************************************************************************/

private static class ThreeAxis extends CatbridgeSmartThingsCapability {
   
   ThreeAxis() {
      super("ThreeAxis","threeAxis");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bpx = d.getUniverse().createRealParameter("x");
      bpx.setLabel(d.getLabel() + " X");
      CatreParameter bpy = d.getUniverse().createRealParameter("y");
      bpy.setLabel(d.getLabel() + " Y");
      CatreParameter bpz = d.getUniverse().createRealParameter("z");
      bpz.setLabel(d.getLabel() + " Z");
      addSensor(d,bpx);
      addSensor(d,bpy);
      addSensor(d,bpz);
    }

}	// end of inner class ThreeAxis



/********************************************************************************/
/*										*/
/*	Tone Capability 							*/
/*										*/
/********************************************************************************/

private static class Tone extends CatbridgeSmartThingsCapability {
   
   Tone() {
      super("Tone","tone");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      addSmartThingsTransition(d,null,null,"Beep","beep");
    }
   
}	// end of inner class Tone



/********************************************************************************/
/*										*/
/*	Touch Sensor Capability 						*/
/*										*/
/********************************************************************************/

private static class TouchSensor extends CatbridgeSmartThingsCapability {
   
   private static enum TouchState { TOUCHED, NOT_TOUCHED };
   
   TouchSensor() {
      super("Touch Sensor","touch");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("touch",TouchState.NOT_TOUCHED);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }

}	// end of inner class TouchSensor



/********************************************************************************/
/*										*/
/*	Valve Capability							*/
/*										*/
/********************************************************************************/

private static class Valve extends CatbridgeSmartThingsCapability {
   
   private static enum VALVE_STATE { CLOSED, OPEN };
   
   Valve() {
      super("Valve","valve");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("state",VALVE_STATE.CLOSED);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
      addSmartThingsTransition(d,bp,VALVE_STATE.OPEN,"Open","open");
      addSmartThingsTransition(d,bp,VALVE_STATE.CLOSED,"Close","close");
    }
   
}	// end of inner class Valve




/********************************************************************************/
/*										*/
/*	Water Sensor Capability 						*/
/*										*/
/********************************************************************************/


private static class WaterSensor extends CatbridgeSmartThingsCapability {
   
   private static enum WaterState { DRY, WET };
   
   WaterSensor() {
      super("Water Sensor","waterSensor");
    }
   
   @Override public void addToDevice(CatreDevice d) {
      CatreParameter bp = d.getUniverse().createEnumParameter("waterSensor",WaterState.DRY);
      bp.setLabel(d.getLabel() + " State");
      addSensor(d,bp);
    }
   
}	// end of inner class WaterSensor




/********************************************************************************/
/*										*/
/*	Field set transition							*/
/*										*/
/********************************************************************************/

protected void addSmartThingsTransition(CatreDevice ud,CatreParameter p,Object state,
      String lbl,String rtn)
{
   addSmartThingsTransition(ud,p,state,lbl,rtn,null,null,false);
}


protected void addSmartThingsTransition(CatreDevice ud,CatreParameter p,Object state,
      String lbl,String rtn,boolean force)
{
   addSmartThingsTransition(ud,p,state,lbl,rtn,null,null,force);
}


protected void addSmartThingsTransition(CatreDevice ud,CatreParameter p,Object state,
      String lbl,String rtn,CatreParameter tp,Object dflt)
{
   addSmartThingsTransition(ud,p,state,lbl,rtn,tp,dflt,false);
}


protected void addSmartThingsTransition(CatreDevice ud,CatreParameter p,Object state,
      String lbl,String rtn,CatreParameter tp,Object dflt,boolean force)
{
   CatbridgeSmartThingsDevice std = (CatbridgeSmartThingsDevice) ud;
   SetTransition st = new SetTransition(std,lbl,p,state,rtn,tp,dflt,force);
   std.addTransition(st);
}



private class SetTransition extends CatdevTransition {
   
   private String transition_name;
   private String transition_label;
   private Object field_value;
   private CatreParameter for_parameter;
   private String routine_name;
   private boolean force_on;
   
   SetTransition(CatreDevice d,String name,CatreParameter p,Object value,
         String rtn,CatreParameter tp,
         Object tval,boolean force) {
      super(d.getUniverse());
      transition_label = name;
      transition_name = name.replaceAll(" ",NSEP);
      for_parameter = p;
      field_value = value;
      routine_name = rtn;
      if (tp != null) {
         addParameter(tp,tval);
       }
      force_on = force;
    }
   
   @Override public String getName()		{ return transition_name; }
   @Override public String getLabel()		{ return transition_label; }
   @Override public String getDescription()	{ return transition_label; }
   @Override public CatreTransitionType getTransitionType() { 
      return CatreTransitionType.STATE_CHANGE;
    }
   
   @Override public void perform(CatreWorld w,CatreDevice d,CatrePropertySet params)
   throws CatreActionException {
      if (d == null) throw new CatreActionException("No entity to act on");
      if (w == null) throw new CatreActionException("No world to act in");
      CatbridgeSmartThingsDevice std = (CatbridgeSmartThingsDevice) d;
      CatbridgeSmartThings stu = (CatbridgeSmartThings) d.getBridge();
      
      synchronized (command_lock) {
         CatreLog.logD("CATBRIDGE","START PERFORM " + transition_name + " " + routine_name + " " +
               field_value + " " + for_parameter + " " + force_on);
         
         if (force_on) forceOn(w,std);
         
         if (w.isCurrent()) {
            JSONObject rqst = new JSONObject();
            rqst.put("call",routine_name);
            if (params != null) {
               for (Map.Entry<String,Object> ent : params.entrySet()) {
                  Object o = ent.getValue();
                  String nm = ent.getKey();
                  CatreLog.logD("CATBRIDGE","CALL PARAMETER: " + nm + " " + o.getClass() + " " + o);
                  if (o instanceof Color) {
                     Color c = (Color) o;
                     StringWriter sw = new StringWriter();
                     PrintWriter pw = new PrintWriter(sw);
                     pw.format("#%02x%02x%02x",c.getRed(),c.getGreen(),c.getBlue());
                     float [] hsb = Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
                     JSONObject obj = new JSONObject();
                     obj.put("hex",sw.toString());
                     obj.put("hue",hsb[0]);
                     obj.put("saturation",hsb[1]);
                     obj.put("level",hsb[2]);
                     rqst.put(nm,obj);
                   }
                  else if (o instanceof Enum) {
                     String obj = o.toString();
                     obj = obj.toLowerCase();
                     rqst.put(nm,o);
                   }
                  else {
                     rqst.put(nm,o);
                   }
                }
             }
            stu.sendCommand(getAccessName(),std,rqst);
          }
         else if (for_parameter != null) {
            if (field_value != null) {
               d.setValueInWorld(for_parameter,field_value,w);
             }
            else {
               for (Object o : params.values()) {
                  d.setValueInWorld(for_parameter,o,w);
                  break;
                }
             }
          }
         
         CatreLog.logD("CATBRIDGE","END PERFORM");
       }
    }
   
   private void forceOn(CatreWorld w,CatbridgeSmartThingsDevice std) {
      String cmd = null;
      CatreParameter param = null;
      Object value = null;
      String acc = null;
      
      if (std.hasCapability("Switch")) {
         cmd = "on";
         param = std.findParameter("switch");
         value = "ON";
         acc = "switch";
       }
      else if (std.hasCapability("Relay Switch")) {
         cmd = "on";
         param = std.findParameter("switch");
         value = "ON";
         acc = "relaySwitch";
       }
      else {
         CatreLog.logD("CATBRIDGE","ATTEMPT TO FORCE ON WITHOUT CAPABILITY");
         CatreLog.logD("CATBRIDGE","   DEVICE: " + std);
       }
      
      if (cmd == null) return;
      
      if (w.isCurrent()) {
         CatbridgeSmartThings stu = (CatbridgeSmartThings) std.getBridge();
         JSONObject rqst = new JSONObject();
         rqst.put("call",cmd);
         stu.sendCommand(acc,std,rqst);
       }
      else if (param != null && value != null) {
         std.setValueInWorld(param,value,w);
       }
    }

}	// end of inner class SetTransition






}       // end of class CatbridgeSmartThingsCapability




/* end of CatbridgeSmartThingsCapability.java */

