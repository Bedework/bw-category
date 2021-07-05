/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.common;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: mike Date: 7/4/21 Time: 21:39
 */
public class CatUtil {
  public static URIBuilder fromServerUrl(final String serverUrl,
                                         final List<String> pathSegments,
                                         final String prefix,
                                         final NameValuePair... nvps) {
    try {
      final List<String> pathSegs = new ArrayList<>();
      pathSegs.add("categories");

      final URI uri = new URI(serverUrl);

      if (uri.getPath() != null) {
        pathSegs.add(uri.getPath());
      }

      pathSegs.addAll(pathSegments);

      final URIBuilder urib = new URIBuilder()
              .setPathSegments(pathSegs)
              .setScheme(uri.getScheme())
              .setHost(uri.getHost())
              .setPort(uri.getPort());

      urib.addParameters(Arrays.asList(nvps));

      if (prefix != null) {
        urib.addParameter("pfx", prefix);
      }

      return urib;
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
