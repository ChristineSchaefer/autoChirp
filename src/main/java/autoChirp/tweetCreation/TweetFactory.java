package autoChirp.tweetCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.tomcat.jni.Local;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import autoChirp.preProcessing.Document;
import autoChirp.preProcessing.HeidelTimeWrapper;
import autoChirp.preProcessing.SentenceSplitter;
import autoChirp.preProcessing.parser.Parser;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;

/**
 * A class to generate tweets and tweetGroups from different input-types (tsv-files or urls)
 *
 * @author Alena Geduldig
 *
 */

public class TweetFactory {
	//a file with regexes of all accepted date-types and their date-formats
	private File dateFormatsFile = new File("src/main/resources/dateTimeFormats/dateTimeFormats.txt");
	// the current year is needed to calculate the next possible tweet-date.
	private int currentYear;
	// regexes for the accepted date and time formats
	private List<String> dateTimeRegexes = new ArrayList<String>();
	// regexes for the accepted date formats
	private List<String> dateRegexes = new ArrayList<String>();
	// accepted date and time formats
	private List<String> dateFormats = new ArrayList<String>();
	// a formatter to normalize the different input formats
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // the time zone used by the server
	private ZoneId serverZoneId = ZoneOffset.ofTotalSeconds(
			TimeZone.getDefault().getRawOffset() / 1000);
	/**
	 * sets the current year in UTC time and reads the accepted formats for date-inputs from dateTimeFormats.txt
	 */
	public TweetFactory(String dateFormatsPath) {
		currentYear = ZonedDateTime.now(serverZoneId).withZoneSameInstant(ZoneId.of("UTC")).getYear();
		this.dateFormatsFile = new File(dateFormatsPath);
		readDateFormatsFromFile();
	}

	/**
	 * read date-formats from file
	 */
	private void readDateFormatsFromFile() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(dateFormatsFile));
			String line = in.readLine();
			while(line!= null){
				String[] split = line.split("\t");
				if(line.startsWith("DateTime:")){
					addDateTimeForm(split[1], split[2]);
				}
				if(line.startsWith("Date:")){
					addDateForm(split[1], split[2]);
				}
				line = in.readLine();
			}
			in.close();
		} catch (IOException e) {
			System.out.println("couldnt read dateFormats from File");
			e.printStackTrace();
		}
	}

	/**
	 *
	 * adds a new dateTime format
	 *
	 * @param regex
	 *            - dateTime-regex
	 * @param format
	 *            - dateTime-format
	 */
	private void addDateTimeForm(String regex, String format) {
		dateTimeRegexes.add(regex);
		dateFormats.add(format);
	}

	/**
	 * adds a new date format
	 *
	 * @param regex
	 *            - date-regex
	 * @param format
	 *            - date-format
	 */
	private void addDateForm(String regex, String format) {
		dateRegexes.add(regex);
		dateFormats.add(format);
	}

	/**
	 * creates a TweetGroup-object from a tsv-file by building a tweet for each
	 * row, which has the following format: [date, meant in client's time zone] tab [time(optional)]
	 * tab [tweet-content] tab [imageUrl (optional)] tab [latitude (optional)]
	 * tab [longitude (optional)]
	 *
	 * @param tsvFile
	 *            the input file
	 * @param title
	 *            - a title for the created tweetGroup
	 * @param description
	 *            - a description for the created tweetGroup
	 * @param delay
	 *            - the number of years between the written date in the file and
	 *            the calculated tweet-date
	 * @return a new tweetGroup with a tweet for each row in the file
	 */
	public TweetGroup getTweetsFromTSVFile(File tsvFile, String title, String description, int delay, String encoding, ZoneId clientZoneId) throws MalformedTSVFileException {
		TweetGroup group = new TweetGroup(title, description);
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tsvFile), encoding));
			ZoneId utcZoneId = ZoneId.of("UTC");
			String line = in.readLine();
			String content;
			String date;
			String time;
			int delayInSeconds = -1;
			LocalDateTime lastLDT = null;
			boolean useDelay = false;
			LocalDateTime ldt;
			ZonedDateTime zdt;
			Tweet tweet;
			int row = 1;
			while (line != null) {
				if(line.equals("")){
					line = in.readLine();
					row++;
					continue;
				}
				if(line.toLowerCase().startsWith("date")||line.toLowerCase().startsWith("datum")){
					line = in.readLine();
					row++;
					continue;
				}
				String[] split = line.split("\t");
				// get tweet-date
				date = split[0].trim();
				String origDate = date;
				if(date.toLowerCase().contains("delay")){
					useDelay = true;
				}
				else if(date.length() <= 7){		
					//add missing day of month
					date = date.concat("-01");
				}
				time = split[1].trim();
				if(!useDelay){
					if (time.equals("")) {
						ldt = parseDateString(date);
						if(ldt == null){
							throw new MalformedTSVFileException(row, 1, date, "malformed date: "+origDate+"  (row: "+row+" column: 1)");
						}
					} else {
						ldt = parseDateString(date + " " + time);
						if(ldt == null){
							throw new MalformedTSVFileException(row, 1, date + " " + time, "malformed date or time: "+date + " " + time+"  (row: "+row+" column: 1-2)");
						}
					}
				}
				else{
				
						try{
							delayInSeconds = Integer.parseInt(time);
						}
						catch(NumberFormatException e){
							if(delayInSeconds == -1){
								throw new MalformedTSVFileException(row, 2, time, "malformed/missing delay: "+time+"  (row "+ row+ "column: 2)");
							}
						}
					

					ldt = lastLDT.plusSeconds(delayInSeconds);
				}

				// get tweet-image
				String imageUrl = null;
				if (split.length > 3) {
					imageUrl = split[3];
					if(imageUrl.length() > 0){
						try {
							Resource image = new UrlResource(imageUrl);
						} catch (Exception e) {
							throw new MalformedTSVFileException(row, 4, imageUrl, "invalid image-Url: "+imageUrl+" (row: "+row+" column: 4)");
						}
					}
				}
				// get latitude
				float latitude = 0;
				float longitude = 0;
				if (split.length > 4) {
					try{
						String number = split[4];
						number = number.replace(",", ".");
						latitude = Float.parseFloat(number);
					}
					catch(NumberFormatException e){
						throw new MalformedTSVFileException(row, 5, split[4], "malformed latitude: "+split[4]+"   (row: "+row+" column: 5)" );
					}
					// get longitude
					if (split.length > 5) {
						try{
							String number = split[5];
							number = number.replace(",", "\\.");
							longitude = Float.parseFloat(split[5]);
						}
						catch(NumberFormatException e){
							throw new MalformedTSVFileException(row, 6, split[5], "malformed longitude: "+split[5]+"   (row: "+row+" column: 6)");
						}
					}
					else{
						//lattitude without longitude will be ignored
						latitude = 0;
					}
				}
				
				// get tweet-content
				content = split[2];
				//escape java
				content = StringEscapeUtils.unescapeJava(content);

				// add delay
				ldt = ldt.plusYears(delay);

				//refactor ldt to zdt (UTC)
				zdt = ldt.atZone(clientZoneId).withZoneSameInstant(utcZoneId);

				// add year according to delay if given date is before now (calculated in UTC)
				if (delay == 0) {
					while (zdt.isBefore(ZonedDateTime.now(serverZoneId).withZoneSameInstant(utcZoneId))) {
						ldt = ldt.plusYears(1);
					}
				}
				// normalize date to the format yyyy-MM-dd HH:mm
				String formattedDate = ldt.format(formatter);
				// set default time to 12:00
				boolean midnight = false;
				if (time.contains(" 00:00")) {
					midnight = true;
				}
				if (!midnight) {
					formattedDate = formattedDate.replace(" 00:00", " 12:00");
				}
				//if ldt has changed, correct zdt
				zdt = ldt.atZone(clientZoneId).withZoneSameInstant(utcZoneId);

				if (zdt.isAfter(ZonedDateTime.now(serverZoneId).withZoneSameInstant(utcZoneId))) {
					tweet = new Tweet(formattedDate, content, imageUrl, longitude, latitude);
					group.addTweet(tweet);
				}
				line = in.readLine();
				row++;
				lastLDT = ldt;
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return group;
	}

	/**
	 * Creates a TweetGroup-object from the given url. 1. Creates a
	 * Document-object with the given Parser 2. Splits the documents text into
	 * sentences using the SentenceSplitter and concatenates it again with a
	 * sentence-delimiter 3. Tags dates in the concatenated text using
	 * HeidelTime 4. Splits tagged sentences at the delimiter and extract
	 * date-strings for each sentence 5. Calculates the tweetDate (= next
	 * anniversary) for each tagged date 6. Created a new Tweet-object for each
	 * date and its containing sentence and adds it to the TweetGroup
	 *
	 * @param url
	 *            url
	 * @param parser
	 *            the appropriate parser for the given url (e.g. WikipediaParser
	 *            for Wikipedia-urls)
	 * @param description
	 *            a description for the created TweetGroup
	 * @param prefix
	 *            a prefix for each tweet in the created tweetGroup. Each
	 *            tweet-content will start with "[prefix]: " *
	 * @return a new TweetGroup
	 */
	public TweetGroup getTweetsFromUrl(String url, Parser parser, String description, String prefix) {
		// create document
		Document doc = parser.parse(url);
		SentenceSplitter splitter = new SentenceSplitter(doc.getLanguage());
		doc.setSentences(splitter.splitIntoSentences(doc.getText()));
		// tag dates
		String[] processedSentences = tagDatesWithHeideltime(doc);
		List<Tweet> tweets = new ArrayList<Tweet>();
		for (int i = 0; i < processedSentences.length; i++) {
			String sentence = processedSentences[i];
			// extract dates from sentence
			List<String> origDates = extractDates(sentence);
			Tweet tweet;
			String content;
			for (String date : origDates) {
				// calc. next possible tweet-date
				String tweetDate = getTweetDate(date);
				if (tweetDate == null)
					continue;
				// trim sentence to 140 character
				if (prefix != null ) {
					if(prefix.equals("")){
						 //content = trimToTweet(doc.getSentences().get(i - 1), url, null);
						content = doc.getSentences().get(i-1)+" "+url;
					}
					else{
						//content = trimToTweet(prefix + ": " + doc.getSentences().get(i - 1), url, null);
						content = prefix+": "+doc.getSentences().get(i-1)+" "+url;
					}

				} else {
					//content = trimToTweet(doc.getSentences().get(i - 1), url, null);
					content = doc.getSentences().get(i-1)+" "+url;
				}
//				tweet = new Tweet(tweetDate, content, null, 0, 0);
				tweet = new Tweet(tweetDate, content, null, 0, 0);
				tweets.add(tweet);
			}
		}
		currentYear = ZonedDateTime.now(serverZoneId).withZoneSameInstant(ZoneId.of("UTC")).getYear();
		TweetGroup group = new TweetGroup(doc.getTitle(), description);
		group.setTweets(tweets);
		return group;
	}

	/**
	 * returns a list of TimeML-annotated sentences
	 *
	 * @param document
	 * @return list of tagged sentences
	 */
	private String[] tagDatesWithHeideltime(Document document) {
		HeidelTimeWrapper ht = new HeidelTimeWrapper(document.getLanguage(), DocumentType.NARRATIVES, OutputType.TIMEML,
				"/heideltime/config.props", POSTagger.TREETAGGER, false);
		String toProcess = concatenateSentences(document.getSentences());
		String processed;
		try {
			processed = ht.process(toProcess);
		} catch (DocumentCreationTimeMissingException e) {
			e.printStackTrace();
			return null;
		}
		return processed.split("#SENTENCE#");
	}

	/**
	 * concatenates a list of sentences with the delimiter '#SENTENCE#'
	 *
	 * @param sentences
	 * @return concatenated sentences
	 */
	private String concatenateSentences(List<String> sentences) {
		StringBuffer sb = new StringBuffer();
		for (String s : sentences) {
			sb.append("#SENTENCE#" + s);
		}
		return sb.toString();
	}

	
	
//	/**
//	 * trims a sentence to a tweet-lenth of max. 140 characters and adds the
//	 * given url to the tweets content
//	 *
//	 * @param toTrim
//	 * @param url
//	 * @return a valid tweet content
//	 */
//	public String trimToTweet(String toTrim, String url) {
//		int maxLength = MAX_TWEET_LENGTH;
//		if(url!= null){
//			maxLength = maxLength - 25;
//		}
//		StringBuffer sb = new StringBuffer();
//		char[] chars = toTrim.toCharArray();
//		int counter = 0;
//		for(int i = 0; i < chars.length; i++){
//			counter++;
//			if(Character.codePointAt(chars, i)>= 4352){
//				counter++;
//			}
//			sb.append(chars[i]);
//			if(counter == maxLength){
//				break;
//			}
//			if(counter > maxLength){
//				sb.deleteCharAt(sb.length()-1);
//				break;
//			}
//		}
//		if (url != null) {
//			sb.append(" "+url);
//		}
//		return sb.toString();
//	}
//	
//	public String trimToTweet(String toTrim){
//		String urlRegex = "(http(s)?:\\/\\/(.*))(\\s)?" ;
//		Pattern pattern = Pattern.compile(urlRegex);
//		Matcher matcher = pattern.matcher(toTrim);
//		String url = null;
//		if(matcher.find()){
//			url = matcher.group(1);
//			toTrim = toTrim.replace(url, "");
//		}	
//		return trimToTweet(toTrim, url);
//	}

	//TODO: next three date helper methods could optionally be refactored to use ZonedDateTime from scratch
	/**
	 * extract date-strings from a TimeML annotated sentence. Extracts only dates
	 * with at least a year and month.
	 *
	 * @param sentence
	 * @return a list of date-expressions
	 */
	private List<String> extractDates(String sentence) {
		List<String> dates = new ArrayList<String>();
		String dateRegex = "type=\"DATE\" value=\"([0-9|XXXX]{4}-[0-9]{2}(-[0-9]{2})?)\">";
		String timeRegex = "type=\"TIME\" value=\"(([0-9]{4}|XXXX)-[0-9]{2}(-[0-9]{2})?)(( [A-Z]{2,4})|(T[0-9]{2}:[0-9]{2}(:[0-9]{2})?))\">";
		Pattern pattern = Pattern.compile(dateRegex);
		Matcher matcher = pattern.matcher(sentence);
		Pattern pattern2 = Pattern.compile(timeRegex);
		Matcher matcher2 = pattern2.matcher(sentence);
		String date = null;
		String time = null;
		while (matcher.find()) {
			date = matcher.group(1);
			// select only date with at least a month
			if (date.contains("-")) {
				dates.add(date);
			}
			while (matcher2.find()) {
				time = matcher2.group(1) + matcher2.group(6).replace("T", " ");
				dates.add(time);
			}
		}
		return dates;
	}

	/**
	 * calculates the next possible tweet-date (= next anniversary in the
	 * future) from the given date-expression (e.g. 1955-08-01  returns
	 * 2017-08-01 )
	 *
	 * @param pastDate past date
	 * @return next anniversary in the format YYYY-MM-dd HH:mm
	 */
	private String getTweetDate(String pastDate) {
		// set default time to 12:00
		boolean midnight = false;
		if (pastDate.contains(" 00:00")) {
			midnight = true;
		}
		// handle dates with unspecified year
		if (pastDate.startsWith("XXXX")) {
			pastDate = pastDate.replace("XXXX", currentYear + "");
		}
		// set default day in month to 01
		if (pastDate.length() == 7) {
			pastDate = pastDate.concat("-01");
		}
		LocalDateTime ldtOriginal = parseDateString(pastDate);
		if (ldtOriginal == null)
			return null;
		// find next anniverary in the future
		LocalDateTime ldt = LocalDateTime.of(currentYear, ldtOriginal.getMonth(), ldtOriginal.getDayOfMonth(),
				ldtOriginal.getHour(), ldtOriginal.getMinute());
		LocalDateTime today = LocalDateTime.now();
		if (ldt.isBefore(today)) {
			ldt = ldt.plusYears(1);
		}
		// normalize date to the format YYYY-MM-dd HH:mm
		String formattedDate = ldt.format(formatter);
		if (!midnight) {
			formattedDate = formattedDate.replace(" 00:00", " 12:00");
		}
		return formattedDate;
	}

	/**
	 * parses a date-string and returns a LocalDateTime-object of the format
	 * YYYY-MM-dd HH:mm
	 *
	 * @param date
	 * @return a LocalDateTime-object of the given date-string or null if the
	 *         string does not satisfy one of the accepted date-formats
	 */
	private LocalDateTime parseDateString(String date) {
		LocalDateTime ldt;
		LocalDate ld;
		Pattern pattern;
		Matcher matcher;
		for (int i = 0; i < dateTimeRegexes.size(); i++) {
			String regex = dateTimeRegexes.get(i);
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(date);
			if (matcher.find()) {
				DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(i));
				ldt = LocalDateTime.parse(date, dtFormatter);
				return ldt;
			}
		}
		int dateTimes = dateTimeRegexes.size();
		for (int j = 0; j < dateRegexes.size(); j++) {
			String regex = dateRegexes.get(j);
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(date);
			if (matcher.find()) {
				DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(j + dateTimes));
				ld = LocalDate.parse(date, dtFormatter);
				ldt = LocalDateTime.of(ld, LocalTime.of(12, 0));
				return ldt;
			}
		}
		return null;
	}
}
