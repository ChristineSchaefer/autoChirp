package autoChirp;

import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.AcceptHeaderLocaleTzCompositeResolver;
import autoChirp.tweeting.TweetScheduler;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import autoChirp.tweeting.TzRedirectInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Default (SpringBoot-)Application class with main() method and minor
 * extensions: On start a persistent connection to the SQLite database is opened
 * and all relevant Tweets from that database are scheduled.
 *
 * @author Philip Schildkamp
 * @author Alena Geduldig
 */
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	private static Class<Application> applicationClass = Application.class;

	@Value("${autochirp.database.dbfile}")
	private String dbfile;

	@Value("${autochirp.database.schema}")
	private String schema;

	/**
	 * @param args
	 *            Command line arguments
	 * @throws IOException
	 *             IOException
	 */
	public static void main(String[] args) throws IOException {
		ApplicationContext ctx = SpringApplication.run(Application.class, args);
	}

	/**
	 * @param application
	 *            SpringApplicationBuilder object
	 * @return SpringApplicationBuilder SpringApplicationBuilder object
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(applicationClass);
	}

	@Bean
	LocaleContextResolver localeResolver () {
		SessionLocaleResolver l = new SessionLocaleResolver();
		AcceptHeaderLocaleTzCompositeResolver r = new
				AcceptHeaderLocaleTzCompositeResolver(l);
		return r;
	}

	@Bean
	public WebMvcConfigurer configurer () {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addInterceptors (InterceptorRegistry registry) {
				TzRedirectInterceptor interceptor = new TzRedirectInterceptor();
				InterceptorRegistration i = registry.addInterceptor(interceptor);
				//TODO: replace tzHandler?
				i.excludePathPatterns("/tzHandler", "/tzValueHandler");
			}
		};
	}

	/**
	 * Open database connection and schedule relevant Tweets.
	 */
	@PostConstruct
	private void initializeApplication() {
		File file = new File(dbfile);

		if (!file.exists()) {
			DBConnector.connect(dbfile);
			DBConnector.createOutputTables(schema);
		} else {
			DBConnector.connect(dbfile);
		}

		Map<Integer, List<TweetGroup>> toSchedule = DBConnector.getAllEnabledGroups();
		for (int userID : toSchedule.keySet()) {
			for (TweetGroup group : toSchedule.get(userID)) {
				TweetScheduler.scheduleTweetsForUser(group.tweets, userID);
			}
		}
		System.out.println("Application initialized");
	}

}
