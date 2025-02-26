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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
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
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;

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
    // the time zone used for the whole application
	private ZoneId berlinTimeZone = ZoneId.of("Europe/Berlin");
	/**
	 * sets the current year and reads the accepted formats for date-inputs from dateTimeFormats.txt
	 */
	public TweetFactory(String dateFormatsPath) {
		currentYear = ZonedDateTime.now(berlinTimeZone).getYear();
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
	 * row, which has the following format: [date, should be meant in timezone of Berlin] tab [time(optional)]
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
	 * @return a list of tweetGroups with a tweet for each row in the file
	 */
	public List<TweetGroup> getTweetsFromTSVFile(File tsvFile, String title, String description, int delay, String encoding) throws MalformedTSVFileException {
		List<TweetGroup> toReturn = new ArrayList<>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tsvFile), encoding));
			String line = in.readLine();
			String content;
			String date;
			String time;
			int delayInSeconds = -1;
			ZonedDateTime lastZDT = null;
			boolean useDelay = false;
			ZonedDateTime zdt;
			Tweet tweet;
			int row = 1;
			// map with thread-name as key and tweetgroup as value to save all groups from imported table
			Map<String, TweetGroup> threadGroups = new HashMap<>();
			// default group = group name and description from user input
			TweetGroup group = new TweetGroup(title, description);
			threadGroups.put("default", group);
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
						zdt = parseDateString(date).atZone(berlinTimeZone);
						if(zdt == null){
							throw new MalformedTSVFileException(row, 1, date, "malformed date: "+origDate+"  (row: "+row+" column: 1)");
						}
					} else {
						zdt = parseDateString(date + " " + time).atZone(berlinTimeZone);
						if(zdt == null){
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
					
					
					zdt = lastZDT.plusSeconds(delayInSeconds);
				}

				// get tweet-image
				String imageUrl = null;
				if (split.length > 3) {
					imageUrl = split[3];
					if(imageUrl.length() > 0){
						try {
							if(ImageIO.read(new URL(imageUrl)) == null){
								if (!(imageUrl.endsWith(".jpg") || imageUrl.endsWith(".jpeg") || imageUrl.endsWith(".png") || imageUrl.endsWith(".gif") || imageUrl.endsWith(".webp"))) {
									throw new MalformedTSVFileException(row, 4, imageUrl, "invalid image-Url, wrong format, please use jpg, jpeg, gif, png or webp: "+imageUrl+" (row: "+row+" column: 5)");
								}
								throw new MalformedTSVFileException(row, 4, imageUrl, "invalid image-Url: "+imageUrl+" (row: "+row+" column: 5)");
							}
						} catch (Exception e) {
							throw new MalformedTSVFileException(row, 4, imageUrl, "invalid image-Url: "+imageUrl+" (row: "+row+" column: 5)");
						}
					}
				}
				// get latitude
				float latitude = 0;
				float longitude = 0;
				if (split.length > 4 && !(split[4].isEmpty())) {
					try{
						String number = split[4];
						number = number.replace(",", "\\.");
						latitude = Float.parseFloat(number);
					}
					catch(NumberFormatException e){
						throw new MalformedTSVFileException(row, 5, split[4], "malformed latitude: "+split[4]+"   (row: "+row+" column: 5)" );
					}
					// get longitude
					if (split.length > 5 && !(split[5].isEmpty())) {
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

				// new: create thread-groups
				// connected tweets to a thread are labeled in column "Threading"
				String thread = null;
				if(split.length > 6) {
					thread = split[6].trim();

					if (thread.length() > 0) {
						if (threadGroups.containsKey(thread)) {
							group = threadGroups.get(thread);
						} else {
							// create new group for each thread
							group = new TweetGroup(title + "_Thread_" + thread, description);
							group.setThreaded(true);
							threadGroups.put(thread, group);
						}
					}
				}
				else {
					group = threadGroups.get("default");
				}
				
				// get tweet-content
				content = split[2];
				//escape java
				content = StringEscapeUtils.unescapeJava(content);

				// add delay
				zdt = zdt.plusYears(delay);

				if (delay == 0) {
					while (zdt.isBefore(ZonedDateTime.now(berlinTimeZone))) {
						zdt = zdt.plusYears(1);
					}
				}
				// normalize date to the format yyyy-MM-dd HH:mm
				String formattedDate = zdt.format(formatter);
				// set default time to 12:00
				boolean midnight = false;
				if (time.contains(" 00:00")) {
					midnight = true;
				}
				if (!midnight) {
					formattedDate = formattedDate.replace(" 00:00", " 12:00");
				}
				if (zdt.isAfter(ZonedDateTime.now(berlinTimeZone))) {
					tweet = new Tweet(formattedDate, content, imageUrl, longitude, latitude);
					group.addTweet(tweet);
				}
				line = in.readLine();
				row++;
				lastZDT = zdt;
			}
			for (String thread : threadGroups.keySet()){
				toReturn.add(threadGroups.get(thread));
			}
			threadGroups = new HashMap<>();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return toReturn;
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
		currentYear = ZonedDateTime.now(berlinTimeZone).getYear();
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
		ZonedDateTime zdtOriginal = parseDateString(pastDate).atZone(berlinTimeZone);
		if (zdtOriginal == null)
			return null;
		// find next anniverary in the future
		ZonedDateTime zdt = LocalDateTime.of(currentYear, zdtOriginal.getMonth(), zdtOriginal.getDayOfMonth(),
				zdtOriginal.getHour(), zdtOriginal.getMinute()).atZone(berlinTimeZone);
		ZonedDateTime today = ZonedDateTime.now(berlinTimeZone);
		if (zdt.isBefore(today)) {
			zdt = zdt.plusYears(1);
		}
		// normalize date to the format YYYY-MM-dd HH:mm
		String formattedDate = zdt.format(formatter);
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
