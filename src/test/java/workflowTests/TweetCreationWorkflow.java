package workflowTests;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.DBConnector;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.MalformedTSVFileException;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.TweetGroup;
import org.springframework.beans.factory.annotation.Value;


/**
 * @author Alena Geduldig
 * @editor Laura Pascale Berg
 * 
 * JUnit test-class for the Data-Minint- and TweetCreationWorkflow
 *
 */
public class TweetCreationWorkflow {

	@Value("${autochirp.database.dbtestlink}")
	private static String dblink;

	@Value("${autochirp.database.dbcreatelink}")
	private static String dbcreatelink;

	@Value("${autochirp.database.schema}")
	private static String schema;


	/**
	 * connect to database
	 * if there isn't a database create database and create tables with schema
	 */
	@BeforeClass
	public static void dbConnection() {
		try {
			DBConnector.connect(dblink);
		} catch (SQLException e) {
			DBConnector.createDatabase(dbcreatelink, dblink);
			DBConnector.createOutputTables(schema);
		}
	}


	
	/**
	 * test for the tweet creation from wiki-urls
	 */
	@Test
	public void generateTweetsFromWikipediaUrl(){
		TweetFactory factory = new TweetFactory("src/main/resources/parser/datetime.formats");
		List<String> urls = new ArrayList<String>();
		String url = "https://de.wikipedia.org/wiki/Universität_zu_Köln";
		urls.add(url);
		url = "https://en.wikipedia.org/wiki/Harvard_University";
		urls.add(url);
		for (String currentUrl : urls) {
			TweetGroup group = factory.getTweetsFromUrl(currentUrl, new WikipediaParser(),"testDescription", "prefix");
			System.out.println("Title: " + group.title);
			System.out.println("Description: " + group.description);
			System.out.println("numberOfTweets: " + group.tweets.size());
			System.out.println("Tweets: ");
			for (Tweet tweet : group.tweets) {
				Assert.assertTrue(tweet.content.startsWith("prefix: "));
				System.out.println(tweet.tweetDate+": "+ tweet.content);
			}
		}
	}
	
	/**
	 * test for tsv-imports
	 * @throws MalformedTSVFileException 
	 */
	@Test
	public void generateTweetsFromTSVFile() throws MalformedTSVFileException{
		TweetFactory factory = new TweetFactory("src/main/resources/parser/datetime.formats");
		File testFile = new File("src/test/resources/testTSVFile_Image_Locations.txt");
		
		//without delay
		List<TweetGroup> group = factory.getTweetsFromTSVFile(testFile, "testTitle", "testDescription", 0, "UTF-8");
		for(TweetGroup tw : group) {
			Assert.assertEquals(tw.tweets.size(), 3);
			System.out.println("Title: " + tw.title);
			System.out.println("Description: " + tw.description);
			System.out.println("numberOfTweets: " + tw.tweets.size());
			System.out.println("Tweets: ");
			for (Tweet tweet : tw.tweets) {
				Assert.assertTrue(tweet.tweetDate.startsWith("2016"));
				System.out.println(tweet.tweetDate + ": " + tweet.content);
			}
		}
		//with delay
		group = factory.getTweetsFromTSVFile(testFile, "testTitle", "testDescription", 3, "UTF-8");
		for(TweetGroup tw : group) {
			Assert.assertEquals(tw.tweets.size(), 3);
			System.out.println("Title: " + tw.title);
			System.out.println("Description: " + tw.description);
			System.out.println("numberOfTweets: " + tw.tweets.size());
			System.out.println("Tweets: ");
			for (Tweet tweet : tw.tweets) {
				Assert.assertTrue(tweet.tweetDate.startsWith("2019"));
				System.out.println(tweet.tweetDate + ": " + tweet.content + " " + tweet.imageUrl + " " + tweet.longitude + " " + tweet.latitude);
			}
		}
	}
	
	/**
	 * test all supported dateTime- and dateFormats specified in src/main/resources/dateTmeFormats
	 * @throws IOException 
	 * @throws MalformedTSVFileException 
	 */
	@Test
	public void dateTimeFomatsTest() throws IOException, MalformedTSVFileException{
		File file = new File("src/test/resources/testTSVFile_DateFormats.txt");
		TweetFactory factory = new TweetFactory("src/main/resources/parser/datetime.formats");
		List<TweetGroup> group = factory.getTweetsFromTSVFile(file, "dateFormatTest", "test all supported formats", 3, "UTF-8");
		for(TweetGroup tw : group) {
			Assert.assertEquals(tw.tweets.size(), 18);
			for (Tweet tweet : tw.tweets) {
				System.out.println(tweet.tweetDate);
			}
		}
	}
	
	@Test
	public void repeatGroupForYearsTest() throws MalformedTSVFileException{
		File file = new File("src/test/resources/testTSVFile_DateFormats.txt");
		TweetFactory factory = new TweetFactory("src/main/resources/parser/datetime.formats");
		List<TweetGroup> group = factory.getTweetsFromTSVFile(file, "dateFormatTest", "test all supported formats", 3, "UTF-8");
		for(TweetGroup tw : group) {
			TweetGroup oneYearLater = DBConnector.createRepeatGroupInYears(tw, 2, 2);
			System.out.println(oneYearLater.title);
			Assert.assertEquals(tw.description, oneYearLater.description);
			Assert.assertEquals(tw.tweets.size(), oneYearLater.tweets.size());
			for (int t = 0; t < tw.tweets.size(); t++) {
				Tweet t1 = tw.tweets.get(t);
				Tweet t2 = oneYearLater.tweets.get(t);
				System.out.println("old: " + t1.tweetDate + "     new: " + t2.tweetDate);
				Assert.assertEquals(t1.content, t2.content);
				Assert.assertEquals(t1.groupName, t2.groupName);
				Assert.assertTrue(t1.latitude == t2.latitude);
				Assert.assertTrue(t1.longitude == t2.longitude);
				Assert.assertEquals(t1.imageUrl, t2.imageUrl);
				LocalDateTime ldt1 = LocalDateTime.parse(t1.tweetDate,
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				LocalDateTime ldt2 = LocalDateTime.parse(t2.tweetDate,
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				Assert.assertEquals(ldt1.getMonth(), ldt2.getMonth());
				Assert.assertEquals(ldt1.getDayOfMonth(), ldt2.getDayOfMonth());
				Assert.assertEquals(ldt1.getMinute(), ldt2.getMinute());
				Assert.assertEquals(ldt1.getSecond(), ldt2.getSecond());
				Assert.assertTrue(ldt2.getYear() - ldt1.getYear() == 2);
			}
		}
	}
	
	@Test
	public void repeatGroupForSecondsTest() throws MalformedTSVFileException{
		File file = new File("src/test/resources/testTSVFile_DateFormats.txt");
		TweetFactory factory = new TweetFactory("src/main/resources/parser/datetime.formats");
		List<TweetGroup> group = factory.getTweetsFromTSVFile(file, "dateFormatTest", "test all supported formats", 3, "UTF-8");
		for(TweetGroup tw : group) {
			TweetGroup oneYearLater = DBConnector.createRepeatGroupInSeconds(tw, 2, 2000, "newTitle");
			System.out.println(oneYearLater.title);
			Assert.assertEquals(tw.description, oneYearLater.description);
			Assert.assertEquals(tw.tweets.size(), oneYearLater.tweets.size());
			for (int t = 0; t < tw.tweets.size(); t++) {
				Tweet t1 = tw.tweets.get(t);
				Tweet t2 = oneYearLater.tweets.get(t);
				System.out.println("old: " + t1.tweetDate + "     new: " + t2.tweetDate);
				Assert.assertEquals(t1.content, t2.content);
				Assert.assertEquals(t1.groupName, t2.groupName);
				Assert.assertTrue(t1.latitude == t2.latitude);
				Assert.assertTrue(t1.longitude == t2.longitude);
				Assert.assertEquals(t1.imageUrl, t2.imageUrl);
				LocalDateTime ldt1 = LocalDateTime.parse(t1.tweetDate,
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				LocalDateTime ldt2 = LocalDateTime.parse(t2.tweetDate,
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				Duration d = Duration.between(ldt1, ldt2);
				Assert.assertTrue(d.getSeconds() == 2000);
			}
		}
	}


}