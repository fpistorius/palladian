package ws.palladian.extraction.location;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.TrainableWrap;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class FeatureBasedDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguation.class);

    private final Set<Trainable> trainInstanceCollection = CollectionHelper.newHashSet();

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> annotations, MultiMap<String, Location> locations) {

        Set<LocationInstance> instances = makeInstances(annotations, locations, "foo");
        CollectionHelper.print(instances);

        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    public void addTrainData(List<Annotated> annotations, MultiMap<String, Location> locations, Set<Location> positive,
            String fileName) {
        Set<LocationInstance> instances = makeInstances(annotations, locations, fileName);
        Set<Trainable> trainInstances = markPositiveInstances(instances, positive);
        trainInstanceCollection.addAll(trainInstances);
    }

    private static Set<Trainable> markPositiveInstances(Set<LocationInstance> instances, Set<Location> positive) {
        Set<Trainable> result = CollectionHelper.newHashSet();
        int numPositive = 0;
        for (LocationInstance instance : instances) {
            boolean positiveClass = false;
            for (Location location : positive) {
                // we cannot determine the correct location, if the training data did not provide coordinates
                if (instance.getLatitude() == null || instance.getLongitude() == null) {
                    continue;
                }
                boolean samePlace = GeoUtils.getDistance(instance, location) < 50;
                boolean sameName = LocationExtractorUtils.commonName(instance, location);
                if (samePlace && sameName) {
                    numPositive++;
                    positiveClass = true;
                    break;
                }
            }
            result.add(new TrainableWrap(instance, String.valueOf(positiveClass)));
        }

        double positivePercentage = MathHelper.round((float)numPositive / instances.size() * 100, 2);
        LOGGER.info("{} positive instances in {} ({}%)", new Object[] {numPositive, instances.size(),
                positivePercentage});
        return result;
    }

    public void buildModel() {
        ClassificationUtils.writeToCsv(trainInstanceCollection, new File("instancesNew2.csv"));
    }

    private static Set<LocationInstance> makeInstances(List<Annotated> annotations,
            MultiMap<String, Location> locations, String fileName) {
        Set<LocationInstance> instances = CollectionHelper.newHashSet();
        Collection<Location> allLocations = locations.allValues();

        CountMap<String> counts = getCounts(annotations);
        int annotationCount = annotations.size();

        for (Annotated annotation : annotations) {
            String value = annotation.getValue();
            Collection<Location> candidates = locations.get(LocationExtractorUtils.normalizeName(value));
            for (Location location : candidates) {

                Set<Location> others = new HashSet<Location>(allLocations);
                others.remove(location);
                Long population = location.getPopulation();

                // extract features and add them to the feature vector
                FeatureVector fv = new FeatureVector();
                fv.add(new NominalFeature("locationType", location.getType().toString()));
                fv.add(new NumericFeature("population", population));
                fv.add(new NumericFeature("populationMagnitude", MathHelper.getOrderOfMagnitude(population)));
                fv.add(new NumericFeature("numTokens", value.split("\\s").length));
                fv.add(new NumericFeature("numCharacters", value.length()));
                fv.add(new NumericFeature("ambiguity", 1. / candidates.size()));
                fv.add(new BooleanFeature("acronym", isAcronym(annotation.getValue())));
                fv.add(new NumericFeature("count", counts.getCount(value)));
                fv.add(new NumericFeature("frequency", (double)counts.getCount(value) / annotationCount));
                fv.add(new BooleanFeature("parentOccurs", parentOccurs(location, others)));
                fv.add(new BooleanFeature("ancestorOccurs", ancestorOccurs(location, others)));
                fv.add(new NumericFeature("numLocIn10", countLocationsInDistance(location, others, 10)));
                fv.add(new NumericFeature("numLocIn50", countLocationsInDistance(location, others, 50)));
                fv.add(new NumericFeature("numLocIn100", countLocationsInDistance(location, others, 100)));
                fv.add(new NumericFeature("numLocIn250", countLocationsInDistance(location, others, 250)));
                fv.add(new NumericFeature("distLoc1m", getDistanceToPopulation(location, others, 1000000)));
                fv.add(new NumericFeature("distLoc100k", getDistanceToPopulation(location, others, 100000)));
                fv.add(new NumericFeature("distLoc10k", getDistanceToPopulation(location, others, 10000)));
                fv.add(new NumericFeature("distLoc1k", getDistanceToPopulation(location, others, 1000)));
                fv.add(new NumericFeature("popIn10", getPopulationInRadius(location, others, 10)));
                fv.add(new NumericFeature("popIn50", getPopulationInRadius(location, others, 50)));
                fv.add(new NumericFeature("popIn100", getPopulationInRadius(location, others, 100)));
                fv.add(new NumericFeature("popIn250", getPopulationInRadius(location, others, 250)));
                fv.add(new NumericFeature("siblingCount", siblingCount(location, others)));
                fv.add(new BooleanFeature("siblingOccurs", siblingCount(location, others) > 0));

                // just for debuggging purposes
                fv.add(new NominalFeature("locationId", String.valueOf(location.getId())));
                fv.add(new NominalFeature("documentId", fileName));

                instances.add(new LocationInstance(location, fv));
            }
        }
        return instances;
    }

    private static int getPopulationInRadius(Location location, Collection<Location> others, double distance) {
        int population = 0;
        for (Location other : others) {
            if (GeoUtils.getDistance(location, other) <= distance) {
                population += other.getPopulation();
            }
        }
        return population;
    }

    private static int getDistanceToPopulation(Location location, Collection<Location> others, int population) {
        int distance = Integer.MAX_VALUE;
        for (Location other : others) {
            if (other.getPopulation() >= population) {
                distance = (int)Math.min(distance, GeoUtils.getDistance(other, location));
            }
        }
        return distance;
    }

    private static int countLocationsInDistance(Location location, Collection<Location> others, double distance) {
        int count = 0;
        for (Location other : others) {
            if (GeoUtils.getDistance(location, other) < distance) {
                count++;
            }
        }
        return count;
    }

    private static boolean ancestorOccurs(Location location, Collection<Location> others) {
        for (Location other : others) {
            if (LocationExtractorUtils.isChildOf(location, other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean parentOccurs(Location location, Collection<Location> others) {
        for (Location other : others) {
            if (LocationExtractorUtils.isDirectChildOf(location, other)) {
                return true;
            }
        }
        return false;
    }

    private static int siblingCount(Location location, Collection<Location> others) {
        int count = 0;
        for (Location other : others) {
            if (location.getAncestorIds().equals(other.getAncestorIds())) {
                count++;
            }
        }
        return count;
    }

    private static CountMap<String> getCounts(List<Annotated> annotations) {
        CountMap<String> frequencies = CountMap.create();
        for (Annotated annotation : annotations) {
            frequencies.add(LocationExtractorUtils.normalizeName(annotation.getValue()));
        }
        return frequencies;
    }

    private static boolean isAcronym(String value) {
        return value.matches("[A-Z]+|([A-Z]\\.)+");
    }

    private static final class LocationInstance implements Location, Classifiable {

        private final Location location;
        private final FeatureVector featureVector;

        public LocationInstance(Location location, FeatureVector featureVector) {
            this.location = location;
            this.featureVector = featureVector;
        }

        @Override
        public Double getLatitude() {
            return location.getLatitude();
        }

        @Override
        public Double getLongitude() {
            return location.getLongitude();
        }

        @Override
        public int getId() {
            return location.getId();
        }

        @Override
        public String getPrimaryName() {
            return location.getPrimaryName();
        }

        @Override
        public Collection<AlternativeName> getAlternativeNames() {
            return location.getAlternativeNames();
        }

        @Override
        public LocationType getType() {
            return location.getType();
        }

        @Override
        public Long getPopulation() {
            return location.getPopulation();
        }

        @Override
        public List<Integer> getAncestorIds() {
            return location.getAncestorIds();
        }

        @Override
        public FeatureVector getFeatureVector() {
            return featureVector;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((featureVector == null) ? 0 : featureVector.hashCode());
            result = prime * result + ((location == null) ? 0 : location.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LocationInstance other = (LocationInstance)obj;
            if (featureVector == null) {
                if (other.featureVector != null)
                    return false;
            } else if (!featureVector.equals(other.featureVector))
                return false;
            if (location == null) {
                if (other.location != null)
                    return false;
            } else if (!location.equals(other.location))
                return false;
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LocationInstance [location=");
            builder.append(location);
            builder.append(", featureVector=");
            builder.append(featureVector);
            builder.append("]");
            return builder.toString();
        }

    }

}
