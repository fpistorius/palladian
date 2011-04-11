package ws.palladian.retrieval.feeds;

import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;

/**
 * A scheduler task handles the distribution of feeds to worker threads that
 * read these feeds.
 * 
 * @author Klemens Muthmann
 * 
 */
class SchedulerTask extends TimerTask {

	/**
	 * The logger for objects of this class. Configure it using
	 * <tt>src/main/resources/log4j.xml</tt>.
	 */
	private static final Logger LOGGER = Logger.getLogger(SchedulerTask.class);

	/**
	 * The collection of all the feeds this scheduler should create update
	 * threads for.
	 */
	private transient final FeedReader feedReader;

	/**
	 * The thread pool managing threads that read feeds from the feed sources
	 * provided by {@link #collectionOfFeeds}.
	 */
	private transient final ExecutorService threadPool;

	/**
	 * Tasks currently scheduled but not yet checked.
	 */
	private transient final Map<Integer, Future<?>> scheduledTasks;

	/**
	 * Creates a new {@code SchedulerTask} for a feed reader.
	 * 
	 * @param feedReader
	 *            The feed reader containing settings and providing the
	 *            collection of feeds to check.
	 */
	public SchedulerTask(final FeedReader feedReader) {
		super();
		threadPool = Executors.newFixedThreadPool(feedReader
				.getThreadPoolSize());
		this.feedReader = feedReader;
		scheduledTasks = new TreeMap<Integer, Future<?>>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		LOGGER.debug("wake up to check feeds");
		int feedCount = 0;
		for (Feed feed : feedReader.getFeeds()) {
			if (needsLookup(feed)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Scheduling feed at address: "
							+ feed.getFeedUrl());
				}
				// check whether feed is in the queue already
				if (isScheduled(feed.getId())) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("It seems the machine cannot keep up with update intervall since feed "
								+ feed.getId() + " is already scheduled.");
					}
					continue;
				}
				// incrementThreadPoolSize();
				scheduledTasks.put(feed.getId(),
						threadPool.submit(new FeedTask(feed, feedReader)));
				feedCount++;
			}

		}
		LOGGER.info("Scheduled " + feedCount + " feeds for reading");
		LOGGER.info("Queue now contains: " + scheduledTasks.size());
	}

	/**
	 * Returns whether the last time the provided feed was checked for updates
	 * is further in the past than its update interval.
	 * 
	 * @param feed
	 *            The feed to check.
	 * @return {@code true} if this feeds check interval is over and
	 *         {@code false} otherwise.
	 */
	private Boolean needsLookup(final Feed feed) {
		final long now = System.currentTimeMillis();
		return feed.getChecks() == 0
				|| feed.getLastPollTime() == null
				|| now - feed.getLastPollTime().getTime() > feed
						.getUpdateInterval() * DateHelper.MINUTE_MS;
	}

	/**
	 * Checks if this feed is already queued for updates. If this is the case
	 * one should not queue it a second time to reduce traffic.
	 * 
	 * @param feedId
	 *            The id of the feed to check.
	 * @return {@code true} if this feed is already queued and {@code false}
	 *         otherwise.
	 */
	private Boolean isScheduled(final Integer feedId) {
		final Future<?> future = scheduledTasks.get(feedId);
		Boolean ret = true;
		if (future == null) {
			ret = false;
		} else {
			if (future.isDone()) {
				scheduledTasks.remove(feedId);
				ret = false;
			}
		}
		return ret;
	}
}
