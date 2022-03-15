package autoChirp.webController;

import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

/**
 * a class to manage handlers used for processing the client's time zone
 * based on: https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/client-time-zone.html
 *
 * @author Ricarda Boente
 */

@Controller
public class TzExampleController {

    /**
     * method for testing purposes only, not used at the moment
     * @return redirect to a page showing obtained time zone information
     */
    @RequestMapping("/tz")
    @ResponseBody
    public String testHandler (Locale clientLocale, ZoneId clientZoneId) {

        ZoneOffset serverZoneOffset = ZoneOffset.ofTotalSeconds(
                  TimeZone.getDefault().getRawOffset() / 1000);

        return String.format("client timeZone: %s" +
                                       "<br/> " +
                                       "server timeZone: %s" +
                                       "<br/>" +
                                       " locale: %s%n",
                             clientZoneId.normalized().getId(),
                             serverZoneOffset.getId(),
                             clientLocale);
    }

    /**
     * @return redirect to tzPage that will obtain information in hidden input field and call /tzValueHandler
     */
    @RequestMapping("/tzHandler")
    public String handle () {
        return "tzPage";
    }

    /**
     * processes time zone information from client
     * @return redirect to requested URL, /home in this case
     */
    @RequestMapping(value = "/tzValueHandler", method = RequestMethod.POST)
    public String handleTzValue (
              Locale locale, HttpServletRequest req,
              HttpServletResponse res,
              @RequestParam("requestedUrl") String requestedUrl,
              @RequestParam("timeZoneOffset") int timeZoneOffset) {

        ZoneOffset zoneOffset =
                  ZoneOffset.ofTotalSeconds(-timeZoneOffset * 60);

        TimeZone timeZone = TimeZone.getTimeZone(zoneOffset);

        LocaleContextResolver localeResolver =
                  (LocaleContextResolver) RequestContextUtils.getLocaleResolver(req);

        localeResolver.setLocaleContext(req, res,
                                        new SimpleTimeZoneAwareLocaleContext(
                                                  locale, timeZone));
        System.out.println("TimeZone: " + timeZone);

        return "redirect:" + requestedUrl;

    }
}