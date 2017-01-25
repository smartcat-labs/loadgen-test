package io.smartcat.data.loader;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.smartcat.data.loader.rules.DiscreteRule;
import io.smartcat.data.loader.rules.DiscreteRuleBoolean;
import io.smartcat.data.loader.rules.RangeRuleDate;
import io.smartcat.data.loader.rules.RangeRuleDouble;
import io.smartcat.data.loader.rules.RangeRuleFloat;
import io.smartcat.data.loader.rules.RangeRuleInt;
import io.smartcat.data.loader.rules.RangeRuleLong;
import io.smartcat.data.loader.rules.RangeRuleShort;
import io.smartcat.data.loader.rules.Rule;
import io.smartcat.data.loader.rules.SubListRule;
import io.smartcat.data.loader.rules.SubSetRule;
import io.smartcat.data.loader.rules.UUIDRule;
import io.smartcat.data.loader.util.Randomizer;
import io.smartcat.data.loader.util.RandomizerImpl;

/**
 * Class used for building random objects of certain type.
 *
 * @param <T>
 */
public class RandomBuilder<T> {

    private Class<T> objectType;

    private Map<String, Rule<?>> fieldRules = new HashMap<>();

    private int numberOfObjects;

    private Map<String, RandomBuilder<?>> nestedObjectBuilderMap = new HashMap<>();

    private Randomizer random = new RandomizerImpl();

    /**
     * Creates RandomBuilder that is used with passed {@code objectType}.
     *
     * @param objectType type of the object to be built with this builder
     */
    public RandomBuilder(Class<T> objectType) {
        this.objectType = objectType;
    }

    /**
     * Creates RandomBuilder that is used with passed {@code objectType} and randomizer.
     *
     * @param objectType type of the object to be built with this builder
     * @param random Randomizer impl.
     */
    public RandomBuilder(Class<T> objectType, Randomizer random) {
        this.objectType = objectType;
        this.random = random;
    }

    /**
     * Sets the allowed ranges of Shorts for the field with {@code fieldName}. The ranges are defined by array of Shorts
     * S1,S2, ... ,Sn such that S1 < S2 < ... < Sn and (n % 2) = 0;
     *
     * The ranges defined by S1,S2, ... ,Sn are: [S1,S2), [S3,S4), ... , [Sn-1, Sn). In each range [Sj,Sk) Sj denotes
     * inclusive start of the range and Sk denotes exclusive end of the range.
     *
     *
     * @param fieldName name of the field in the type <T>
     * @param rangeMarkers array of Short that denotes the ranges.
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomFromRange(String fieldName, Short... rangeMarkers) {
        checkRangeInput(rangeMarkers);
        fieldRules.put(fieldName, RangeRuleShort.withRanges(rangeMarkers).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed ranges of Integers for the field with {@code fieldName}. The ranges are defined by an array of
     * Integers S1,S2, ... ,Sn such that S1 < S2 < ... < Sn and (n % 2) = 0;
     *
     * The ranges defined by S1,S2, ... ,Sn are: [S1,S2), [S3,S4), ... , [Sn-1, Sn). In each range [Sj,Sk) Sj denotes
     * inclusive start of the range and Sk denotes exclusive end of the range.
     *
     *
     * @param fieldName name of the field in the type <T>
     * @param rangeMarkers array of Integers that denotes the ranges.
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomFromRange(String fieldName, Integer... rangeMarkers) {
        checkRangeInput(rangeMarkers);
        fieldRules.put(fieldName, RangeRuleInt.withRanges(rangeMarkers).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed ranges of Float for the field with {@code fieldName}. The ranges are defined by an array of
     * Floats F1,F2, ... ,Fn such that F1 < F2 < ... < Fn and (n % 2) = 0;
     *
     * The ranges defined by F1,F2, ... ,Fn are: [F1,F2), [F3,F4), ... , [Fn-1, Fn). In each range [Fj,Fk) Fj denotes
     * inclusive start of the range and Fk denotes exclusive end of the range.
     *
     *
     * @param fieldName name of the field in the type <T>
     * @param rangeMarkers array of Floats that denotes the ranges.
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomFromRange(String fieldName, Float... rangeMarkers) {
        checkRangeInput(rangeMarkers);
        fieldRules.put(fieldName, RangeRuleFloat.withRanges(rangeMarkers).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed ranges of dates for the field with {@code fieldName}.
     *
     * Note that the corner cases will always be generated first in order to ensure protection against off-by-one
     * errors. For example, if startDate is 2000-01-01 and endDate is 2000-01-04 the generator will first create two
     * corner cases, that is: 2000-01-01 00:00:00:000 and 2000-01-03 23:59:59:999. The end of the range is calculated
     * with millisecond granularity.
     *
     * @param fieldName name of the field in the type <T>
     * @param startDate start of the range (inclusive)
     * @param endDate end of the range (exclusive)
     * @return RandomBuilder<T>
     *
     * @throws IllegalArgumentException if {@code startDate} is greater than <i>or equal to</i> {@code endDate}
     */
    public RandomBuilder<T> randomFromRange(String fieldName, LocalDateTime startDate, LocalDateTime endDate) {
        checkRangeInput(startDate, endDate);
        Instant lower = startDate.toInstant(ZoneOffset.UTC);
        Instant upper = endDate.toInstant(ZoneOffset.UTC);

        fieldRules.put(fieldName, RangeRuleDate.withRanges(Date.from(lower), Date.from(upper)).withRandom(random));

        return this;
    }

    /**
     * Sets the exclusive allowed ranges of dates for the field with {@code fieldName}. That means that only this
     * instance of the builder is allowed to set property with passed name {@code fieldName} from these ranges.
     *
     * Note that the corner cases will always be generated first in order to ensure protection against off-by-one
     * errors. For example, if startDate is 2000-01-01 and endDate is 2000-01-04 the generator will first create two
     * corner cases, that is: 2000-01-01 00:00:00:000 and 2000-01-03 23:59:59:999. The end of the range is calculated
     * with millisecond granularity.
     *
     * @param fieldName name of the field in the type <T>
     * @param startDate start of the range (inclusive)
     * @param endDate end of the range (exclusive)
     * @return RandomBuilder<T>
     *
     * @throws IllegalArgumentException if {@code startDate} is greater than <i>or equal to</i> {@code endDate}
     */
    public RandomBuilder<T> exclusiveRandomFromRange(String fieldName, LocalDateTime startDate, LocalDateTime endDate) {
        checkRangeInput(startDate, endDate);
        Instant lower = startDate.toInstant(ZoneOffset.UTC);
        Instant upper = endDate.toInstant(ZoneOffset.UTC);

        fieldRules.put(fieldName, RangeRuleDate.withRangesX(Date.from(lower), Date.from(upper)).withRandom(random));

        return this;
    }

    /**
     * Sets the allowed ranges of Longs for the field with {@code fieldName}.
     *
     * @param fieldName name of the field in the type <T>
     * @param lower start of the range (inclusive)
     * @param upper end of the range (exclusive)
     * @return RandomBuilder<T>
     *
     * @throws IllegalArgumentException if {@code lower} is greater than <i>or equal to</i> {@code upper}
     */
    public RandomBuilder<T> randomFromRange(String fieldName, Long lower, Long upper) {
        checkRangeInput(lower, upper);
        fieldRules.put(fieldName, RangeRuleLong.withRanges(lower, upper).withRandom(random));
        return this;
    }

    /**
     * Sets the exclusive allowed ranges of Longs for the field with {@code fieldName}. That means that only this
     * instance of the builder is allowed to set property with passed name {@code fieldName} from these ranges.
     *
     * @param fieldName name of the field in the type <T>
     * @param lower start of the range (inclusive)
     * @param upper end of the range (exclusive)
     * @return RandomBuilder<T>
     *
     * @throws IllegalArgumentException if {@code lower} is greater than <i>or equal to</i> {@code upper}
     */
    public RandomBuilder<T> exclusiveRandomFromRange(String fieldName, Long lower, Long upper) {
        checkRangeInput(lower, upper);
        fieldRules.put(fieldName, RangeRuleLong.withRangesX(lower, upper).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed ranges of Doubles for the field with {@code fieldName}.
     *
     * @param fieldName name of the field in the type <T>
     * @param lower start of the range (inclusive)
     * @param upper end of the range (exclusive)
     * @return RandomBuilder<T>
     *
     * @throws IllegalArgumentException if {@code lower} is greater than <i>or equal to</i> {@code upper}
     */
    public RandomBuilder<T> randomFromRange(String fieldName, Double lower, Double upper) {
        checkRangeInput(lower, upper);
        fieldRules.put(fieldName, RangeRuleDouble.withRanges(lower, upper).withRandom(random));
        return this;
    }

    /**
     * Sets the exclusive allowed ranges of Doubles for the field with {@code fieldName}. That means that only this
     * instance of the builder is allowed to set property with passed name {@code fieldName} from these ranges.
     *
     * @param fieldName name of the field in the type <T>
     * @param lower start of the range (inclusive)
     * @param upper end of the range (exclusive)
     * @return RandomBuilder<T>
     *
     * @throws IllegalArgumentException if {@code lower} is greater than <i>or equal to</i> {@code upper}
     */
    public RandomBuilder<T> exclusiveRandomFromRange(String fieldName, Double lower, Double upper) {
        checkRangeInput(lower, upper);
        fieldRules.put(fieldName, RangeRuleDouble.withRangesX(lower, upper).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed list of String values for the field with {@code fieldName}.
     *
     * @param fieldName name of the field in the type <T>
     * @param values list of allowed values
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomFrom(String fieldName, String... values) {
        fieldRules.put(fieldName, DiscreteRule.newSet(values).withRandom(random));
        return this;
    }

    /**
     * Declares that the field with {@code fieldName} should be assigned random boolean value.
     *
     * @param fieldName name of the field in the type <T>
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomBoolean(String fieldName) {
        fieldRules.put(fieldName, DiscreteRuleBoolean.withRandom(random));
        return this;
    }

    /**
     * Sets the builder for the property for the field with {@code fieldName}.
     *
     * @param fieldName name of the field in the type <T>
     * @param builder instance of RandomBuilder<?> for the type of the field with passed {@code fieldName}
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomWithBuilder(String fieldName, RandomBuilder<?> builder) {
        nestedObjectBuilderMap.put(fieldName, builder);
        return this;
    }

    /**
     * Sets the exclusive allowed list of String values for the field with {@code fieldName}. That means that only this
     * instance of the builder is allowed to set property with passed name {@code fieldName} with these values.
     *
     * @param fieldName name of the field in the type <T>
     * @param values list of allowed values
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> exclusiveRandomFrom(String fieldName, String... values) {
        fieldRules.put(fieldName, DiscreteRule.newSetExclusive(values).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed list of String values for the field with {@code fieldName} from which a random sub set should be
     * chosen (including empty set).
     *
     * @param fieldName name of the field in the type <T>
     * @param values list of allowed values
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomSubsetFrom(String fieldName, String... values) {
        fieldRules.put(fieldName, SubSetRule.withValues(Arrays.asList(values)).withRandom(random));
        return this;
    }

    /**
     * Sets the exclusive allowed list of String values for the field with {@code fieldName} from which a random sub set
     * should be chosen (including empty set).
     *
     * @param fieldName name of the field in the type <T>
     * @param values allowed values
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> exclusiveRandomSubsetFrom(String fieldName, String... values) {
        fieldRules.put(fieldName, SubSetRule.withValuesX(Arrays.asList(values)).withRandom(random));
        return this;
    }

    /**
     * Sets the allowed list of String values for the field with {@code fieldName} from which a random sub list should
     * be chosen (including empty list).
     *
     * @param fieldName name of the field in the type <T>
     * @param values allowed values
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomSubListFrom(String fieldName, String... values) {
        fieldRules.put(fieldName, SubListRule.withValues(Arrays.asList(values)).withRandom(random));
        return this;
    }

    /**
     * Sets the exclusive allowed list of String values for the field with {@code fieldName} from which a random sub
     * list should be chosen (including empty list).
     *
     * @param fieldName name of the field in the type <T>
     * @param values allowed values
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> exclusiveRandomSubListFrom(String fieldName, String... values) {
        fieldRules.put(fieldName, SubListRule.withValuesX(Arrays.asList(values)).withRandom(random));
        return this;
    }

    /**
     * Sets the builder for the property for the field with {@code fieldName}. The passed builder will be used to create
     * a list of size between passed {@code lower} value (inclusive) and passed {@code upper} value (exclusive).
     *
     * @param fieldName name of the field in the type <T>
     * @param builder builder for the type of the field with passed {@code fieldName}
     * @param lower lower bound of random list size
     * @param upper uppoer bound of random list size
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomSubListWithBuilder(String fieldName, RandomBuilder<?> builder, long lower,
            long upper) {
        // TODO as part of issue #37
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Declares that field with passed {@code fieldName} is UUID string.
     *
     * @param fieldName name of the field in the type <T>
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> randomUUID(String fieldName) {
        fieldRules.put(fieldName, new UUIDRule());
        return this;
    }

    /**
     * Sets the number of entities of type {@code <T>} to be built.
     *
     * @param numberOfObjects number of objects to be built
     * @return RandomBuilder<T>
     */
    public RandomBuilder<T> toBeBuilt(int numberOfObjects) {
        this.numberOfObjects = numberOfObjects;
        return this;
    }

    /**
     * Builds passed {@code numberOfObjects} of entities of type {@code <T>}.
     *
     * @param numberOfObjects number of entities to be built
     * @return List<T>
     */
    public List<T> build(long numberOfObjects) {
        List<T> result = new ArrayList<>();
        for (long i = 1; i <= numberOfObjects; i++) {
            final T randomEntity = buildOne();
            result.add(randomEntity);
        }

        return result;
    }

    private T buildOne() {
        final T instance = initObject();

        fieldRules.keySet().forEach(key -> set(instance, key, fieldRules.get(key).getRandomAllowedValue()));

        nestedObjectBuilderMap.keySet().forEach(key -> set(instance, key, nestedObjectBuilderMap.get(key).buildOne()));

        return instance;
    }

    private T initObject() {
        T instance;
        try {
            instance = objectType.newInstance();
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds set {@code numberOfObjects} of entities of type {@code <T>}.
     *
     * @param numberOfEntitiesToBuild
     * @return List<T>
     */
    public List<T> buildAll() {
        return this.build(numberOfObjects);
    }

    public Map<String, Rule<?>> getFieldRules() {
        return fieldRules;
    }

    public Map<String, RandomBuilder<?>> getNestedObjectBuilderMap() {
        return nestedObjectBuilderMap;
    }

    public void setNestedObjectBuilderMap(Map<String, RandomBuilder<?>> nestedObjectBuilderMap) {
        this.nestedObjectBuilderMap = nestedObjectBuilderMap;
    }

    /**
     * Sets the passed value {@code fieldValue} to field {@code fieldName} on the object {@code object}.
     *
     * @param object which field should be set with value
     * @param fieldName name of the field that should be set with value
     * @param fieldValue the value which should be set to field with {@code fieldName}
     * @return true if value is successfully set. Otherwise, false.
     */
    public static boolean set(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);

                field.setAccessible(true);
                field.set(object, fieldValue);
                return true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("Unexisting field: " + fieldName);
    }

    private static <C extends Comparable<C>> void checkRangeInput(C... markers) {
        List<C> markersList = new LinkedList<>(Arrays.asList(markers));
        if (markersList.size() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Invalid ranges definition. Ranges must be defined with even number of elements.");
        }
        C firstElement = markersList.remove(0);
        for (C c : markersList) {
            if (c.compareTo(firstElement) <= 0) {
                throw new IllegalArgumentException(
                        "Invalid range bounds. Range definition must be stricly increasing.");
            }
            firstElement = c;
        }
    }

}
