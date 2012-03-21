package ws.palladian.helper.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * This class provides some helper methods for working with collections. <b>Important:</b> If you are looking for a
 * functionality which is not provided here, look in {@link Collections}, {@link Arrays} and {@link CollectionUtils}
 * first, before adding new, redundant methods here!
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CollectionHelper {

    public static boolean ASCENDING = true;
    public static boolean DESCENDING = false;

    private CollectionHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map, final boolean ascending) {

        LinkedList<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());

        Comparator<Map.Entry<K, V>> comparator = new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                int ret = o1.getValue().compareTo(o2.getValue());
                return ascending ? ret : -ret;
            }
        };
        Collections.sort(list, comparator);

        LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @return A sorted map, in ascending order.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, CollectionHelper.ASCENDING);
    }

    // /**
    // * Sort a {@link HashMap} by value.
    // *
    // * @param <K> Type of the keys.
    // * @param <V> Type of the values.
    // * @param entrySet The entry set.
    // * @return A sorted map, in ascending order.
    // */
    // public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Set<Map.Entry<K, V>> entrySet) {
    // return CollectionHelper.sortByValue(entrySet, CollectionHelper.ASCENDING);
    // }

    // /**
    // * Sort a {@link HashMap} by value.
    // *
    // * @param <K> Type of the keys.
    // * @param <V> Type of the values.
    // * @param entrySet The entry set.
    // * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
    // * @return A sorted map.
    // */
    // public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Set<Map.Entry<K, V>> entrySet,
    // boolean ascending) {
    //
    // }

    /**
     * <p>
     * Sort a {@link HashMap} by length of the key string.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The entry set.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <V extends Comparable<V>> LinkedHashMap<String, V> sortByStringKeyLength(Map<String, V> map,
            final boolean ascending) {

        LinkedList<Map.Entry<String, V>> list = new LinkedList<Map.Entry<String, V>>(map.entrySet());

        Comparator<Map.Entry<String, V>> comparator = new Comparator<Map.Entry<String, V>>() {
            @Override
            public int compare(Map.Entry<String, V> o1, Map.Entry<String, V> o2) {
                int ret = new Integer(o1.getKey().length()).compareTo(o2.getKey().length());
                return ascending ? ret : -ret;
            }
        };
        Collections.sort(list, comparator);

        LinkedHashMap<String, V> result = new LinkedHashMap<String, V>();
        for (Entry<String, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <p>
     * Get a key given for a value (1 to 1 {@link Map}s).
     * </p>
     * 
     * @param value The value.
     * @return The key that matches the given value, or <code>null</code> if no such value.
     */
    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Entry<K, V> mapEntry : map.entrySet()) {
            if (mapEntry.getValue().equals(value)) {
                return mapEntry.getKey();
            }
        }
        return null;
    }

    // /**
    // * <p>
    // * Get a human readable, line separated output of an Array.
    // * </p>
    // *
    // * @param array
    // * @return
    // */
    // public static String getPrint(Object[] array) {
    // Set<Object> set = new HashSet<Object>();
    // for (Object o : array) {
    // set.add(o);
    // }
    // return getPrint(set);
    // }

    /**
     * <p>
     * Print a human readable, line separated output of an Array.
     * </p>
     * 
     * @param array
     */
    public static void print(Object[] array) {
        for (Object o : array) {
            System.out.println(o);
        }
        System.out.println("#Entries: " + array.length);
    }

    /**
     * <p>
     * Print a human readable, line separated output of a {@link Map}.
     * </p>
     * 
     * @param <K>
     * @param <V>
     * @param map
     */
    public static <K, V> void print(Map<K, V> map) {
        print(map, -1);
    }

    public static <K, V> void print(Map<K, V> map, int limit) {
        int c = 0;
        Iterator<Map.Entry<K, V>> mapIterator = map.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<K, V> entry = mapIterator.next();
            System.out.println(entry.getKey() + " : " + entry.getValue());
            c++;
            if (c >= limit && limit > -1) {
                break;
            }
        }
        System.out.println("#Entries: " + map.entrySet().size());
    }

    // /**
    // * <p>
    // * Check whether an Array contains an entry.
    // * </p>
    // *
    // * @param array The array.
    // * @param entry The entry that is checked against the array.
    // * @return True, if the entry is contained in the array, false otherwise.
    // * @deprecated Use {@link Arrays#asList(Object...)} and {@link List#contains(Object)} instead:
    // <code>Arrays.asList(array).contains(entry);</code>
    // */
    // @Deprecated
    // public static <T> boolean contains(T[] array, T entry) {
    // for (T s : array) {
    // if (s.equals(entry)) {
    // return true;
    // }
    // }
    // return false;
    // }

    /**
     * <p>
     * Get a human readable, line separated output of a {@link Collection}.
     * </p>
     * 
     * @param collection
     * @return
     */
    public static String getPrint(Collection<?> collection) {
        StringBuilder s = new StringBuilder();

        for (Object entry : collection) {
            s.append(entry).append("\n");
        }
        s.append("#Entries: ").append(collection.size()).append("\n");

        return s.toString();
    }

    /**
     * <p>
     * Print a human readable, line separated output of a {@link Collection}.
     * </p>
     * 
     * @param collection
     */
    public static void print(Collection<?> collection) {
        System.out.println(getPrint(collection));
    }

    // /**
    // * Convert a string array to a Set, skip empty strings.
    // *
    // * @param array
    // * @return
    // */
    // public static HashSet<String> toHashSet(String[] array) {
    // HashSet<String> set = new HashSet<String>();
    // for (String s : array) {
    // if (s.length() > 0) {
    // set.add(s);
    // }
    // }
    // return set;
    // }

    // /**
    // * <p>
    // * Converts a {@link List} to a {@link TreeSet}.
    // * </p>
    // *
    // * @param <T>
    // * @param list
    // * @return
    // */
    // public static <T> TreeSet<T> toTreeSet(List<T> list) {
    // TreeSet<T> set = new TreeSet<T>();
    // for (T item : list) {
    // set.add(item);
    // }
    // return set;
    // }

    // /**
    // * Removes null objects out of an array.
    // *
    // * @param <T>
    // * @param array
    // * @return
    // * @deprecated Use {@link List#removeAll(Collection)} with {@link Collections#singletonList(Object)} initalized to
    // * <code>null</code> instead: <code>list.removeAll(Collections.singletonList(null));</code>
    // */
    // @Deprecated
    // public static <T> ArrayList<T> removeNullElements(ArrayList<T> array) {
    // ArrayList<T> returnArray = new ArrayList<T>();
    // Iterator<T> iterator = array.iterator();
    // while (iterator.hasNext()) {
    // T element = iterator.next();
    // if (element != null) {
    // returnArray.add(element);
    // }
    // }
    // return returnArray;
    // }

    /**
     * <p>
     * Concatenate two String arrays.
     * </p>
     * 
     * @param array1
     * @param array2
     * @return The concatenated String array consisting of the first, then the second array's items.
     */
    public static String[] concat(String[] array1, String[] array2) {
        String[] helpArray = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, helpArray, 0, array1.length);
        System.arraycopy(array2, 0, helpArray, array1.length, array2.length);

        return helpArray;
    }

    // /**
    // * Returns a ArrayList of keys of map.
    // * @param <K>
    // * @param <V>
    // * @param map
    // * @return
    // */
    // public static <K,V> ArrayList<K> toArrayList(Map<K, V> map){
    // ArrayList<K> returnList = new ArrayList<K>();
    // for(Entry<K, V> e : map.entrySet()){
    // returnList.add(e.getKey());
    // }
    // return returnList;
    // }

    // /**
    // * <p>
    // * Intersect two sets and return a new set with the elements that both sets had in common.
    // * </p>
    // *
    // * @param set1 The first set.
    // * @param set2 The second set.
    // * @return The intersection between the sets.
    // * @deprecated Use {@link CollectionUtils#intersection(Collection, Collection)}
    // */
    // @Deprecated
    // public static <T> Set<T> intersect(Set<T> set1, Set<T> set2) {
    // Set<T> intersection = new HashSet<T>(set1);
    // intersection.retainAll(new HashSet<T>(set2));
    // return intersection;
    // }

    // /**
    // * <p>
    // * Unite two sets and return a new set with the elements from both sets.
    // * </p>
    // *
    // * @param set1 The first set.
    // * @param set2 The second set.
    // * @return The union of the sets.
    // * @deprecated Use {@link CollectionUtils#union(Collection, Collection)}
    // */
    // @Deprecated
    // public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
    // Set<T> union = new HashSet<T>(set1);
    // union.addAll(new HashSet<T>(set2));
    // return union;
    // }
}