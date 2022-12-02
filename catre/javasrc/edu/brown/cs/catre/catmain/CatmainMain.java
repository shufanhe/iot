/********************************************************************************/
/*                                                                              */
/*              CatmainMain.java                                                */
/*                                                                              */
/*      Main program for Continuous and Trigger-based Rule Environment          */
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
import edu.brown.cs.catre.catserve.CatserveServer;
import edu.brown.cs.catre.catstore.CatstoreFactory;
import edu.brown.cs.ivy.file.IvyLog.LogLevel;
 
public class CatmainMain implements CatmainConstants, CatreController
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   CatmainMain main = new CatmainMain(args);
   
   main.start();
}
 

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ScheduledThreadPoolExecutor     thread_pool;
private CatserveServer rest_server;
private CatreStore     data_store;
private CatmodelFactory model_factory;
private CatbridgeFactory bridge_factory;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CatmainMain(String [] args)
{
   CatreLog.setLogLevel(LogLevel.DEBUG);
   
   thread_pool = new TimerThreadPool(); 
   
   CatstoreFactory cf = new CatstoreFactory(this);
   data_store = cf.getStore();
   
   model_factory = new CatmodelFactory(this);
   
   rest_server = new CatserveServer(this);
   
   bridge_factory = new CatbridgeFactory(this);
}
 


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
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
/*                                                                              */
/*      Model creation methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override
public CatreUniverse createUniverse(String name)
{
   return model_factory.createUniverse(this,name);
}











/********************************************************************************/
/*                                                                              */
/*      Task methods                                                            */
/*                                                                              */
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
/*                                                                              */
/*      Other access methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void addRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f)
{
   rest_server.addRoute(method,url,f);
}


@Override public void addPreRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f)
{
   rest_server.addPreRoute(method,url,f);
}


@Override public File findBaseDirectory()
{
   File f1 = new File(System.getProperty("user.dir"));
   for (File f2 = f1; f2 != null; f2 = f2.getParentFile()) {
      if (isBaseDirectory(f2)) return f2;
    }
   File f3 = new File(System.getProperty("user.home"));
   if (isBaseDirectory(f3)) return f3;
   
   File fc = new File("/vol");
   File fd = new File(fc,"iot");
   if (isBaseDirectory(fd)) return fd;
   
   File fa = new File("/pro");
   File fb = new File(fa,"iot");
   if (isBaseDirectory(fb)) return fb;
   
   return null;
}


private static boolean isBaseDirectory(File dir)
{
   File f2 = new File(dir,"secret");
   if (!f2.exists()) return false;
   
   File f3 = new File(f2,"Database.props");
   File f5 = new File(dir,"svgimagelib");
   if (f3.exists() && f5.exists()) return true;
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Working methods                                                         */
/*                                                                              */
/********************************************************************************/

private void start()
{
   try {
      rest_server.start();
    }
   catch (IOException e) { 
      // handle failure to start
    }
}



/********************************************************************************/
/*                                                                              */
/*      Thread pool methods                                                     */
/*                                                                              */
/********************************************************************************/

private class TimerThreadPool extends ScheduledThreadPoolExecutor {
   
   TimerThreadPool() {
      super(THREAD_POOL_SIZE,new TimerThreadFactory());
    }
   
}       // end of inner class TimerThreadPool


private static class TimerThreadFactory implements ThreadFactory {
   
   private int thread_counter;
   
   TimerThreadFactory() {
      thread_counter = 0;
    }
   
   @Override public Thread newThread(Runnable r) {
      return new TimerThread(++thread_counter,r);
    }
   
}       // end of inner class TimerThreadFactory


private static class TimerThread extends Thread implements CatreLog.LoggerThread  {
   
   private int thread_count;
   
   TimerThread(int ct,Runnable r) {
      super(r,"CatreExec_" + ct);
      thread_count = ct;
    }
   
   @Override public int getLogId()                      { return thread_count; }
   
}       // end of inner class TimerThread



}       // end of class CatmainMain




/* end of CatmainMain.java */

