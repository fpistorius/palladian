package tud.iir.news;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.news.statistics.PollData;

public class FeedBenchmarkFileReader {

    protected static final Logger LOGGER = Logger.getLogger(FeedBenchmarkFileReader.class);

    private Feed feed;
    private FeedChecker feedChecker;
    private List<String> historyFileLines;
    private String historyFilePath = "";
    private int totalEntries = 0;

    /**
     * We need to loop through the file many times, to expedite the process we save the last index position where the
     * window started (actually one entry before the window so we can calculate a delay to the next entry).
     */
    private int lastStartIndex = 1;

    public FeedBenchmarkFileReader(Feed feed, FeedChecker feedChecker) {
        this.feed = feed;
        this.feedChecker = feedChecker;

        String safeFeedName = feed.getId()
                + "_"
                + StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst("www.", ""),
                        30);

        this.historyFilePath = feedChecker.findHistoryFile(safeFeedName);

        // if file doesn't exist skip the feed
        if (!new File(historyFilePath).exists()) {
            feed.setHistoryFileCompletelyRead(true);
        } else {
            try {
                this.historyFileLines = FileHelper.readFileToArray(historyFilePath);
                this.totalEntries = historyFileLines.size();
            } catch (Exception e) {
                LOGGER.error("out of memory error for feed " + feed.getId() + ", " + e.getMessage());
            }
        }
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public FeedChecker getFeedChecker() {
        return feedChecker;
    }

    public void setFeedChecker(FeedChecker feedChecker) {
        this.feedChecker = feedChecker;
    }

    public String getHistoryFilePath() {
        return historyFilePath;
    }

    public void setHistoryFilePath(String historyFilePath) {
        this.historyFilePath = historyFilePath;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    /**
     * For benchmarking purposes, we created a dataset of feed post histories and stored it on disk. We can now run the
     * feed reader on this dataset and evaluate the updateInterval techniques.
     */
    public void updateEntriesFromDisk() {

        try {
            List<FeedEntry> entries = new ArrayList<FeedEntry>();

            long lastEntryInWindowTimestamp = Long.MIN_VALUE;
            int misses = 0;
            long totalBytes = 0;
            boolean footHeadAdded = false;

            // the cumulated delay of the lookup times in milliseconds (in benchmark min mode), this can happen when we
            // read too early or too late
            long cumulatedDelay = 0l;

            // get hold of the post entry just before the window starts, this way we can determine the delay in case
            // there are no new entries between lookup time and last lookup time
            long postEntryBeforeWindowTime = 0l;

            // count new entries, they must be between lookup time and last lookup time
            int newEntries = 0;

            boolean windowStartIndexFound = false;

            for (int i = lastStartIndex; i <= totalEntries; i++) {

                String line = historyFileLines.get(i - 1);

                String[] parts = line.split(";");

                // skip MISS lines
                // if (parts[0].equalsIgnoreCase("miss")) {
                // return;
                // }

                if (feed.getWindowSize() == -1) {
                    int windowSize = Integer.valueOf(parts[5]);
                    if (windowSize > totalEntries) {
                        windowSize = totalEntries;
                    }
                    if (windowSize > 1000) {
                        LOGGER.info("feed has a window size of " + windowSize + " and will be discarded");
                        feed.setHistoryFileCompletelyRead(true);
                        return;
                    }
                    feed.setWindowSize(windowSize);
                }

                long entryTimestamp = Long.valueOf(parts[0]);

                // FIXME remove
                if (entryTimestamp < 1000000000000l) {
                    feed.setHistoryFileCompletelyRead(true);
                    return;
                }

                // get hold of the post entry just before the window starts
                if (entryTimestamp > feed.getBenchmarkLookupTime()) {
                    postEntryBeforeWindowTime = entryTimestamp;
                    lastStartIndex = i;
                    windowStartIndexFound = true;
                } else if (!windowStartIndexFound && i >= 2) {
                    i -= 2;
                    continue;
                }

                // process post entries that are in the current window
                // if ((entryTimestamp < feed.getBenchmarkLookupTime() && entries.size() < feed.getWindowSize()) ||
                // totalEntries - i < feed.getWindowSize()) {
                if ((entryTimestamp <= feed.getBenchmarkLookupTime() || totalEntries - i < feed.getWindowSize())
                        && entries.size() < feed.getWindowSize()) {

                    windowStartIndexFound = true;

                    // find the first lookup date if the file has not been read yet
                    if (feed.getBenchmarkLookupTime() == Long.MIN_VALUE && totalEntries - i + 1 == feed.getWindowSize()) {
                        feed.setBenchmarkLookupTime(entryTimestamp);
                    }

                    // add up download size (head and foot if necessary and post size itself)
                    if (!footHeadAdded) {
                        totalBytes = totalBytes + Integer.valueOf(parts[4]);
                        footHeadAdded = true;
                    }

                    totalBytes += Integer.valueOf(parts[3]);

                    // FeedEntry fe = feedEntryMap.get(i);
                    // if (fe == null) {

                    // create feed entry
                    FeedEntry feedEntry = new FeedEntry();
                    feedEntry.setPublished(new Date(entryTimestamp));
                    feedEntry.setTitle(parts[1]);
                    feedEntry.setLink(parts[2]);

                    // fe = feedEntry;

                    // feedEntryMap.put(i, fe);
                    // }

                    entries.add(feedEntry);

                    // for all post entries in the window that are newer than the last lookup time we need to sum up the
                    // delay to the current lookup time (and weight it)
                    if (entryTimestamp > feed.getBenchmarkLastLookupTime() && feed.getChecks() > 0) {
                        cumulatedDelay += feed.getBenchmarkLookupTime() - entryTimestamp;

                        // count new entry
                        newEntries++;
                    }

                    // if top of the file is reached, we read the file completely and can stop scheduling reading this
                    // feed
                    if (i == 1) {
                        LOGGER.debug("complete history has been read for feed " + feed.getId() + " ("
                                + feed.getFeedUrl() + ")");
                        feed.setHistoryFileCompletelyRead(true);
                    }

                    // check whether current post entry is the last one in the window
                    if (entries.size() == feed.getWindowSize()) {
                        lastEntryInWindowTimestamp = entryTimestamp;
                    }
                }

                // process post entries between the end of the current window and the last lookup time
                else if (entryTimestamp < lastEntryInWindowTimestamp
                        && entryTimestamp > feed.getBenchmarkLastLookupTime()) {

                    cumulatedDelay += feed.getBenchmarkLookupTime() - entryTimestamp;

                    // count post entry as miss
                    misses = misses + 1;
                    // System.out.println("miss");
                } else if (entryTimestamp <= feed.getBenchmarkLastLookupTime()) {
                    break;
                }

            }

            // if no new entry was found, we add the delay to the next new post entry
            if (newEntries == 0 && postEntryBeforeWindowTime != 0l && feed.getChecks() > 0) {
                cumulatedDelay = feed.getBenchmarkLookupTime() - postEntryBeforeWindowTime;
            }

            feed.setEntries(entries);

            // now that we set the entries we can add information about the poll to the poll series
            PollData pollData = new PollData();

            pollData.setBenchmarkType(FeedChecker.benchmark);
            pollData.setTimestamp(feed.getBenchmarkLookupTime());
            pollData.setPercentNew(feed.getTargetPercentageOfNewEntries());
            pollData.setMisses(misses);

            if (FeedChecker.benchmark == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
                pollData.setCheckInterval(feed.getMaxCheckInterval());
            } else {
                pollData.setCheckInterval(feed.getMinCheckInterval());
            }

            pollData.setNewPostDelay(cumulatedDelay);
            pollData.setWindowSize(feed.getWindowSize());
            pollData.setDownloadSize(totalBytes);

            // add poll data object to series of poll data
            feed.getPollDataSeries().add(pollData);

            // remember the time the feed has been checked
            feed.setLastChecked(new Date());

            feedChecker.updateCheckIntervals(feed);

            if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MIN_CHECK_TIME) {
                feed.addToBenchmarkLookupTime((long) feed.getMinCheckInterval() * (long) DateHelper.MINUTE_MS);
            } else if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
                feed.addToBenchmarkLookupTime((long) feed.getMinCheckInterval() * (long) DateHelper.MINUTE_MS);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            feed.setHistoryFileCompletelyRead(true);
        }

        // feed.increaseChecks();

    }

}
