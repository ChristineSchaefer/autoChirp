package autoChirp.timezoneExtraction;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * A custom LocaleContextResolver implementation going to delegate Locale responsibilities to AcceptHeaderLocaleResolver
 * and time zone responsibility to one of the Spring's implementation of LocaleContextResolver:,
 * based on https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/client-time-zone.html
 *
 * @author Ricarda Boente
 *
 */

public class AcceptHeaderLocaleTzCompositeResolver implements LocaleContextResolver {
    private LocaleContextResolver localeContextResolver;
    private AcceptHeaderLocaleResolver acceptHeaderLocaleResolver;

    public AcceptHeaderLocaleTzCompositeResolver (LocaleContextResolver localeContextResolver) {
        this.localeContextResolver = localeContextResolver;
        acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
    }

    @Override
    public LocaleContext resolveLocaleContext (HttpServletRequest request) {
        return localeContextResolver.resolveLocaleContext(request);
    }

    @Override
    public void setLocaleContext (HttpServletRequest request, HttpServletResponse response,
                                  LocaleContext localeContext) {
        localeContextResolver.setLocaleContext(request, response, localeContext);

    }

    @Override
    public Locale resolveLocale (HttpServletRequest request) {
        return acceptHeaderLocaleResolver.resolveLocale(request);
    }

    @Override
    public void setLocale (HttpServletRequest request, HttpServletResponse response, Locale locale) {
        acceptHeaderLocaleResolver.setLocale(request, response, locale);

    }
}