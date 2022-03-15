package autoChirp.timezoneExtraction;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.TimeZone;

/**
* A custom HandlerInterceptor implementation that will intercept each request and will try to obtain TimeZone instance.
* based on https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/client-time-zone.html
 *
* @author: Ricarda Boente
 */
public class TzRedirectInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle (HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {

        TimeZone tz = RequestContextUtils.getTimeZone(request);

        if (tz == null) {
            System.out.println("Forwarding to js to get timezone offset");
            request.setAttribute("requestedUrl", request.getRequestURI());
            RequestDispatcher dispatcher = request.getRequestDispatcher("/tzHandler");
            dispatcher.forward(request, response);
            return false;
        }

        return true;
    }
}