/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.commons.internal.base;

import static org.apache.isis.commons.internal.base._Strings_SplitIterator.splitIterator;
import static org.apache.isis.commons.internal.base._With.mapIfPresentElse;
import static org.apache.isis.commons.internal.base._With.requires;

import java.nio.charset.Charset;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.apache.isis.commons.internal._Constants;
import org.apache.isis.commons.internal.base._Bytes.BytesOperator;

/**
 * <h1>- internal use only -</h1>
 * <p>
 * Provides String related algorithms.
 * </p><p>
 * Keep the public methods simple, these are basic building blocks for more complex composites.
 * Composites are provided as static fields.
 * </p>
 * <p>
 * <b>WARNING</b>: Do <b>NOT</b> use any of the classes provided by this package! <br/>
 * These may be changed or removed without notice!
 * </p>
 *
 * @since 2.0.0
 */
public final class _Strings {

    private _Strings() {}

    // -- CONSTANTS

    /**
     * Convenient e.g. for toArray conversions
     * (a duplicate of in {@link _Constants.emptyStringArray} )
     */
    public final static String[] emptyArray = new String[0];

    // -- BASIC PREDICATES

    /**
     * Same as {@link #isNullOrEmpty(CharSequence)}
     * @param x
     * @return true only if string is of zero length or null.
     */
    public static boolean isEmpty(@Nullable final CharSequence x){
        return x==null || x.length()==0;
    }
    /**
     * Same as {@link #isEmpty(CharSequence)}
     * @param x
     * @return true only if string is of zero length or null.
     */
    public static boolean isNullOrEmpty(@Nullable final CharSequence x){
        return x==null || x.length()==0;
    }


    /**
     *
     * @param x
     * @return inverse of isEmpty(CharSequence).
     */
    public static boolean isNotEmpty(@Nullable final CharSequence x){
        return x!=null && x.length()!=0;
    }

    // -- BASIC UNARY OPERATORS

    /**
     * Trims the input.
     * @param input
     * @return null if the {@code input} is null
     */
    public static String trim(@Nullable String input) {
        return mapIfPresentElse(input, String::trim, null);
    }

    /**
     * Converts all of the characters in {@code input} to lower case using the rules of the default locale.
     * @param input
     * @return null if {@code input} is null
     */
    public static String lower(@Nullable final String input) {
        return mapIfPresentElse(input, String::toLowerCase, null);
    }

    /**
     * Converts all of the characters in {@code input} to upper case using the rules of the default locale.
     * @param input
     * @return null if {@code input} is null
     */
    public static String upper(@Nullable final String input) {
        return mapIfPresentElse(input, String::toUpperCase, null);
    }

    /**
     * Converts the first character in {@code input} to upper case using the rules of the default locale.
     * @param input
     * @return null if {@code input} is null
     */
    public static String capitalize(@Nullable final String input) {
        if(input==null) {
            return null;
        }
        if (input.length() == 0) {
            return input;
        }
        if (input.length() == 1) {
            return input.toUpperCase();
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    // -- PREFIX/SUFFIX

    /**
     * If {@code input} does not start with {@code prefix} prepends {@code prefix} to the input.
     * @param input
     * @param prefix
     * @return null if {@code input} is null
     */
    public static String prefix(@Nullable final String input, final String prefix) {
        if(input==null) {
            return null;
        }
        requires(prefix, "prefix");
        if(input.startsWith(prefix)) {
            return input;
        }
        return prefix + input;
    }

    /**
     * If {@code input} does not end with {@code suffix} appends {@code suffix} to the input.
     * @param input
     * @param suffix
     * @return null if {@code input} is null
     */
    public static String suffix(@Nullable final String input, final String suffix) {
        if(input==null) {
            return null;
        }
        requires(suffix, "suffix");
        if(input.endsWith(suffix)) {
            return input;
        }
        return input + suffix;
    }

    // -- PADDING

    /**
     * Returns a string, of length at least minLength, consisting of string prepended with as many copies
     * of padChar as are necessary to reach that length.
     * @param str
     * @param minLength
     * @param c
     * @return
     */
    public static String padStart(@Nullable String str, int minLength, char c) {
        if(minLength==0) {
            return "";
        }
        if(minLength<0) {
            throw new IllegalArgumentException("minLength can't be negative, got: " + minLength);
        }

        final int len = str!=null ? str.length() : 0;
        if(len>=minLength) {
            return str;
        }

        final int fillCount = minLength - len;

        return _With.stringBuilder(sb->{

            for(int i=0; i<fillCount; ++i) {
                sb.append(c);
            }

            if(len>0) {
                sb.append(str);
            }

        }).toString();
    }

    /**
     * Returns a string, of length at least minLength, consisting of string appended with as many copies
     * of padChar as are necessary to reach that length.
     * @param str
     * @param padTo
     * @param c
     * @return
     */
    public static String padEnd(@Nullable String str, int minLength, char c) {
        if(minLength==0) {
            return "";
        }
        if(minLength<0) {
            throw new IllegalArgumentException("minLength can't be negative, got: " + minLength);
        }

        final int len = str!=null ? str.length() : 0;
        if(len>=minLength) {
            return str;
        }

        final int fillCount = minLength - len;

        return _With.stringBuilder(sb->{

            if(len>0) {
                sb.append(str);
            }

            for(int i=0; i<fillCount; ++i) {
                sb.append(c);
            }

        }).toString();
    }

    // -- SPLITTING

    /**
     * Splits the {@code input} into chunks separated by {@code separator},
     * then puts all chunks on the stream.
     * <p>
     * Corner cases:
     * <ul>
     * <li>{@code input} starts with {@code separator}: an empty string is the first chunk put on the stream</li>
     * <li>{@code input} ends with {@code separator}: an empty string is the last chunk put on the stream</li>
     * <li>a {@code separator} is followed by another: an empty string is put on the stream</li>
     * </ul>
     * @param input
     * @param separator non-empty string
     * @return empty stream if {@code input} is null
     * @throws {@link IllegalArgumentException} if {@code separator} is empty
     */
    public static Stream<String> splitThenStream(@Nullable final String input, final String separator) {
        if(isEmpty(separator)) {
            throw new IllegalArgumentException("a non empty separator is required");
        }
        if(isEmpty(input)) {
            return Stream.of();
        }
        if(!input.contains(separator)) {
            return Stream.of(input);
        }

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(splitIterator(input, separator), Spliterator.ORDERED),
                false); // not parallel
    }

    // -- REPLACEMENT OPERATORS

    /**
     * Condenses any whitespace to the given {@code replacement}
     *
     * @param input
     * @param replacement
     * @return null if {@code input} is null
     */
    public static String condenseWhitespaces(@Nullable final String input, final String replacement) {
        requires(replacement, "replacement");
        return mapIfPresentElse(input, __->input.replaceAll("\\s+", replacement), null);
    }

    // -- BYTE ARRAY CONVERSION

    /**
     * Encodes {@code str} into a sequence of bytes using the given {@code charset}.
     * @param str
     * @param charset
     * @return null if {@code str} is null
     */
    public final static byte[] toBytes(@Nullable final String str, Charset charset) {
        requires(charset, "charset");
        return mapIfPresentElse(str, __->str.getBytes(charset), null);
    }

    /**
     * Constructs a new String by decoding the specified array of bytes using the specified {@code charset}.
     * @param bytes
     * @param charset
     * @return null if {@code bytes} is null
     */
    public final static String ofBytes(@Nullable final byte[] bytes, Charset charset) {
        requires(charset, "charset");
        return mapIfPresentElse(bytes, __->new String(bytes, charset), null);
    }

    /**
     * Converts the {@code input} to a byte array using the specified {@code charset},
     * then applies the byte manipulation operator {@code converter},
     * then converts the (manipulated) byte array back to a string, again using the specified {@code charset}.
     * @param input
     * @param converter
     * @param charset
     * @return null if {@code input} is null
     */
    public final static String convert(@Nullable final String input, final BytesOperator converter, final Charset charset) {
        requires(converter, "converter");
        requires(charset, "charset");
        return mapIfPresentElse(input, __->ofBytes(converter.apply(toBytes(input, charset)), charset), null);
    }

    // -- UNARY OPERATOR COMPOSITION

    /**
     * Monadic StringOperator that allows composition of unary string operators.
     */
    public final static class StringOperator {

        private final UnaryOperator<String> operator;

        private StringOperator(UnaryOperator<String> operator) {
            this.operator = requires(operator, "operator");
        }

        public String apply(String input) {
            return operator.apply(input);
        }

        public StringOperator andThen(UnaryOperator<String> andThen) {
            return new StringOperator(s->andThen.apply(operator.apply(s)));
        }

    }

    /**
     * Returns a monadic StringOperator that allows composition of unary string operators
     * @return
     */
    public static StringOperator operator() {
        return new StringOperator(UnaryOperator.identity());
    }

    // -- SPECIAL COMPOSITES

    // using naming convention asXxx...

    public final static StringOperator asLowerDashed = operator()
            .andThen(_Strings::lower)
            .andThen(s->_Strings.condenseWhitespaces(s, "-"));

    public final static StringOperator asNormalized = operator()
            .andThen(s->_Strings.condenseWhitespaces(s, " "));

    public final static StringOperator asNaturalName2 = operator()
            .andThen(s->_Strings_NaturalNames.naturalName2(s, true));


    public final static String asFileNameWithExtension(final String fileName, String fileExtension) {
        requires(fileName, "fileName");
        requires(fileExtension, "fileExtension");
        return suffix(fileName, prefix(fileExtension, "."));
    }







}
