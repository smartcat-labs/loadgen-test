package io.smartcat.ranger.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

import io.smartcat.ranger.core.CircularRangeValueDouble;
import io.smartcat.ranger.core.CircularRangeValueLong;
import io.smartcat.ranger.core.CircularValue;
import io.smartcat.ranger.core.DiscreteValue;
import io.smartcat.ranger.core.ExactWeightedValue;
import io.smartcat.ranger.core.ExactWeightedValue.CountValuePair;
import io.smartcat.ranger.core.JsonTransformer;
import io.smartcat.ranger.core.ListValue;
import io.smartcat.ranger.core.NowDateValue;
import io.smartcat.ranger.core.NowLocalDateTimeValue;
import io.smartcat.ranger.core.NowLocalDateValue;
import io.smartcat.ranger.core.NowValue;
import io.smartcat.ranger.core.NullValue;
import io.smartcat.ranger.core.PrimitiveValue;
import io.smartcat.ranger.core.RandomLengthStringValue;
import io.smartcat.ranger.core.Range;
import io.smartcat.ranger.core.RangeValueDouble;
import io.smartcat.ranger.core.RangeValueLong;
import io.smartcat.ranger.core.StringTransformer;
import io.smartcat.ranger.core.TimeFormatTransformer;
import io.smartcat.ranger.core.UUIDValue;
import io.smartcat.ranger.core.Value;
import io.smartcat.ranger.core.ValueProxy;
import io.smartcat.ranger.core.WeightedValue;
import io.smartcat.ranger.core.WeightedValue.WeightedValuePair;
import io.smartcat.ranger.distribution.Distribution;
import io.smartcat.ranger.distribution.NormalDistribution;
import io.smartcat.ranger.distribution.UniformDistribution;

/**
 * Parser for configuration value expressions.
 */
public class ValueExpressionParser extends BaseParser<Object> {

    private static final String STRING_VALUE_DELIMITER = "stringValueDelimiter";

    private final Map<String, ValueProxy<?>> proxyValues;

    private String parentName;

    /**
     * Constructs parser with initial <code>proxyValues</code>.
     *
     * @param proxyValues Map containing proxy values by name.
     */
    public ValueExpressionParser(Map<String, ValueProxy<?>> proxyValues) {
        this.proxyValues = proxyValues;
    }

    /**
     * Sets parent name.
     *
     * @param parentName Parent name.
     */
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     * Whitespace definition.
     *
     * @return Whitespace definition rule.
     */
    public Rule whitespace() {
        return AnyOf(" \t");
    }

    /**
     * Newline definition.
     *
     * @return Newline definition rule.
     */
    public Rule newline() {
        return AnyOf("\r\n");
    }

    /**
     * Comma definition.
     *
     * @return Comma definition rule.
     */
    public Rule comma() {
        return Sequence(ZeroOrMore(whitespace()), ",", ZeroOrMore(whitespace()));
    }

    /**
     * Open parenthesis definition.
     *
     * @return Open parenthesis definition rule.
     */
    public Rule openParenthesis() {
        return Sequence(ZeroOrMore(whitespace()), "(", ZeroOrMore(whitespace()));
    }

    /**
     * Closed parenthesis definition.
     *
     * @return Closed parenthesis definition rule.
     */
    public Rule closedParenthesis() {
        return Sequence(ZeroOrMore(whitespace()), ")", ZeroOrMore(whitespace()));
    }

    /**
     * Open bracket definition.
     *
     * @return Open bracket definition rule.
     */
    public Rule openBracket() {
        return Sequence(ZeroOrMore(whitespace()), "[", ZeroOrMore(whitespace()));
    }

    /**
     * Closed bracket definition.
     *
     * @return Closed bracket definition rule.
     */
    public Rule closedBracket() {
        return Sequence(ZeroOrMore(whitespace()), "]", ZeroOrMore(whitespace()));
    }

    /**
     * Sign definition.
     *
     * @return Sign definition rule.
     */
    public Rule sign() {
        return AnyOf("+-");
    }

    /**
     * Letter definition.
     *
     * @return Letter definition rule.
     */
    public Rule letter() {
        return FirstOf('_', CharRange('a', 'z'), CharRange('A', 'Z'));
    }

    /**
     * Digit definition.
     *
     * @return Digit definition rule.
     */
    public Rule digit() {
        return CharRange('0', '9');
    }

    /**
     * Letter or digit definition.
     *
     * @return Letter or digit definition rule.
     */
    public Rule letterOrDigit() {
        return FirstOf(letter(), digit());
    }

    /**
     * Escape sequence definition.
     *
     * @return Escape sequence definition rule.
     */
    public Rule escape() {
        return Sequence('\\', AnyOf("btnfr\"\'\\"));
    }

    /**
     * Unsigned integer definition.
     *
     * @return Unsigned integer definition rule.
     */
    public Rule unsignedIntegerLiteral() {
        return OneOrMore(digit());
    }

    /**
     * Exponent definition.
     *
     * @return Exponent definition rule.
     */
    public Rule exponent() {
        return Sequence(AnyOf("eE"), Optional(sign()), unsignedIntegerLiteral());
    }

    /**
     * Null value definition.
     *
     * @return Null value definition rule.
     */
    public Rule nullValue() {
        return Sequence("null", openParenthesis(), closedParenthesis(), push(new NullValue()));
    }

    /**
     * Integer definition.
     *
     * @return Integer definition rule.
     */
    public Rule intLiteral() {
        return Sequence(Sequence(Optional(sign()), unsignedIntegerLiteral()), push(Integer.parseInt(match())));
    }

    /**
     * Long definition.
     *
     * @return Long definition rule.
     */
    public Rule longLiteral() {
        return Sequence(Sequence(Optional(sign()), unsignedIntegerLiteral()), push(Long.parseLong(match())));
    }

    /**
     * Long value definition.
     *
     * @return Long value definition rule.
     */
    public Rule longLiteralValue() {
        return Sequence(longLiteral(), push(PrimitiveValue.of(pop())));
    }

    /**
     * Double definition.
     *
     * @return Double definition rule.
     */
    public Rule doubleLiteral() {
        return Sequence(
                Sequence(Optional(sign()),
                        FirstOf(Sequence(unsignedIntegerLiteral(), '.', unsignedIntegerLiteral(), Optional(exponent())),
                                Sequence('.', unsignedIntegerLiteral(), Optional(exponent())))),
                push(Double.parseDouble(match())));
    }

    /**
     * Number definition.
     *
     * @return Number definition rule.
     */
    public Rule numberLiteral() {
        return Sequence(FirstOf(doubleLiteral(), longLiteral()), push(((Number) pop()).doubleValue()));
    }

    /**
     * Double value definition.
     *
     * @return Double value definition rule.
     */
    public Rule doubleLiteralValue() {
        return Sequence(doubleLiteral(), push(PrimitiveValue.of(pop())));
    }

    /**
     * Boolean literal definition.
     *
     * @return Boolean literal definition rule.
     */
    public Rule booleanLiteral() {
        return Sequence(FirstOf(FirstOf("True", "true"), FirstOf("False", "false")),
                push(Boolean.parseBoolean(match())));
    }

    /**
     * Boolean value definition.
     *
     * @return Boolean value definition rule.
     */
    public Rule booleanLiteralValue() {
        return Sequence(booleanLiteral(), push(PrimitiveValue.of(pop())));
    }

    /**
     * String definition.
     *
     * @return String definition rule.
     */
    public Rule stringLiteral() {
        return FirstOf(singleQuoteStringLiteral(), doubleQuoteStringLiteral());
    }

    /**
     * Character literal definition.
     *
     * @return Character literal definition rule.
     */
    public Rule charLiteral() {
        return Sequence(Sequence('\'', FirstOf(escape(), Sequence(TestNot(AnyOf("'\\")), ANY)), '\''),
                push(new Character(match().charAt(1) == '\\' ? match().charAt(2) : match().charAt(1))));
    }

    /**
     * Naked string definition.
     *
     * @return Naked string definition rule.
     */
    public Rule nakedStringLiteral() {
        return Sequence(TestNot(AnyOf("\r\n\"'\\")), ZeroOrMore(ANY), push(match()));
    }

    /**
     * Single quote string definition.
     *
     * @return Single quote string definition rule.
     */
    public Rule singleQuoteStringLiteral() {
        return Sequence(Sequence("'", ZeroOrMore(FirstOf(escape(), Sequence(TestNot(AnyOf("\r\n'\\")), ANY))), "'"),
                push(trimOffEnds(match())));
    }

    /**
     * Double quote string definition.
     *
     * @return Double quote string definition rule.
     */
    public Rule doubleQuoteStringLiteral() {
        return Sequence(Sequence('"', ZeroOrMore(FirstOf(escape(), Sequence(TestNot(AnyOf("\r\n\"\\")), ANY))), '"'),
                push(trimOffEnds(match())));
    }

    /**
     * String value definition.
     *
     * @return String value definition rule.
     */
    public Rule stringLiteralValue() {
        return Sequence(FirstOf(stringLiteral(), nakedStringLiteral()), push(PrimitiveValue.of(pop())));
    }

    /**
     * Literal definition.
     *
     * @return Literal definition rule.
     */
    public Rule literalValue() {
        return FirstOf(nullValue(), doubleLiteralValue(), longLiteralValue(), booleanLiteralValue(),
                stringLiteralValue());
    }

    /**
     * Identifier definition.
     *
     * @return Identifier definition rule.
     */
    public Rule identifier() {
        return Sequence(Sequence(letter(), ZeroOrMore(letterOrDigit())), push(match()));
    }

    /**
     * Identifier definition which does not push match to value stack.
     *
     * @return Identifier definition rule.
     */
    public Rule identifierWithNoPush() {
        return Sequence(letter(), ZeroOrMore(letterOrDigit()));
    }

    /**
     * Long range definition.
     *
     * @return Long range definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule longRange() {
        return Sequence(Sequence(longLiteral(), "..", longLiteral()), push(new Range((Long) pop(1), (Long) pop())));
    }

    /**
     * Double range definition.
     *
     * @return Double range definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule doubleRange() {
        return Sequence(Sequence(numberLiteral(), "..", numberLiteral()),
                push(new Range(((Number) pop(1)).doubleValue(), ((Number) pop()).doubleValue())));
    }

    /**
     * Character range definition.
     *
     * @return Character range definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule charRange() {
        return Sequence(Sequence(charLiteral(), "..", charLiteral()),
                push(new Range((Character) pop(1), (Character) pop())));
    }

    /**
     * Function definition.
     *
     * @param functionName Name of a function.
     * @return Function definition rule.
     */
    protected Rule function(String functionName) {
        return function(functionName, fromStringLiteral(""));
    }

    /**
     * Function definition.
     *
     * @param functionArgument Function argument rule.
     * @return Function definition rule.
     */
    protected Rule function(Rule functionArgument) {
        return function("", functionArgument);
    }

    /**
     * Function definition.
     *
     * @param functionName Name of a function.
     * @param functionArgument Function argument rule.
     * @return Function definition rule.
     */
    protected Rule function(String functionName, Rule functionArgument) {
        return Sequence(functionName, openParenthesis(), functionArgument, closedParenthesis());
    }

    /**
     * List of items enclosed in brackets.
     *
     * @param rule Rule of a list item.
     * @return Bracket list definition rule.
     */
    protected Rule bracketList(Rule rule) {
        return Sequence(openBracket(), list(rule), closedBracket());
    }

    /**
     * List of items.
     *
     * @param rule Rule of a list item.
     * @return List definition rule.
     */
    protected Rule list(Rule rule) {
        return Sequence(Sequence(push("args"), Optional(rule, ZeroOrMore(comma(), rule))),
                push(getItemsUpToDelimiter("args")));
    }

    /**
     * Value reference definition.
     *
     * @return Value reference definition rule.
     */
    public Rule valueReference() {
        return Sequence('$', Sequence(Sequence(identifierWithNoPush(), ZeroOrMore('.', identifierWithNoPush())),
                push(getValueProxy(match()))));
    }

    /**
     * Uniform distribution definition.
     *
     * @return Uniform distribution definition rule.
     */
    public Rule uniformDistribution() {
        return Sequence(function("uniform"), push(new UniformDistribution()));
    }

    /**
     * Normal distribution definition.
     *
     * @return Normal distribution definition rule.
     */
    public Rule normalDistribution() {
        return Sequence(function("normal", list(numberLiteral())), push(createNormalDistribution()));
    }

    /**
     * Distribution definition.
     *
     * @return Distribution definition rule.
     */
    public Rule distribution() {
        return FirstOf(uniformDistribution(), normalDistribution());
    }

    /**
     * Discrete value definition.
     *
     * @return Discrete value definition rule.
     */
    public Rule discreteValue() {
        return Sequence(function("random", Sequence(bracketList(value()), Optional(comma(), distribution()))),
                push(createDiscreteValue()));
    }

    /**
     * Double range value definition.
     *
     * @return Double range value definition rule.
     */
    public Rule rangeValueDouble() {
        return Sequence(
                function("random",
                        Sequence(doubleRange(),
                                Optional(comma(), booleanLiteral(), Optional(comma(), distribution())))),
                push(createRangeValueDouble()));
    }

    /**
     * Long range value definition.
     *
     * @return Long range value definition rule.
     */
    public Rule rangeValueLong() {
        return Sequence(
                function("random",
                        Sequence(longRange(), Optional(comma(), booleanLiteral(), Optional(comma(), distribution())))),
                push(createRangeValueLong()));
    }

    /**
     * Range value definition.
     *
     * @return Range value definition rule.
     */
    public Rule rangeValue() {
        return FirstOf(rangeValueLong(), rangeValueDouble());
    }

    /**
     * UUID value definition.
     *
     * @return UUID value definition rule.
     */
    public Rule uuidValue() {
        return Sequence(function("uuid"), push(new UUIDValue()));
    }

    /**
     * Circular value definition.
     *
     * @return Circular value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule circularValue() {
        return Sequence(function("circular", bracketList(value())), push(new CircularValue((List) pop())));
    }

    /**
     * Circular long range value definition.
     *
     * @return Circular long range value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule circularRangeValueLong() {
        return Sequence(function("circular", Sequence(longRange(), comma(), longLiteral())),
                push(new CircularRangeValueLong((Range) pop(1), (Long) pop())));
    }

    /**
     * Circular double range value definition.
     *
     * @return Circular double range value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule circularRangeValueDouble() {
        return Sequence(function("circular", Sequence(doubleRange(), comma(), doubleLiteral())),
                push(new CircularRangeValueDouble((Range) pop(1), (Double) pop())));
    }

    /**
     * Circular range value definition.
     *
     * @return Circular range value definition rule.
     */
    public Rule circularRangeValue() {
        return FirstOf(circularRangeValueLong(), circularRangeValueDouble());
    }

    /**
     * List value definition.
     *
     * @return List value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule listValue() {
        return Sequence(function("list", bracketList(value())), push(new ListValue((List) pop())));
    }

    /**
     * Weighted value pair definition.
     *
     * @return Weighted value pair definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule weightedValuePair() {
        return Sequence(function(Sequence(value(), comma(), numberLiteral())),
                push(new WeightedValuePair((Value) pop(1), (double) pop())));
    }

    /**
     * Weighted value definition.
     *
     * @return Weighted value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule weightedValue() {
        return Sequence(function("weighted", bracketList(weightedValuePair())), push(new WeightedValue((List) pop())));
    }

    /**
     * Count value pair definition.
     *
     * @return Count value pair definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule countValuePair() {
        return Sequence(function(Sequence(value(), comma(), longLiteral())),
                push(new CountValuePair((Value) pop(1), (Long) pop())));
    }

    /**
     * Exact weighted value definition.
     *
     * @return Weighted value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule exactWeightedValue() {
        return Sequence(function("exactly", bracketList(countValuePair())),
                push(new ExactWeightedValue<>((List) pop())));
    }

    /**
     * Random length value definition.
     *
     * @return Random length value definition rule.
     */
    public Rule randomLengthStringValue() {
        return Sequence(
                function("randomLengthString", Sequence(intLiteral(), Optional(comma(), bracketList(charRange())))),
                push(createRandomLengthStringValue()));
    }

    /**
     * Now definition.
     *
     * @return Now definition rule.
     */
    public Rule now() {
        return Sequence(function("now"), push(new NowValue()));
    }

    /**
     * Now date definition.
     *
     * @return Now date definition rule.
     */
    public Rule nowDate() {
        return Sequence(function("nowDate"), push(new NowDateValue()));
    }

    /**
     * Now local date definition.
     *
     * @return Now local date definition rule.
     */
    public Rule nowLocalDate() {
        return Sequence(function("nowLocalDate"), push(new NowLocalDateValue()));
    }

    /**
     * Now local date time definition.
     *
     * @return Now local date time definition rule.
     */
    public Rule nowLocalDateTime() {
        return Sequence(function("nowLocalDateTime"), push(new NowLocalDateTimeValue()));
    }

    /**
     * Generator definition.
     *
     * @return Generator definition rule.
     */
    public Rule generator() {
        return FirstOf(discreteValue(), rangeValue(), uuidValue(), circularValue(), circularRangeValue(), listValue(),
                weightedValue(), exactWeightedValue(), randomLengthStringValue(), now(), nowDate(), nowLocalDate(),
                nowLocalDateTime());
    }

    /**
     * String transformer definition.
     *
     * @return String transformer definition rule.
     */
    public Rule stringTransformer() {
        return Sequence(Sequence("string", openParenthesis(), stringLiteral(), push(STRING_VALUE_DELIMITER),
                ZeroOrMore(comma(), value()), closedParenthesis()), push(getStringValue()));
    }

    /**
     * JSON transformer definition.
     *
     * @return JSON transformer definition rule.
     */
    public Rule jsonTransformer() {
        return Sequence(function("json", valueReference()), push(new JsonTransformer((Value<?>) pop())));
    }

    /**
     * Time format transformer definition.
     *
     * @return Time format transformer definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule timeFormatTransformer() {
        return Sequence(function("time", Sequence(stringLiteral(), comma(), value())),
                push(new TimeFormatTransformer((String) pop(1), (Value) pop())));
    }

    /**
     * Transformer definition.
     *
     * @return Transformer definition rule.
     */
    public Rule transformer() {
        return FirstOf(stringTransformer(), jsonTransformer(), timeFormatTransformer());
    }

    /**
     * Value definition.
     *
     * @return Value definition rule.
     */
    public Rule value() {
        return FirstOf(valueReference(), generator(), transformer(), literalValue());
    }

    /**
     * Creates normal distribution.
     *
     * @return Instance of {@link NormalDistribution}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected NormalDistribution createNormalDistribution() {
        List<Double> args = (List) pop();
        if (args.isEmpty()) {
            return new NormalDistribution();
        }
        if (args.size() != 4) {
            throw new RuntimeException("Normal distribution must have following parameters:"
                    + " mean, standard deviation, lower bound and upper bound.");
        }
        return new NormalDistribution(args.get(0), args.get(1), args.get(2), args.get(3));
    }

    /**
     * Creates discrete value.
     *
     * @return Instance of {@link DiscreteValue}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected DiscreteValue createDiscreteValue() {
        return peek() instanceof Distribution ? new DiscreteValue((List) pop(1), (Distribution) pop())
                : new DiscreteValue((List) pop());
    }

    /**
     * Creates double range value.
     *
     * @return Instance of {@link RangeValueDouble}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected RangeValueDouble createRangeValueDouble() {
        return peek() instanceof Distribution
                ? new RangeValueDouble((Range) pop(2), (Boolean) pop(1), (Distribution) pop())
                : peek() instanceof Boolean ? new RangeValueDouble((Range) pop(1), (Boolean) pop())
                        : new RangeValueDouble((Range) pop());
    }

    /**
     * Creates long range value.
     *
     * @return Instance of {@link RangeValueLong}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected RangeValueLong createRangeValueLong() {
        return peek() instanceof Distribution
                ? new RangeValueLong((Range) pop(2), (Boolean) pop(1), (Distribution) pop())
                : peek() instanceof Boolean ? new RangeValueLong((Range) pop(1), (Boolean) pop())
                        : new RangeValueLong((Range) pop());
    }

    /**
     * Creates random length value.
     *
     * @return Instance of {@link RandomLengthStringValue}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected RandomLengthStringValue createRandomLengthStringValue() {
        return peek() instanceof List ? new RandomLengthStringValue((Integer) pop(1), (List) pop())
                : new RandomLengthStringValue((Integer) pop());
    }

    /**
     * Trims off ' and " characters from beginning and end of the string.
     *
     * @param s String to be trimmed off.
     * @return Trimmed off string.
     */
    protected String trimOffEnds(String s) {
        return s.substring(1, s.length() - 1);
    }

    /**
     * Returns or creates new value proxy for given name.
     *
     * @param name Name of the value proxy.
     * @return Proxy value.
     */
    protected Value<?> getValueProxy(String name) {
        String parent = parentName;
        while (parent != null) {
            String testName = null;
            if (parent.isEmpty()) {
                testName = name;
                parent = null;
            } else {
                testName = parent + "." + name;
                parent = stripOffLastReference(parent);
            }
            if (proxyValues.containsKey(testName)) {
                return proxyValues.get(testName);
            }
        }
        throw new InvalidReferenceNameException(name);
    }

    /**
     * Strips off the last reference from name.
     *
     * @param name Name from which to strip off the last reference.
     * @return Name with stripped off last reference.
     */
    protected String stripOffLastReference(String name) {
        if (!name.contains(".")) {
            return "";
        } else {
            return name.substring(0, name.lastIndexOf('.'));
        }
    }

    /**
     * Constructs {@link StringTransformer}.
     *
     * @return Instance of {@link StringTransformer}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Value<String> getStringValue() {
        List values = getItemsUpToDelimiter(STRING_VALUE_DELIMITER);
        String formatString = (String) pop();
        return new StringTransformer(formatString, values);
    }

    /**
     * Collects all items up to specified delimiter.
     *
     * @param delimiter Delimiter up to which to collect all the items.
     * @param <T> Type of item.
     * @return List of items up to specified delimiter.
     */
    @SuppressWarnings({ "unchecked" })
    protected <T> List<T> getItemsUpToDelimiter(String delimiter) {
        List<T> result = new ArrayList<>();
        while (true) {
            Object val = pop();
            if (val instanceof String && ((String) val).equals(delimiter)) {
                break;
            } else {
                result.add((T) val);
            }
        }
        Collections.reverse(result);
        return result;
    }
}
