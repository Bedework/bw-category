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
package org.bedework.category.web;

import org.bedework.category.common.Category;
import org.bedework.category.common.SearchResult;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.misc.Util;
import org.bedework.util.servlet.ReqUtil;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle POST for categories servlet.
 */
public class GetMethod extends CategoryMethodBase {
  @SuppressWarnings({"unchecked"})
  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    try {
      final List<String> resourceUri = getResourceUri(req);

      if (Util.isEmpty(resourceUri)) {
        throw new ServletException("Bad resource url - no path specified");
      }

      final String resName = resourceUri.get(0);

      if (resName.equals("category")) {
        processCategory(resourceUri, req, resp);
        return;
      }

      if (resName.equals("categories")) {
        processCategories(resourceUri, req, resp);
        return;
      }

      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (final ServletException se) {
      throw se;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }

  private void processCategory(final List<String> resourceUri,
                               final HttpServletRequest req,
                               final HttpServletResponse resp) throws ServletException {
    final ReqUtil rutil = new ReqUtil(req, resp);

    final String href = hrefFromPath(resourceUri, 1);

    if (href == null) {
      sendJsonError(resp, "failed");
      return;
    }
    
    boolean fromRemote = false;

    try {
      Category cat = getIndex().getCategory(href);

      if ((cat == null) && !config.getPrimaryServer()) {
        // Try remote retrievals
        cat = getRemote(href);
        fromRemote = true;
      }

      if (cat == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      
      final String accept = req.getHeader("Accept");
      final String format = rutil.getReqPar("format");
      
      boolean rdf ="rdf".equals(format);
      boolean html ="html".equals(format);
      
      if (!rdf && !html) {
        rdf = (accept != null) &&
                 (accept.contains("application/xml") ||
                          (accept.contains("application/rdf+xml")));

        html = (accept != null) &&
                accept.contains("text/html");
      }

      if (rdf) {
        writeRdf(cat, resp);
      } else if (html) {
        writeHtml(cat, resp);
      } else {
        writeJson(cat, resp);
      }
      
      if (fromRemote) {
        // Should we cache it?
        if (config.getToken() == null) {
          return;
        }

        final String token = rutil.getReqPar("token");
        final boolean store = rutil.present("store");
        
        if (!store || !config.getToken().equals(token)) {
          return;
        }
        
        // TODO - store the category
      }
    } catch (final ServletException se) {
      throw se;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }

  private void processCategories(final List<String> resourceUri,
                                 final HttpServletRequest req,
                                 final HttpServletResponse resp) throws ServletException {
    final ReqUtil rutil = new ReqUtil(req, resp);

    try {
      final boolean primary = rutil.present("primary");
      final int from = rutil.getIntReqPar("from", 0);
      final int ct = rutil.getIntReqPar("ct", 30);
      final boolean href = rutil.getBooleanReqPar("href", false);
      final String q = rutil.getReqPar("q");
      
      if (q == null) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      
      final String pfx = rutil.getReqPar("pfx");

      final SearchResult sr;
      if (primary) {
        sr = findRemote(q, pfx, href, from, ct);
      } else {
        sr = getIndex().find(q, pfx, href, from, ct);
      }

      final String accept = req.getHeader("Accept");

      final boolean html =
              "html".equals(rutil.getReqPar("format")) ||
                      ((accept != null) &&
                               accept.contains("text/html"));

      if (html) {
        writeHtml(sr, resp, href);
        return;
      }

      writeJson(sr, resp);
    } catch (final ServletException se) {
      throw se;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}

