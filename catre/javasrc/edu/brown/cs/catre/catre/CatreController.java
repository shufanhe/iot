/********************************************************************************/
/*                                                                              */
/*              CatreUniverse.java                                              */
/*                                                                              */
/*      Universe for a single user                                              */
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

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiFunction;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

/**
 *      The universe is the container for everything for all users.  It is the
 *      basis for the global controller that handles triggering rule evaluation.
 **/
      
public interface CatreController 
{


ScheduledFuture<?> schedule(Runnable task,long delay);
ScheduledFuture<?> schedule(Runnable task,long delay,long period);


Future<?> submit(Runnable task);
<T> Future<T> submit(Runnable task,T result);
<T> Future<T> submit(Callable<T> task);


CatreStore getDatabase();
void register(CatreTable tbl);


Collection<CatreBridge> getAllBridges(CatreUniverse universe);
CatreBridge createBridge(String name,CatreUniverse universe);


CatreUniverse createUniverse(String name,CatreUser user);

void addRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f);
void addPreRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f);

File findBaseDirectory();







}       // end of interface CatreUniverse




/* end of CatreUniverse.java */

