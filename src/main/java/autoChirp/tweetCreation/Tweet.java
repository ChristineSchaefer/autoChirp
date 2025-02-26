package autoChirp.tweetCreation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.format.datetime.DateTimeFormatAnnotationFormatterFactory;

import autoChirp.DBConnector;

/**
 * 
 * represents a single tweet consisting of content, tweetDate, imageUrl
 * (optional), and geo-locations (optional). If read from DB, also consisting of
 * tweetID, userID, groupID, groupName and the status-attributes scheduled and tweeted
 * 
 * @author Alena Geduldig
 * @editor Laura Pascale Berg
 */
public class Tweet implements Comparable<Tweet> {
	public int userID;
	public String tweetDate;
	public String content;
	public int tweetID;
	public int groupID;
	public boolean scheduled;
	public boolean tweeted;
	public String groupName;
	public String imageUrl;
	public float longitude;
	public float latitude;
	public long statusID;
	private String trimmedContent;
	private int adjustedLength = -1;
	public static final int MAX_TWEET_LENGTH = 280;

	/**
	 * Constructor for tweets read from the database. In contrast to new tweets,
	 * tweets read from DB already have a tweetID, groupID, userID and
	 * status-attributes
	 * 
	 * @param tweetDate
	 *            scheduling date of the tweet
	 * @param content
	 *            the tweets content
	 * @param tweetID
	 *            db-key
	 * @param groupID
	 *            db-group-assignment key
	 * @param scheduled
	 *            tweet is already scheduled and will be tweeted
	 * @param tweeted
	 *            tweet has already been tweeted
	 * @param userID
	 *            db-user-assignment key
	 * @param imageUrl
	 *            link to an image
	 * @param longitude
	 *            longitude of geo-location
	 * @param latitude
	 *            latitude of geo-location
	 */
	public Tweet(String tweetDate, String content, int tweetID, int groupID, boolean scheduled, boolean tweeted,
			int userID, String imageUrl, float longitude, float latitude, long statusID) {
		this.userID = userID;
		this.tweetDate = tweetDate;
		this.content = content;
		this.tweetID = tweetID;
		this.scheduled = scheduled;
		this.tweeted = tweeted;
		this.groupID = groupID;
		this.groupName = DBConnector.getGroupTitle(groupID, userID);
		this.imageUrl = imageUrl;
		this.longitude = longitude;
		this.latitude = latitude;
		this.statusID = statusID;
	}

	/**
	 * Constructor for new tweets, which were not stored in the database yet.
	 * (don't have a tweetID, groupID or status-attributes)
	 * 
	 * @param tweetDate
	 *            scheduling date of the tweet
	 * @param content
	 *            the tweets content
	 * @param imageUrl
	 *            link to an image
	 * @param longitude
	 *            longitude of geo-location
	 * @param latitude
	 *            latitude of geo-location
	 */
	public Tweet(String tweetDate, String content, String imageUrl, float longitude, float latitude) {
		this.tweetDate = tweetDate;
		this.content = content;
		this.imageUrl = imageUrl;
		this.longitude = longitude;
		this.latitude = latitude;
	}

	/**
	 * Constructor for simple Tweets without imageUrl and Geo-locations
	 * 
	 * @param tweetDate
	 *            tweetDate
	 * @param content
	 *            tweetContent
	 */
	public Tweet(String tweetDate, String content) {
		this.tweetDate = tweetDate;
		this.content = content;
	}

	/**
	 * ascending tweet order by tweetdate
	 * 
	 * @param tweet
	 *            tweet
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Tweet tweet) {
		return this.tweetDate.compareTo(tweet.tweetDate);
	}
	
	private String trimContent(String toTrim, String url) {
		int maxLength = MAX_TWEET_LENGTH;
		if(url!= null){
			maxLength = maxLength - 25;
		}
		StringBuffer sb = new StringBuffer();
		char[] chars = toTrim.toCharArray();
		int counter = 0;
		for(int i = 0; i < chars.length; i++){
			counter++;
			if(Character.codePointAt(chars, i)>= 4352){
				counter++;
			}
			sb.append(chars[i]);
			if(counter == maxLength){
				break;
			}
			if(counter > maxLength){
				sb.deleteCharAt(sb.length()-1);
				break;
			}
		}
		if (url != null) {
			sb.append(" "+url);
		}
		this.trimmedContent = sb.toString();
		return sb.toString();
	}
	
	public String trimmedContent(){
		if(trimmedContent != null) return trimmedContent;
		String trimmed = this.content;
		String urlRegex = "(http(s)?:\\/\\/(.*))(\\s)?" ;
		Pattern pattern = Pattern.compile(urlRegex);
		Matcher matcher = pattern.matcher(trimmed);
		String url = null;
		if(matcher.find()){
			url = matcher.group(1);
			trimmed = trimmed.replace(url, "");
		}	
		return trimContent(trimmed, url);
	}
	

	/**
	 * Get adjusted Tweet length (Twitter replaces URLs with t.co shortURLs,
	 * resulting in the necessity to adjust the content string lengths).
	 *
	 * @return The adjustet Tweet content length
	 */
	public int adjustedLength() {
		if(adjustedLength != -1){
			return adjustedLength;
		}
		String placeholder = String.format("%24s", "");
		String adjusted = this.content.replaceAll("https?://[^\\s]*", placeholder);
		char[] chars = adjusted.toCharArray();
		int counter = 0;
		for(int i = 0; i < chars.length; i++){
			counter++;
			if(Character.codePointAt(chars, i) >= 4352){
				counter++;
			}
		}
		return counter;
	}
	
	public String formatDate(){
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.parse(tweetDate,dtf);
		dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
		return dtf.format(date);
		
	
	}
}
