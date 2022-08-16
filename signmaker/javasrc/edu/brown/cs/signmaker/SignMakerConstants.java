/********************************************************************************/
/*                                                                              */
/*              SignMakerConstants.java                                         */
/*                                                                              */
/*      Constants for code to make sign image from text specification           */
/*                                                                              */
/********************************************************************************/


package edu.brown.cs.signmaker;

import java.io.InputStream;

public interface SignMakerConstants
{

int     DEFAULT_WIDTH = 2560;


enum SignImageType {
   NONE,
   URL,
   FILE,
   FONT_AWESOME,
   QR,
}

String DATABASE = "postgres://spr:XXXXXX@db.cs.brown.edu/iqsign";

int SERVER_PORT = 3399;


interface SignMakerParser {
   SignMakerSign parse(InputStream ins) throws SignMakerException;
}

}       // end of interface SignMakerConstants




/* end of SignMakerConstants.java */

