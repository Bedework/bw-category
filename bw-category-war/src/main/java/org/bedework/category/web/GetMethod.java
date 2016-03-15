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

    try {
      Category cat = getIndex().getCategory(href);

      if (cat == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      writeJson(cat, resp);
    } catch (final ServletException se) {
      throw se;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }
}

