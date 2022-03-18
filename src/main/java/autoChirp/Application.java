package autoChirp;

import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.TweetScheduler;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import com.mysql.cj.jdbc.exceptions.SQLError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;

/**
 * Default (SpringBoot-)Application class with main() method and minor
 * extensions: On start a persistent connection to the SQLite database is opened
 * and all relevant Tweets from that database are scheduled.
 *
 * @author Philip Schildkamp
 * @author Alena Geduldig
 * @editor Laura Pascale Berg
 */
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	private static Class<Application> applicationClass = Application.class;

	@Value("${autochirp.database.dblink}")
	private String dblink;

	@Value("${autochirp.database.dbcreatelink}")
	private String dbcreatelink;

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

	/**
	 * Open database connection and schedule relevant Tweets.
	 * If there isn't a database create database and create tables with schema
	 */
	@PostConstruct
	private void initializeApplication() {
		try {
			DBConnector.connect(dblink);
		} catch (SQLException e) {
			DBConnector.createDatabase(dbcreatelink, dblink);
			DBConnector.createOutputTables(schema);
		}

		Map<Integer, List<TweetGroup>> toSchedule = DBConnector.getAllEnabledGroups();
		for (int userID : toSchedule.keySet()) {
			for (TweetGroup group : toSchedule.get(userID)) {
				TweetScheduler.scheduleTweetsForUser(group.tweets, userID);
			}
		}
	}

}
