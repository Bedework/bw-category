/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.ical;

import org.bedework.category.common.CatUtil;
import org.bedework.util.args.Args;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * User: mike Date: 5/4/16 Time: 11:57
 */
public class Categoriser implements Logged {
  void usage(final String error_msg) {
    if (error_msg != null) {
      print(error_msg);
    }

    print("Usage: icalcats [options]\n" +
                  "Options:\n" +
                  "    -h            Print this help and exit\n" +
                  "    --cats        URL of category service\n" +
                  "    --ics         path to ics file\n" +
                  "    --pfx         prefix of categories to limit search\n" +
                  "\n" +
                  "Description:\n" +
                  "    This utility parses iCalendar data and calls the .\n" +
                  "    category service for suggested categories.\n");

    if (error_msg != null) {
      throw new RuntimeException(error_msg);
    }

    System.exit(0);
  }

  String catsUrl = "http://localhost:8080/bwcat/";
  String prefix;
  String ics;

  final CloseableHttpClient cl =
          HttpClients.createDefault();

  boolean processArgs(final Args args) throws Throwable {
    if (args == null) {
      return true;
    }

    while (args.more()) {
      if (args.ifMatch("")) {
        continue;
      }

      if (args.ifMatch("-h")) {
        usage(null);
      } else if (args.ifMatch("--cats")) {
        catsUrl = args.next();
      } else if (args.ifMatch("--ics")) {
        ics = args.next();
      } else if (args.ifMatch("--pfx")) {
        prefix = args.next();
      } else {
        usage("Unrecognized option: " + args.current());
        return false;
      }
    }

    if (ics == null) {
      usage("Must provide an ics file");
      return false;
    }
    return true;
  }

  public static void main(final String[] args) {
    final Categoriser cat = new Categoriser();

    try {
      if (!cat.processArgs(new Args(args))) {
        return;
      }

      final CalendarBuilder builder = new CalendarBuilder();

      final UnfoldingReader ufrdr =
              new UnfoldingReader(new FileReader(cat.ics),
                                  true);

      final Calendar calendar = builder.build(ufrdr);

      for (final Object o : calendar.getComponents()) {
        final Component component = (Component)o;

        if (component.getName().equals(Component.VTIMEZONE)) {
          continue;
        }

        String dtstart = null;
        String summary = null;
        String categories = "     ";
        String val = "";

        for (final Object o1 : component.getProperties()) {
          final Property property = (Property)o1;

          final String nm = property.getName();

          switch (nm) {
            case Property.DTSTART:
              dtstart = property.getValue();
              continue;

            case Property.SUMMARY:
              summary = property.getValue();
              val = val + " " + summary;
              continue;

            case Property.CATEGORIES:
              val += " " + property.getValue();
              categories += property.getValue() + " ";
              continue;

            case Property.COMMENT:
            case Property.DESCRIPTION:
              val += " " + property.getValue();
              continue;

          }

          System.out.println("Dtstart: " + dtstart +
                                     " summary: " + summary);
          System.out.println(categories);

          System.out.println(cat.getString(val));
        }
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  public String getString(final String query) {
    try {
      final URIBuilder urib =
              CatUtil.fromServerUrl(catsUrl,
                                    null,
                                    prefix,
                                    new BasicNameValuePair("href", null),
                                    new BasicNameValuePair("q", query));

      try (final CloseableHttpResponse hresp =
                   HttpUtil.doGet(cl,
                                  urib.build(),
                                  null, // headers
                                  null)) {   // content type
        final int rc = HttpUtil.getStatus(hresp);

        if (rc == HttpServletResponse.SC_NOT_MODIFIED) {
          // Data unchanged.
          if (debug()) {
            debug("data unchanged");
          }
          return null;
        }

        if (rc != HttpServletResponse.SC_OK) {
          if (debug()) {
            debug("Unsuccessful response from server was " + rc);
          }

          return null;
        }

        final InputStream is = hresp.getEntity().getContent();

        if (is == null) {
          return null;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final int bufSize = 2048;
        final byte[] buf = new byte[bufSize];
        while (true) {
          final int len = is.read(buf, 0, bufSize);
          if (len == -1) {
            break;
          }

          baos.write(buf, 0, len);
        }

        return baos.toString(StandardCharsets.UTF_8);
      }
    } catch (final Throwable t) {
      error(t);
      return null;
    }
  }

  void print(final String fmt,
             final Object... params) {
    final Formatter f = new Formatter();

    info(f.format(fmt, params).toString());
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
