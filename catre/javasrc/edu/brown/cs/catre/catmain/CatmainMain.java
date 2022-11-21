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
   thread_pool = new TimerThreadPool(); 
   CatstoreFactory cf = new CatstoreFactory(this);
   data_store = cf.getStore();
   model_factory = new CatmodelFactory(this);
   bridge_factory = new CatbridgeFactory(this);
   
   rest_server = new CatserveServer(this);
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



@Override public void addRoute(String method,String url,BiFunction<IHTTPSession,CatreSession,Response> f)
{
   rest_server.addRoute(method,url,f);
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

