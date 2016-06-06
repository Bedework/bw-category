/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.category.service;

import javax.naming.NameNotFoundException;

/**
 * User: mike
 * Date: 4/17/15
 * Time: 12:27 AM
 */
public abstract class ProcessorThread extends Thread {
  public static final String statusDone = "Done";
  public static final String statusFailed = "Failed";
  public static final String statusRunning = "Running";
  public static final String statusStopped = "Stopped";
  
  /** cannot stop the thread */
  public static final String statusUnstoppable = "Unstoppable";

  private String status = statusStopped;

  private boolean running;

  public ProcessorThread(final String name) {
    super(name);
  }

  /**
   * @param msg an error message
   */
  public abstract void error(String msg);

  /**
   * @param t a Throable
   */
  public abstract void error(Throwable t);

  /**
   * @param msg an info message
   */
  public abstract void info(String msg);

  /**
   * @param msg a warning message
   */
  public abstract void warn(String msg);

  /** called at end - allows output of termination messsages
   * @param msg an info message
   */
  public abstract void end(String msg);

  public boolean getRunning() {
    return running;
  }

  public void setRunning(final boolean val) {
    running = val;
  }

  protected boolean handleException(final Throwable val) {
//            if (!(val instanceof NotificationException)) {
    //              return false;
    //        }

    final Throwable t = val.getCause();
    if (t instanceof NameNotFoundException) {
      // jmx shutting down?
      error("Looks like JMX shut down.");
      error(t);
      running = false;
      return true;
    }

    return false;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String val) {
    status = val;
  }

  /** Check for processor started
   * 
   * @param processor
   * @return status string.
   */
  public static String checkStarted(final ProcessorThread processor) {
    if (processor == null) {
      return statusStopped;
    }

    if (!processor.isAlive()) {
      return statusStopped;
    }

    if (processor.running) {
      return statusRunning;
    }

    /* Kill it and return false */
    processor.interrupt();
    try {
      processor.join(5000);
    } catch (final Throwable ignored) {}

    if (!processor.isAlive()) {
      return statusStopped;
    }

    return statusUnstoppable;
  }

  public boolean isRunning() {
    return running;
  }

  public static void stop(final ProcessorThread processor) {
    if (processor == null) {
      return;
    }

    processor.info("************************************************************");
    processor.info(" * Stopping " + processor.getName());
    processor.info("************************************************************");

    processor.running = false;
    //?? ProcessorThread.stopProcess(processor);

    processor.interrupt();
    try {
      processor.join(20 * 1000);
    } catch (final InterruptedException ignored) {
    } catch (final Throwable t) {
      processor.error("Error waiting for processor termination");
      processor.error(t);
    }

    processor.info("************************************************************");
    processor.info(" * " + processor.getName() + " terminated");
    processor.info("************************************************************");
  }
}
