### 1. About
 #### The initial idea
-The first draft of the idea, which turned out as this project, was conceived in the seminar "Digital Humanities und die Informatik der Geisteswissenschaften" at the Universität zu Köln during the fall semester 2016. First thoughts went towards creating a "social media bot", a semi-interactive application that would parse arbitrary sources and create Tweets or Facebook status-updates from the parsed data. While developing this idea within the seminar, Dr. Øyvind Eide and Dr. Jürgen Hermes came forward with a more concrete project, which was derived from the needs of other, real-life users.
+The first draft of the idea, which turned out as this project, was conceived in the seminar "Digital Humanities und die Informatik der Geisteswissenschaften" at the Universität zu Köln during the fall semester 2016. First thoughts went towards creating a "social media bot", a semi-interactive application that would parse arbitrary sources and create Tweets or Facebook status-updates from the parsed data. While developing this idea within the seminar, Øyvind Eide and Jürgen Hermes came forward with a more concrete project, which was derived from the needs of other, real-life users.
 
 It didn't take long until a first prototype of the to be implemented application was ready to be reviewd; no need to say, our critics and us were confident and went along with the project. The basic idea was to create a web-application, that enables the users to input a Tweet, like they would on Twitter, but schedule it instead of publishing it right away. Utilized to keep track of historic developments by actually publishing event details (as Tweets) on their respective dates, autoChirp is imagened to enrichen the academic dialog.
 
 #### Development and implementation
 The autoChirp-development was done independently by us two developers and weekly meetings were basis of collaborative bug-fixing and coordination of further tasks. While we both had no prior experiance with neither the Spring MVC framework nor most of the other employed technologies, we managed to adapt well and help each other out. Problems arose within nearly all the different tasks, while some were merely one web-inquest away, others made it necessary to refactor large parts of code. Lastly all those hurdles were conquered and the project is (at least by us developers) regarded as a success story.
 
-Our academic patrons did alot of testing and reviewing and the outcome of those test-runs and use-cases fed back into the development process. During the final phase of the implementation, a public demonstration of the application was held by Dr. Jürgen Hermes for some of the future user and other interested parties. Even though all mayor construction sites were already resolved and the road for finalizing the code was paved, all feedback and feature-requests, that emerged from that presentation, could still be met. As such, we are proud to present to You the possibilities to append images and geolocations to Tweets, even applicable when importing huge sets from TSV-files!
+Our academic patrons did alot of testing and reviewing and the outcome of those test-runs and use-cases fed back into the development process. During the final phase of the implementation, a public demonstration of the application was held by Jürgen Hermes for some of the future user and other interested parties. Even though all mayor construction sites were already resolved and the road for finalizing the code was paved, all feedback and feature-requests, that emerged from that presentation, could still be met. As such, we are proud to present to You the possibilities to append images and geolocations to Tweets, even applicable when importing huge sets from TSV-files!
 
 #### Employed technologies
 This application is build upon the Spring MVC framework (with its Spring Social Twitter module) and uses Thymeleaf as templating-engine while custom styles are written in SASS. Behind the scenes Heideltime and the TreeTagger dig through Wikipedia-articles to find parsable dates and extract those. The fully documented source code of this application can be obtained from our public GitHub repository.

 #### Updates
 In march 2022, three other developers worked on the application and implemented the following upgrades.

 ##### Database update

 ##### Short threads
With the help of threads, it is possible on Twitter to connect several tweets in terms of content or categorically. Thus, additional context, an update to an existing tweet, or a more detailed argument can be added.

In autoChirp, it is possible to create threads within groups. Threading can be enabled or disabled via a button for a group. If enabled, it will then be set to all future tweets from that group that have not yet been posted. So if Threading is turned off after several tweets, but there are still tweets in the group, they will no longer be posted in a thread, but will be posted individually again. So currently threads are set manually by the user. This brings some disadvantages: when importing a table with tweets, it is a much higher effort to create threads for single tweets of a group or the user has to think in advance about which tweets should appear in a thread and put them directly into a separate group or table.

The idea of the short thread is to prevent this problem: when importing a Google or tsv table, it is already possible to classify the tweets into different threads. For each thread a separate group is created automatically and threading is activated. This way, the user no longer has to worry about when to activate and deactivate threading for which tweets.

 ##### Image errors

 ##### Automatized switches to/from daylight-saving-time