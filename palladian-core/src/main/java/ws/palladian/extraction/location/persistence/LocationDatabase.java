package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.OneColumnRowConverter;
import ws.palladian.persistence.RowConverter;

public class LocationDatabase extends DatabaseManager implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDatabase.class);

    // ////////////////// location prepared statements ////////////////////
    // XXX using INSERT IGNORE to avoid lots of errors because of unique constraints
    private static final String ADD_LOCATION = "INSERT IGNORE INTO locations SET type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String ADD_ALTERNATIVE_NAME = "INSERT IGNORE INTO location_alternative_names SET locationId = ?, alternativeName = ?";
    private static final String GET_LOCATION = "SELECT * FROM locations WHERE name = ? UNION SELECT l.* FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ? GROUP BY id";
    private static final String GET_LOCATION_ALTERNATIVE_NAMES = "SELECT alternativeName FROM location_alternative_names WHERE locationId = ?";

    // ////////////////// row converts ////////////////////////////////////
    private static final RowConverter<Location> LOCATION_ROW_CONVERTER = new RowConverter<Location>() {
        @Override
        public Location convert(ResultSet resultSet) throws SQLException {
            Location location = new Location();
            location.setId(resultSet.getInt("id"));
            location.setType(LocationType.valueOf(resultSet.getString("type")));
            location.setPrimaryName(resultSet.getString("name"));
            location.setLatitude(resultSet.getDouble("latitude"));
            location.setLongitude(resultSet.getDouble("longitude"));
            location.setPopulation(resultSet.getLong("population"));
            return location;
        }
    };

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        List<Location> locations = runQuery(LOCATION_ROW_CONVERTER, GET_LOCATION, locationName, locationName);
        for (Location location : locations) {
            List<String> alternativeNames = getAlternativeNames(location.getId());
            if (alternativeNames.size() > 0) {
                location.setAlternativeNames(alternativeNames);
            }
        }
        return locations;
    }

    private List<String> getAlternativeNames(int locationId) {
        return runQuery(OneColumnRowConverter.STRING, GET_LOCATION_ALTERNATIVE_NAMES, locationId);
    }

    @Override
    public void save(Location location) {
        List<Object> args = CollectionHelper.newArrayList();
        args.add(location.getType().toString());
        args.add(location.getPrimaryName());
        args.add(location.getLongitude());
        args.add(location.getLatitude());
        args.add(location.getPopulation());
        int generatedLocationId = runInsertReturnId(ADD_LOCATION, args);

        if (generatedLocationId < 1) {
            // TODO something went wrong
            return;
        }

        // save alternative location names
        for (String alternativeName : location.getAlternativeNames()) {
            runInsertReturnId(ADD_ALTERNATIVE_NAME, generatedLocationId, alternativeName);
        }
    }

    public void truncate() {
        LOGGER.warn("Truncating the database");
        runUpdate("TRUNCATE TABLE locations");
        runUpdate("TRUNCATE TABLE location_alternative_names");
    }

    public static void main(String[] args) {
        LocationDatabase locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        for (String city : Arrays.asList("flein", "köln", "dresden", "norway")) {
            StopWatch stopWatch = new StopWatch();
            List<Location> locations = locationSource.retrieveLocations(city);
            CollectionHelper.print(locations);
            System.out.println(stopWatch);
        }
    }

}