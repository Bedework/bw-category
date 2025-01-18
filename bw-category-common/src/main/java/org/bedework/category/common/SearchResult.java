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

import org.bedework.base.ToString;
import org.bedework.base.response.Response;

import java.util.Set;
import java.util.TreeSet;

/**
 * User: mike Date: 3/9/16 Time: 23:54
 */

public class SearchResult extends Response {
  private Set<SearchResultItem> items;
  private long found;

  public SearchResult() {
  }

  public SearchResult(final Status status) {
    setStatus(status);
  }

  public SearchResult(final long found) {
    this.found = found;
  }

  public long getFound() {
    return found;
  }
  
  public Set<SearchResultItem> getItems() {
    if (items == null) {
      items = new TreeSet<>();
    }

    return items;
  }

  public void addItem(final SearchResultItem val) {
    getItems().add(val);
  }

  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("found", getFound());
    ts.append("items", getItems());
  }
} 
