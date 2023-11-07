/********************************************************************************/
/*										*/
/*		CatmainMain.java						*/
/*										*/
/*	Main program for Continuous and Trigger-based Rule Environment		*/
/*										*/
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




package edu.brown.cs.catre.catmain;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

import edu.brown.cs.catre.catbridge.CatbridgeFactory;
import edu.brown.cs.catre.catmodel.CatmodelFactory;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catserve.CatserveServer;
import edu.brown.cs.catre.catstore.CatstoreFactory;
import edu.brown.cs.ivy.file.IvyLog.LogLevel;

public class CatmainMain implements CatmainConstants, CatreController
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   CatmainMain main = new CatmainMain(args);

   main.start();
}


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ScheduledThreadPoolExecutor	thread_pool;
private CatserveServer rest_server;
private CatreStore     data_store;
private CatmodelFactory model_factory;
private CatbridgeFactory bridge_factory;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private CatmainMain(String [] args)
{
   CatreLog.setLogLevel(LogLevel.DEBUG);
   CatreLog.setupLogging("CATRE",true);
   CatreLog.useStdErr(true);

   thread_pool = new TimerThreadPool();

   CatstoreFactory cf = new CatstoreFactory(this);
   data_store = cf.getStore();

   //BUG: data_store isn't being loaded in properly!

   CatreLog.logD("CATMAIN","data_store " + data_store.findAllUsers().size());

   model_factory = new CatmodelFactory(this);

   rest_server = new CatserveServer(this);

   // bridge_factory = new CatbridgeFactory(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override
public CatreStore getDatabase()
{
   return data_store;
}



@Override
public void register(CatreTable tbl)
{
   data_store.register(tbl);
}



@Override
public Collection<CatreBridge> getAllBridges(CatreUniverse cu)
{
   return bridge_factory.getAllBridges(cu);
}

@Override
public CatreBridge createBridge(String name,CatreUniverse cu)
{
   return bridge_factory.createBridge(name,cu);
}




/********************************************************************************/
/*										*/
/*	Model creation methods							*/
/*										*/
/********************************************************************************/

@Override
public CatreUniverse createUniverse(String name,CatreUser cu)
{
   CatreUniverse universe = model_factory.createUniverse(this,name,cu);

   cu.setUniverse(universe);

   return universe;
}



/********************************************************************************/
/*										*/
/*	Task methods								*/
/*										*/
/********************************************************************************/

@Override
public ScheduledFuture<?> schedule(Runnable task,long delay)
{
   ScheduledFuture<?> f =  thread_pool.schedule(task,delay,TimeUnit.MILLISECONDS);
   return f;
}


@Override
public ScheduledFuture<?> schedule(Runnable task,long delay,long period)
{
   ScheduledFuture<?> f = thread_pool.scheduleAtFixedRate(task,delay,period,TimeUnit.MILLISECONDS);

   return f;
}


@Override
public Future<?> submit(Runnable task)
{
   return thread_pool.submit(task);
}


public <T> Future<T> submit(Runnable task,T result)
{
   return thread_pool.submit(task,result);
}


public <T> Future<T> submit(Callable<T> task)
{
   return thread_pool.submit(task);
}



/********************************************************************************/
/*										*/
/*	Other access methods							*/
/*										*/
/********************************************************************************/

//TODO -- rework this back in with HTTP
// @Override public void addRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f)
// {
//    rest_server.addRoute(method,url,f);
// }


// @Override public void addPreRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f)
// {
//    rest_server.addPreRoute(method,url,f);
// }


@Override public File findBaseDirectory()
{
   File basedir = null;

   File f1 = new File(System.getProperty("user.dir"));
   for (File f2 = f1; f2 != null; f2 = f2.getParentFile()) {
      if (isBaseDirectory(f2)) basedir = f2;
    }
   File f3 = new File(System.getProperty("user.home"));
   if (isBaseDirectory(f3)) basedir = f3;

   File fc = new File("/vol");
   File fd = new File(fc,"iot");
   if (isBaseDirectory(fd)) basedir = fd;

   File fa = new File("/pro");
   File fb = new File(fa,"iot");
   if (isBaseDirectory(fb)) basedir = fb;

   File fe = new File("/private");
   File ff = new File(fe,"iot");
   if (isBaseDirectory(ff)) basedir = ff;

   return basedir;
}


private static boolean isBaseDirectory(File dir) {
   File f2 = new File(dir,"secret");

   if (!f2.exists()) return false;

   File f3 = new File(f2,"Database.props");
   File f5 = new File(dir,"svgimagelib");
   if (f3.exists() && f5.exists()) return true;

   return false;
}



/********************************************************************************/
/*										*/
/*	Working methods 							*/
/*										*/
/********************************************************************************/

private void start() {
   for (CatreUser cu : data_store.findAllUsers()) {
      CatreUniverse universe = cu.getUniverse();
      CatreLog.logD("CATMAIN","START universe " + universe.getName());
      universe.start();
      // update program for this universe to handle missing devices
      // start program for this user/universe
    }
   
   for (CatreUser cu : data_store.findAllUsers()) {
      CatreLog.logD("CATMAIN","START bridges " + cu.getUserName());
      bridge_factory.setupForUser(cu);
    }
   
   try {
      rest_server.start();
    }
   catch (IOException e) {
      // handle failure to start
    }
}



/********************************************************************************/
/*										*/
/*	Thread pool methods							*/
/*										*/
/********************************************************************************/

private class TimerThreadPool extends ScheduledThreadPoolExecutor {

   TimerThreadPool() {
      super(THREAD_POOL_SIZE,new TimerThreadFactory());
    }

}	// end of inner class TimerThreadPool


private static class TimerThreadFactory implements ThreadFactory {

   private int thread_counter;

   TimerThreadFactory() {
      thread_counter = 0;
    }

   @Override public Thread newThread(Runnable r) {
      return new TimerThread(++thread_counter,r);
    }

}	// end of inner class TimerThreadFactory


private static class TimerThread extends Thread implements CatreLog.LoggerThread  {

   private int thread_count;

   TimerThread(int ct,Runnable r) {
      super(r,"CatreExec_" + ct);
      thread_count = ct;
    }

   @Override public int getLogId()			{ return thread_count; }

}	// end of inner class TimerThread



}	// end of class CatmainMain




/* end of CatmainMain.java */

