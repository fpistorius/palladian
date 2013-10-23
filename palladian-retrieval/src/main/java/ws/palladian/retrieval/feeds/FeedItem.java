package ws.palladian.retrieval.feeds;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Represents a news item within a feed ({@link Feed}).
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class FeedItem implements WebContent {
    
    private static final String SOURCE_NAME = "Feed";

    private int id = -1;

    /** The feed to which this item belongs to. */
    private Feed feed;

    /**
     * For performance reasons, we need to get feed items from the database and in that case we don't have the feed
     * object.
     */
    private int feedId = -1;

    private String title;
    private String link;

    /** Original ID from the feed entry. */
    private String rawId;

    /** The original publish date as read from the feed('s entry). */
    private Date published;

    /** When the entry was added to the database, usually set by the database. */
    private Date added;

    /** Author information. */
    private String authors;

    /** Description text of feed entry */
    private String description;

    /** Text directly from the feed entry */
    private String text;

    /** The item's hash. */
    private String itemHash = null;

    /** Allows to keep arbitrary, additional information. */
    private Map<String, Object> additionalData;

    /** The timestamp this item was fetched the first time. */
    private Date pollTimestamp;

    /**
     * The item's corrected published date. In contrast to {@link #published}, this value may be modified at the time of
     * the poll this item has been received the first time.
     */
    private Date correctedPublishedDate = null;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFeedId() {
        if (getFeed() != null) {
            return getFeed().getId();
        }
        return feedId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getUrl() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String getIdentifier() {
        return rawId;
    }

    public void setIdentifier(String rawId) {
        this.rawId = rawId;
    }

    /**
     * The original publish date as read from the feed('s item). Might be in the future.
     * 
     * @return
     */
    @Override
    public Date getPublished() {
        return published;
    }

    /**
     * The original publish date as read from the feed('s item).
     * 
     * @param published
     */
    public void setPublished(Date published) {
        this.published = published;
    }

    /**
     * When the entry was added to the database, usually set by the database.
     * 
     * @return Date the entry was added to the database, usually set by the database.
     */
    public Date getAdded() {
        return added;
    }

    /**
     * When the entry was added to the database, usually set by the database.
     * 
     * @param added Date the entry was added to the database, usually generated by the database.
     */
    public void setAdded(Date added) {
        this.added = added;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    @Override
    public String getSummary() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FeedItem");
        sb.append(" feedId:").append(feedId);
        sb.append(" id:").append(id);
        sb.append(" title:").append(title);
        sb.append(" link:").append(link);
        sb.append(" rawId:").append(rawId);
        sb.append(" published:").append(published);
        return sb.toString();
    }

    public String getFeedUrl() {
        if (getFeed() != null) {
            return getFeed().getFeedUrl();
        }
        return "";
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
        setFeedId(feed.getId());
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    /**
     * Replaces the current item hash with the given one. Don't never ever ever ever use this. This is meant to be used
     * only by the persistence layer and administrative authorities. And Chuck Norris.
     * 
     * <p>
     * Setting an item hash that has not been calculated by the current implementation of {@link #generateHash()} voids
     * the {@link Feed}'s duplicate detection. Duplicate items are not identified, you may get false positive MISSes.
     * This setter may be used to create items from persisted csv files used in the TUDCS6 dataset.
     * </p>
     * 
     * @param itemHash New item hash to set.
     */
    public void setHash(String itemHash) {
        this.itemHash = itemHash;
    }

    /**
     * The custom hash used to identify items beyond their raw id that is empty for 20% of the feeds.
     * 
     * @return The items custom hash.
     */
    public String getHash() {
        if (itemHash == null) {
            itemHash = FeedItemHashGenerator.generateHash(this);
        }
        return itemHash;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }

    /**
     * The item's corrected published date. In contrast to {@link #getPublished()}, this value may be modified at the
     * time of the poll this item has been received the first time.
     * 
     * @return the correctedPublishedTimestamp
     */
    public final Date getCorrectedPublishedDate() {
        return correctedPublishedDate;
    }

    /**
     * The item's corrected published date. In contrast to {@link #setPublished(Date)}, this value may be modified at
     * the time of the poll this item has been received the first time.
     * 
     * @param correctedPublishedDate the correctedPublishedTimestamp to set
     */
    public final void setCorrectedPublishedDate(Date correctedPublishedDate) {
        this.correctedPublishedDate = correctedPublishedDate;
    }

    /**
     * @return the pollTimestamp
     */
    public final Date getPollTimestamp() {
        return pollTimestamp;
    }

    /**
     * @param pollTimestamp the pollTimestamp to set
     */
    public final void setPollTimestamp(Date pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
    }
    
    @Override
    public GeoCoordinate getCoordinate() {
    	return null;
    }
    
    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    @Override
    public String getSource() {
        return SOURCE_NAME;
    }

}
