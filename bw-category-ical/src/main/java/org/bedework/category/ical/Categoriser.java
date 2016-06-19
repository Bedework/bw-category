/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.ical;

import org.bedework.util.args.Args;
import org.bedework.util.http.BasicHttpClient;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Formatter;

/**
 * User: mike Date: 5/4/16 Time: 11:57
 */
public class Categoriser {
  static void usage(final String error_msg) {
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

  static String catsUrl = "http://localhost:8080/bwcat/";
  static String prefix;
  static String ics;

  static BasicHttpClient cl;

  static boolean processArgs(final Args args) throws Throwable {
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
    try {
      if (!processArgs(new Args(args))) {
        return;
      }
      cl = new BasicHttpClient(30);
      cl.setBaseURI(new URI(catsUrl));
      
      CalendarBuilder builder = new CalendarBuilder();

      final UnfoldingReader ufrdr =
              new UnfoldingReader(new FileReader(ics),
                                  true);

      Calendar calendar = builder.build(ufrdr);

      for (final Object o : calendar.getComponents()) {
        Component component = (Component)o;

        if (component.getName().equals(Component.VTIMEZONE)) {
          continue;
        }
        
        String dtstart = null;
        String summary = null;
        String categories = "     ";
        String val = "";
        
        for (final Object o1 : component.getProperties()) {
          Property property = (Property)o1;
          
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

          final StringBuilder sb = 
                  new StringBuilder("categories/?href&q=");
          sb.append(URLEncoder.encode(val, "UTF-8"));

          if (prefix != null) {
            sb.append("&pfx=");
            sb.append(URLEncoder.encode(prefix, "UTF-8"));
          }
          
          System.out.println("Dtstart: " + dtstart +
                  " summary: " + summary);
          System.out.println(categories);
          
          System.out.println(getString(sb.toString()));
        }
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  public static String getString(final String request) throws Throwable {
    try {
      final InputStream is = cl.get(request, 
                                    null,  // contentType 
                                    null);

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

      return baos.toString("UTF-8");
    } finally {
      try {
        cl.release();
      } catch (final Throwable ignored) {}
    }
  }


  static void print(final String fmt,
                    final Object... params) {
    final Formatter f = new Formatter();

    info(f.format(fmt, params).toString());
  }

  static void info(final String msg) {
    Logger.getLogger(Categoriser.class).info(msg);
  }

  @SuppressWarnings("unused")
  static void warn(final String msg) {
    Logger.getLogger(Categoriser.class).warn(msg);
  }
}
