package io.smartcat.ranger.core.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.smartcat.ranger.core.*;
import org.parboiled.BaseParser;
import org.parboiled.Rule;

/**
 * Parser for configuration value expressions.
 */
public class ValueExpressionParser extends BaseParser<Object> {

    private static final String DISCRETE_VALUE_DELIMITER = "discreteValueDelimiter";
    private static final String TO_STRING_VALUE_DELIMITER = "toStringVauleDelimiter";

    private Map<String, ValueProxy<?>> proxyValues;

    /**
     * Sets map to be used for proxy values.
     *
     * @param proxyValues Proxy value map.
     */
    public void setProxyValues(Map<String, ValueProxy<?>> proxyValues) {
        if (proxyValues == null) {
            throw new IllegalArgumentException("proxyValues cannot be null.");
        }
        this.proxyValues = proxyValues;
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
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'));
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
        return Sequence(Sequence("null", FirstOf(whitespace(), newline())), push(new NullValue()));
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
        return Sequence(longLiteral(), push(PrimitiveValue.of((Long) pop())));
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
     * Double value definition.
     *
     * @return Double value definition rule.
     */
    public Rule doubleLiteralValue() {
        return Sequence(doubleLiteral(), push(PrimitiveValue.of((Double) pop())));
    }

    /**
     * Boolean value definition.
     *
     * @return Boolean value definition rule.
     */
    public Rule booleanLiteralValue() {
        return Sequence(FirstOf(FirstOf("True", "true"), FirstOf("False", "false")),
                push(PrimitiveValue.of(Boolean.parseBoolean(match()))));
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
     * Naked string definition.
     *
     * @return Naked string definition rule.
     */
    public Rule nakedStringLiteral() {
        return Sequence(ZeroOrMore(TestNot(AnyOf("\r\n\"'\\")), ANY), push(match()));
    }

    /**
     * Single quote string definition.
     *
     * @return Single quote string definition rule.
     */
    public Rule singleQuoteStringLiteral() {
        return Sequence(Sequence("'", ZeroOrMore(TestNot(AnyOf("\r\n'\\")), ANY), "'"), push(trimOffEnds(match())));
    }

    /**
     * Double quote string definition.
     *
     * @return Double quote string definition rule.
     */
    public Rule doubleQuoteStringLiteral() {
        return Sequence(Sequence('"', ZeroOrMore(TestNot(AnyOf("\r\n\"\\")), ANY), '"'), push(trimOffEnds(match())));
    }

    /**
     * String value definition.
     *
     * @return String value definition rule.
     */
    public Rule stringLiteralValue() {
        return Sequence(FirstOf(stringLiteral(), nakedStringLiteral()), push(PrimitiveValue.of((String) pop())));
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
     * Value reference definition.
     *
     * @return Value reference definition rule.
     */
    public Rule valueReference() {
        return Sequence('$', identifier(), push(getValueProxy((String) pop())));
    }

    /**
     * Discrete value definition.
     *
     * @return Discrete value definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule discreteValue() {
        return Sequence(
                Sequence("random([", push(DISCRETE_VALUE_DELIMITER), value(), ZeroOrMore(comma(), value()), "])"),
                push(new DiscreteValue(getDiscreteValues())));
    }

    /**
     * Double range value definition.
     *
     * @return Double range value definition rule.
     */
    public Rule rangeValueDouble() {
        return Sequence(Sequence("random(", doubleLiteral(), "..", doubleLiteral(), ")"),
                push(new RangeValueDouble((Double) pop(1), (Double) pop())));
    }

    /**
     * Long range value definition.
     *
     * @return Long range value definition rule.
     */
    public Rule rangeValueLong() {
        return Sequence(Sequence("random(", longLiteral(), "..", longLiteral(), ")"),
                push(new RangeValueLong((Long) pop(1), (Long) pop())));
    }

    /**
     * Range value definition.
     *
     * @return Range value definition rule.
     */
    public Rule rangeValue() {
        return FirstOf(rangeValueDouble(), rangeValueLong());
    }

    /**
     * UUID value definition.
     *
     * @return UUID value definition rule.
     */
    public Rule uuidValue() {
        return Sequence(fromStringLiteral("uuid()"), push(new UUIDValue()));
    }

    /**
     * Generator definition.
     *
     * @return Generator definition rule.
     */
    public Rule generator() {
        return FirstOf(discreteValue(), rangeValue(), uuidValue());
    }

    /**
     * String transformer definition.
     *
     * @return String transformer definition rule.
     */
    public Rule stringTransformer() {
        return Sequence(Sequence("toString(", stringLiteral(), push(TO_STRING_VALUE_DELIMITER),
                ZeroOrMore(comma(), value()), ")"), push(getToStringValue()));
    }

    /**
     * JSON transformer definition.
     *
     * @return JSON transformer definition rule.
     */
    public Rule jsonTransformer() {
        return Sequence("toJSON(", valueReference(), ")", push(new JsonTransformer((Value<?>) pop())));
    }

    /**
     * Time format transformer definition.
     *
     * @return Time format transformer definition rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Rule timeFormatTransformer() {
        return Sequence(Sequence("toTime(", stringLiteral(), comma(), value(), ")"),
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
        if (proxyValues.containsKey(name)) {
            return proxyValues.get(name);
        } else {
            ValueProxy<?> proxyValue = new ValueProxy<>();
            proxyValues.put(name, proxyValue);
            return proxyValue;
        }
    }

    /**
     * Collects all discrete values.
     *
     * @return List of discrete values.
     */
    @SuppressWarnings({ "rawtypes" })
    protected List getDiscreteValues() {
        return getValuesUpToDelimiter(DISCRETE_VALUE_DELIMITER);
    }

    /**
     * Constructs {@link StringTransformer}.
     *
     * @return Instance of {@link StringTransformer}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Value<String> getToStringValue() {
        List values = getValuesUpToDelimiter(TO_STRING_VALUE_DELIMITER);
        String formatString = (String) pop();
        return new StringTransformer(formatString, values);
    }

    /**
     * Collects all values up to specified delimiter.
     *
     * @param delimiter Delimiter up to which to collect all the values.
     * @param <T> Type value would evaluate to.
     * @return List of values up to specified delimiter.
     */
    @SuppressWarnings({ "unchecked" })
    protected <T> List<Value<T>> getValuesUpToDelimiter(String delimiter) {
        List<Value<T>> result = new ArrayList<>();
        while (true) {
            Object val = pop();
            if (val instanceof String && ((String) val).equals(delimiter)) {
                break;
            } else {
                result.add((Value<T>) val);
            }
        }
        Collections.reverse(result);
        return result;
    }
}
