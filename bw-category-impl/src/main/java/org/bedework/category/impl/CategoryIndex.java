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
package org.bedework.category.impl;

import org.bedework.category.common.Category;
import org.bedework.category.common.CategoryConfigProperties;
import org.bedework.category.common.CategoryException;
import org.bedework.category.common.SearchResult;
import org.bedework.category.common.SearchResultItem;
import org.bedework.util.opensearch.EsDocInfo;
import org.bedework.util.opensearch.EsUtil;
import org.bedework.util.opensearch.IndexProperties;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.response.Response;

import org.opensearch.action.bulk.BulkProcessor;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.document.DocumentField;
import org.opensearch.common.lucene.search.function.FieldValueFactorFunction;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.PrefixQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.bedework.util.misc.response.Response.Status.failed;

/**
 * User: mike Date: 3/13/16 Time: 16:11
 */
public class CategoryIndex implements Logged {
  private final IndexProperties idxprops;
  private final CategoryConfigProperties conf;
  
  public CategoryIndex(final IndexProperties idxprops,
                       final CategoryConfigProperties conf)
          throws CategoryException {
    this.idxprops = idxprops;
    this.conf = conf;
  }

  /** Create a new index and make it current
   * 
   * @return new index name
   * @throws CategoryException on fatal error
   */
  public String newIndex() throws CategoryException {
    try {
      return getEsUtil().newIndex(conf.getIndexName(),
                                  conf.getIndexMapping());
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }
  
  public List<String> purgeIndexes() throws CategoryException {
    try {
      return getEsUtil().purgeIndexes(
              Collections.singleton(conf.getIndexName()));
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }


  /** Parse the dmoz data and store it in the current index
   * 
   * @param infoLines for messsages
   * @throws CategoryException on fatal error
   */
  public void parseDmoz(final InfoLines infoLines,
                        final String indexName) throws CategoryException {
    final long startTime = System.currentTimeMillis();

    final DmozStructureParser parser =
            new DmozStructureParser(conf) {
              @Override
              public void saveCategory(final Category cat)
                      throws CategoryException {
                CategoryIndex.this.saveCategory(cat,
                                                indexName);
              }
            };

    parser.parse();

    parser.stats(infoLines);

    final String times = "Index build: " + elapsed(startTime);
    infoLines.addLn(times);
  }

  /** Save the category object in the current index
   * 
   * @param cat the category
   * @param indexName null for default           
   * @throws CategoryException on fatal error
   */
  public void saveCategory(final Category cat,
                           final String indexName)
          throws CategoryException {
    final EsDocInfo di = makeDoc(cat);
    
    try {
      final String targetIndex;
      if (indexName == null) {
        targetIndex = conf.getIndexName();
      } else {
        targetIndex = indexName;
      }
      
      getEsUtil().indexDoc(di, targetIndex);
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }
  
  public Category getCategory(final String href) throws CategoryException {
    if (debug()) {
      debug("getCategory: target=" + conf.getIndexName() + " href=" + href);
    }

    final GetResponse gr;
    try {
      gr = getEsUtil().get(conf.getIndexName(),
                           Category.docType,
                           href);

      if (gr == null) {
        return null;
      }

      return new EntityBuilder(gr.getSourceAsMap(),
                               gr.getVersion()).makeCategory();
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }
  
  private QueryBuilder qtype1(final String val,
                              final String prefix,
                              final String fuzziness) {
    final BoolQueryBuilder qb = new BoolQueryBuilder();
    if (prefix != null) {
      qb.must(new PrefixQueryBuilder("href", prefix));
    }

    final MultiMatchQueryBuilder mmqb =
            new MultiMatchQueryBuilder(val,
                                       "href.autocathref")
                    .field("last.matcher", 5);

    if (fuzziness != null) {
      mmqb.fuzziness(fuzziness);
    }

    qb.should(mmqb);

    /* Higher score for shorter path length */

    return new FunctionScoreQueryBuilder(
            qb, ScoreFunctionBuilders
            .fieldValueFactorFunction("hrefDepth")
            .factor(10)
            .modifier(FieldValueFactorFunction.Modifier.RECIPROCAL));
  }

  private QueryBuilder qtype2(final String val,
                              final String prefix,
                              final String fuzziness) {
    final BoolQueryBuilder qb = new BoolQueryBuilder();
    if (prefix != null) {
      qb.must(new PrefixQueryBuilder("href", prefix));
    }

    qb.mustNot(new PrefixQueryBuilder("href", "/dmoz/World"));

    final MultiMatchQueryBuilder mmqb =
            new MultiMatchQueryBuilder(val,
                                       "href.autocathref")
                    .field("last.matcher", 5)
            .type(MultiMatchQueryBuilder.Type.MOST_FIELDS);

    if (fuzziness != null) {
      mmqb.fuzziness(fuzziness);
    }

    qb.should(mmqb);

    return qb;
  }
  
  public SearchResult find(final String val,
                           final String prefix,
                           final boolean hrefs,
                           final int from,
                           final int size) {
    try {
      if (val == null) {
        return Response.notOk(new SearchResult(),
                              failed, "Must supply query");
      }

      final RestHighLevelClient cl = getEsUtil().getClient();

      String qstring;
      final int qtype;
      if (val.startsWith("!qt1 ")) {
        qtype = 1;
        qstring = val.substring(5);
      } else if (val.startsWith("!qt2 ")) {
        qtype = 1;
        qstring = val.substring(5);
      } else {
        qtype = 2;
        qstring = val;
      }

      final String fuzziness;
      if (qstring.startsWith("* ")) {
        fuzziness = "AUTO";
        qstring = qstring.substring(2);
      } else {
        fuzziness = null;
      }

      final QueryBuilder qb;
      
      if (qtype == 1) {
        qb = qtype1(qstring, prefix, fuzziness);
      } else {
        qb = qtype2(qstring, prefix, fuzziness);
      }

      final SearchSourceBuilder ssb =
              new SearchSourceBuilder()
                      .size(size)
                      .query(qb);

      final SearchRequest sreq = new SearchRequest(conf.getIndexName())
              .source(ssb);

      final SearchResponse resp;
      try {
        resp = getClient().search(sreq, RequestOptions.DEFAULT);
      } catch (final Throwable t) {
        return Response.error(new SearchResult(), t);
      }

//    if (resp.status() != RestStatus.OK) {
      //TODO
//    }

      final long found =
              resp.getHits().getTotalHits().value;
      if (debug()) {
        debug("find: returned status " + resp.status() +
                      " found: " + found);
      }

      int sz = 0;
      final SearchHits hits = resp.getHits();

      if (hits.getHits() != null) {
        sz = hits.getHits().length;
      }

      //Break condition: No hits are returned
      if (sz == 0) {
        return new SearchResult();
      }

      final SearchResult sr = new SearchResult(found);

      for (final SearchHit hit : hits) {
        final String kval = hit.getId();

        if (kval == null) {
          sr.addItem(new SearchResultItem(failed, 
                                          "org.bedework.index.noitemkey"));
          continue;
        }

        final Map<String, DocumentField> fields = hit.getFields();

        final String fldval = null;
        final String href = null;

        final Category cat = new EntityBuilder(hit.getSourceAsMap(),
                                               0).makeCategory();

        if (hrefs) {
          sr.addItem(new SearchResultItem(cat.getHref(),
                                          hit.getScore()));
        } else {
          sr.addItem(new SearchResultItem(cat, hit.getScore()));
        }
      }

      return sr;
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }

      return Response.notOk(new SearchResult(), failed, t.getLocalizedMessage());
    }
  }
  
  public void makeProduction(final String indexName) throws CategoryException {
    try {
      getEsUtil().swapIndex(indexName, conf.getIndexName());
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  private class BulkListener implements BulkProcessor.Listener {

    @Override
    public void beforeBulk(final long executionId,
                           final BulkRequest request) {
    }

    @Override
    public void afterBulk(final long executionId,
                          final BulkRequest request,
                          final BulkResponse response) {
    }

    @Override
    public void afterBulk(final long executionId,
                          final BulkRequest request,
                          final Throwable failure) {
      error(failure);
    }
  }

  public void reIndex(final String toIndex) throws CategoryException {
    long processed = 0;
    final QueryBuilder qb = QueryBuilders.matchAllQuery();

    final int timeoutMillis = 60000;  // 1 minute
    final TimeValue tv = new TimeValue(timeoutMillis);
    final int batchSize = 100;

    final RestHighLevelClient cl = getClient();

    final BulkListener listener = new BulkListener();

    final BulkProcessor.Builder builder = BulkProcessor.builder(
            (request, bulkListener) ->
                    cl.bulkAsync(request, RequestOptions.DEFAULT,
                                 bulkListener),
            listener);

    final BulkProcessor bulkProcessor =
            builder.setBulkActions(batchSize)
                   .setConcurrentRequests(3)
                   .setFlushInterval(tv)
                   .build();

    final SearchSourceBuilder ssb =
            new SearchSourceBuilder()
                    .size(batchSize)
                    .query(qb);

    final SearchRequest sr = new SearchRequest(conf.getIndexName())
            .source(ssb)
            .scroll(tv);

    try {
      SearchResponse scrollResp = cl.search(sr, RequestOptions.DEFAULT);

      if (scrollResp.status() != RestStatus.OK) {
        if (debug()) {
          debug("Search returned status " + scrollResp.status());
        }
      }

      //Scroll until no hits are returned
      while (true) {
        for (final SearchHit hit : scrollResp.getHits().getHits()) {
          processed++;
          if ((processed % 500) == 0) {
            info("Processed: " + processed);
          }

          final Category cat = makeCat(hit);
          if (cat == null) {
            warn("Unable to build category " + hit.getSourceAsString());
            continue;
          }

          try {
            final EsDocInfo di = makeDoc(cat);
          
            final IndexRequest request =
                    new IndexRequest(toIndex);

            request.id(di.getId());

            request.source(di.getSource());
            bulkProcessor.add(request);
          } catch (final CategoryException pe) {
            warn("Unable to build document " + hit.getSourceAsString());
            continue;
          }
        }

        final SearchScrollRequest scrollRequest =
                new SearchScrollRequest(scrollResp.getScrollId());
        scrollRequest.scroll(tv);
        scrollResp = getClient().scroll(scrollRequest,
                                        RequestOptions.DEFAULT);

        //Break condition: No hits are returned
        if (scrollResp.getHits().getHits().length == 0) {
          break;
        }
      }

      try {
        bulkProcessor.awaitClose(10, TimeUnit.MINUTES);
      } catch (final InterruptedException e) {
        warn("Final bulk close was interrupted. Records may be missing");
      }
    } catch (final Throwable t) {
      error(t);
    }
  }
  
  private Category makeCat(final SearchHit hit) throws CategoryException {
    try {
      return new EntityBuilder(hit.getSourceAsMap(),
                               0).makeCategory();
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  private EsDocInfo makeDoc(final Category cat)
          throws CategoryException {
    /* Set up fields derived from href */
    final String hr = cat.getHref();
    cat.setNamespaceAbbrev(hr.substring(0, hr.indexOf('/', 1)));

    final String[] els = hr.split("/");

    final List<Category.HrefElement> hes = new ArrayList<>(els.length);

    /* skip empty strings - should have one at start
       skip length 1 strings - usually just an alpha group
     */

    for (final String s: els) {
      if (s.length() == 0) {
        continue;
      }

      if (s.length() == 1) {
        // Should we checks for some special ones? e.g. "C"
        continue;
      }

      hes.add(new HrefElementImpl(s.replace('_', ' ')));
    }

    cat.setHrefElements(Collections.unmodifiableList(hes));
    cat.setHrefDepth(cat.getHrefElements().size());

    cat.setLast(hes.get(cat.getHrefDepth() - 1).getDisplayName());
    cat.setLowerLast(cat.getLast().toLowerCase());

    return new DocBuilder().makeDoc(cat);
  }

  private EsUtil getEsUtil() throws CategoryException {
    try {
      return new EsUtil(idxprops);
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  private RestHighLevelClient getClient() throws CategoryException {
    try {
      return getEsUtil().getClient();
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  private String elapsed(final long start) {
    final long millis = System.currentTimeMillis() - start;
    long seconds = millis / 1000;
    final long minutes = seconds / 60;
    seconds -= (minutes * 60);

    return "Elapsed time: " + minutes + ":" +
            twoDigits(seconds);
  }

  /**
   * @param val number
   * @return 2 digit val
   */
  private static String twoDigits(final long val) {
    if (val < 10) {
      return "0" + val;
    }

    return String.valueOf(val);
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
