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
package org.bedework.category.common;

import org.bedework.util.misc.ToString;

import static org.bedework.category.common.Response.Status.ok;

/**
 * User: mike Date: 3/9/16 Time: 23:54
 */

public class Response {
  public enum Status {
    ok,
    
    notFound,
    
    failed,
    
    exception
  }
  private Status status;
  private String message;

  public Response() {
    status = ok;
  }

  public Response(final Status status) {
    this(status, null);
  }

  public Response(final Status status, 
                  final String message) {
    this.status = status;
    this.message = message;
  }

  public Status  getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public void toStringSegment(final ToString ts) {
    ts.append("status", getStatus());
    ts.append("message", getMessage());
  }
  
  public String toString() {
    final ToString ts = new ToString(this);
    
    toStringSegment(ts);
    
    return toString();
  }
} 
