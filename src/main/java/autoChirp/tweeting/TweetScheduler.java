package autoChirp.tweeting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

/**
 *
 * A class to schedule tweets using the
 * java.util.concurrent.ScheduledExecutorService;

 * @author Alena Geduldig
 *
 */
public class TweetScheduler {

  private static final Map<Integer, Future<?>> scheduled = new HashMap<Integer, Future<?>>();
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

	/**
	 * Schedules a list of tweets for the given twitter-user according to Berlin timezone by creating a new
	 * TwitterTask for each tweet. Also updates the tweets status in the
	 * database to scheduled = true
	 *
	 * @param tweets
	 *            a list of tweets to schedule
	 * @param user_id
	 *            id of the associated user
	 */
	public static void scheduleTweetsForUser(List<Tweet> tweets, int user_id) {
		ZoneId zoneId = ZoneId.of("Europe/Berlin");
		long delay;

		for (Tweet tweet: tweets) {
		
      // ignore if tweet is already scheduled
      if (scheduled.containsKey(tweet.tweetID)) {
        continue;
      }

			// create DateTime-Object from date-string
			LocalDateTime ldt = LocalDateTime.parse(tweet.tweetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

			// calculate delay in seconds, including time changes due to DST
			ZonedDateTime znow = ZonedDateTime.now(zoneId);
			ZonedDateTime zdt = ldt.atZone(zoneId);
			delay = ChronoUnit.SECONDS.between(znow, zdt);


      //tweet-time is in the past
			if (delay < 0) {
				continue;
			}

			// schedule
      scheduled.put(tweet.tweetID, scheduler.schedule(new TwitterTask(user_id, tweet.tweetID), delay, TimeUnit.SECONDS));

			// update tweet-status
			DBConnector.flagAsScheduled(tweet.tweetID, user_id);
		}
	}

  /**
	 * Deschedules a Tweet by ID
	 *
	 * @param tweetID
	 *            the ID of the Tweet to deschedule
	 */
  public static void descheduleTweet(int tweetID) {

    if (!scheduled.containsKey(tweetID)) {
      System.out.println("Scheduler: could not descheduled Tweet #" + tweetID + ": No scheduling found");
      return;
    }

    Future<?> tweet = scheduled.remove(tweetID);
    tweet.cancel(true);
  }

}
