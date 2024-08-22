/*
 * Copyright (C) 2024 TeaConMC <contact@teacon.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.teacon.content_disposition;

import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The instance representing a header value of {@code Content-Disposition} header.
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ContentDisposition {
    /**
     * Inline type for {@link ContentDisposition}s
     */
    public static final String INLINE_TYPE = "inline";
    /**
     * Attachment type for {@link ContentDisposition}s
     */
    public static final String ATTACHMENT_TYPE = "attachment";

    /**
     * Construct a {@link ContentDisposition} representing the header value given by input.
     *
     * @param input header value
     * @return an instance of {@link ContentDisposition}
     * @throws IllegalArgumentException when a parse error raised
     */
    public static ContentDisposition parse(String input) {
        return new ContentDisposition(input);
    }

    /**
     * Construct an inline {@link ContentDisposition} without any parameter.
     *
     * @return an instance of {@link ContentDisposition}
     */
    public static ContentDisposition inline() {
        return new ContentDisposition(new Builder(INLINE_TYPE));
    }

    /**
     * Construct an inline {@link ContentDisposition} whose filename is specified by the path.
     * Only the last segment of the given path is used.
     *
     * @return an instance of {@link ContentDisposition}
     */
    public static ContentDisposition inline(Path path) {
        return new ContentDisposition(new Builder(INLINE_TYPE).filename(path.getFileName().toString()));
    }

    /**
     * Construct an inline {@link ContentDisposition} whose filename is specified by the path string.
     * Only the last segment of the given path string is used.
     *
     * @return an instance of {@link ContentDisposition}
     * @throws InvalidPathException if the path string cannot be converted to a {@link Path}
     */
    public static ContentDisposition inline(String path) {
        return new ContentDisposition(new Builder(INLINE_TYPE).filename(Path.of(path).getFileName().toString()));
    }

    /**
     * Construct an attachment {@link ContentDisposition} without any parameter.
     *
     * @return an instance of {@link ContentDisposition}
     */
    public static ContentDisposition attachment() {
        return new ContentDisposition(new Builder(ATTACHMENT_TYPE));
    }

    /**
     * Construct an attachment {@link ContentDisposition} whose filename is specified by the path.
     * Only the last segment of the given path is used.
     *
     * @return an instance of {@link ContentDisposition}
     */
    public static ContentDisposition attachment(Path path) {
        return new ContentDisposition(new Builder(ATTACHMENT_TYPE).filename(path.getFileName().toString()));
    }

    /**
     * Construct an attachment {@link ContentDisposition} whose filename is specified by the path string.
     * Only the last segment of the given path string is used.
     *
     * @return an instance of {@link ContentDisposition}
     * @throws InvalidPathException if the path string is unable to be converted to a {@link Path}
     */
    public static ContentDisposition attachment(String path) {
        return new ContentDisposition(new Builder(ATTACHMENT_TYPE).filename(Path.of(path).getFileName().toString()));
    }

    /**
     * Construct a builder for a {@link ContentDisposition} whose type is given.
     *
     * @param type the given type
     * @return the builder
     * @throws IllegalArgumentException if the type has any character which cannot be encoded as a header value
     */
    public static ContentDisposition.Builder type(String type) {
        return new Builder(type);
    }

    private ContentDisposition(String input) {
        var context = new Context(input);
        var state = STATE_EXPECT_TYPE;
        while (state != STATE_END) {
            state = context.read(state);
        }
        this.type = context.type;
        this.parms = context.parms.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(context.parms);
    }

    private ContentDisposition(Builder builder) {
        this.type = builder.type;
        this.parms = builder.parms.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(builder.parms);
    }

    /**
     * Return the type of the {@link ContentDisposition}.
     *
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Return whether it is an inline {@link ContentDisposition} or not, which is equivalent to
     * {@code getType().equals(INLINE_TYPE)}.
     *
     * @return the type is inline type or not
     */
    public boolean isInline() {
        return INLINE_TYPE.equals(this.type);
    }

    /**
     * Return whether it is an attachment {@link ContentDisposition} or not, which is equivalent to
     * {@code getType().equals(ATTACHMENT_TYPE)}.
     *
     * @return the type is attachment type or not
     */
    public boolean isAttachment() {
        return ATTACHMENT_TYPE.equals(this.type);
    }

    /**
     * Return the parameters of the {@link ContentDisposition}.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return this.parms;
    }

    /**
     * Return the filename represented by the {@link ContentDisposition}, may be empty.
     *
     * @return an {@link Optional} of the filename
     */
    public Optional<String> getFilename() {
        var key = "filename*0";
        var part = this.parms.getOrDefault(key + "*", this.parms.get(key));
        if (part != null) {
            var builder = new StringBuilder(part.length() * 2 + 16);
            var index = 0;
            while (true) {
                builder.append(part);
                key = "filename*" + (++index);
                part = this.parms.getOrDefault(key + "*", this.parms.get(key));
                if (part == null) {
                    return Optional.of(builder.toString());
                }
            }
        }
        key = "filename";
        return Optional.ofNullable(this.parms.getOrDefault(key + "*", this.parms.get(key)));
    }

    /**
     * Check if two {@link ContentDisposition}s are the same (the order will be checked if
     * {@code strict} is set to {@code true}). For example:
     *
     * <pre>{@code
     *     var a = parse("inline; a=foo; b=bar");
     *     var b = parse("inline; b=bar; a=foo");
     *
     *     a.equals(b, true); // false, the order must be consistent in strict mode
     *     a.equals(b, false); // true, the order can be arbitrary here
     * }</pre>
     *
     * @param that   another instance
     * @param strict strict mode
     * @return {@code true} if they are the same
     */
    public boolean equals(ContentDisposition that, boolean strict) {
        if (!this.type.equals(that.type)) {
            return false;
        }
        var parmsThis = this.parms;
        var parmsThat = that.parms;
        if (parmsThis == parmsThat) {
            return true;
        }
        if (parmsThis.size() != parmsThat.size()) {
            return false;
        }
        var itThis = parmsThis.entrySet().iterator();
        var itThat = parmsThat.entrySet().iterator();
        while (itThis.hasNext()) {
            var entry = itThis.next();
            var same = strict ? entry.equals(itThat.next()) : entry.getValue().equals(parmsThat.get(entry.getKey()));
            if (!same) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if two {@link ContentDisposition}s are the same (the order can be arbitrary).
     * This method is an overload of {@link #equals(Object)}.
     *
     * @param that another instance
     * @return {@code true} if they are the same
     * @see #equals(Object)
     * @see #equals(ContentDisposition, boolean)
     */
    public boolean equals(ContentDisposition that) {
        return this.equals(that, false);
    }

    /**
     * Check if two {@link ContentDisposition}s are the same (the order can be arbitrary).
     * There is another overload method with narrowed parameter type: {@link #equals(ContentDisposition)}.
     *
     * @param o another instance
     * @return {@code true} if they are the same
     * @see #equals(ContentDisposition)
     * @see #equals(ContentDisposition, boolean)
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ContentDisposition && this.equals((ContentDisposition) o, false);
    }

    /**
     * Calculate the hash code.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return this.type.hashCode() + this.parms.hashCode();
    }

    /**
     * Return the string representation of the instance which can be used in header values.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        var builder = new StringBuilder(this.type);
        for (var entry : this.parms.entrySet()) {
            builder.append("; ").append(entry.getKey()).append('=');
            if (builder.charAt(builder.length() - 2) == '*') {
                appendUTF8Bytes(builder, entry.getValue().getBytes(StandardCharsets.UTF_8));
            } else {
                appendQuotedBytes(builder, entry.getValue());
            }
        }
        return builder.toString();
    }

    /**
     * The builder for {@link ContentDisposition}s.
     *
     * @see ContentDisposition#type(String)
     */
    public static final class Builder {
        private final String type;
        private final Map<String, String> parms;

        private Builder(String type) {
            checkArgument(Context.TOKENS_PATTERN.matcher(type).matches(), "not a token: " + type);
            this.type = replaceUpperToLower(type, 0, type.length());
            this.parms = new LinkedHashMap<>(4);
        }

        /**
         * Add a parameter (key-value pair) to the builder.
         *
         * @param key   the parameter key
         * @param value the parameter value
         * @return the builder itself
         * @throws IllegalArgumentException if the key has already been added or the key-value pair has
         *                                  any character which cannot be encoded as a header value
         */
        public Builder parameter(String key, String value) {
            checkArgument(Context.TOKENS_PATTERN.matcher(key).matches(), "not a token: " + key);
            var canEncode = key.endsWith("*") || Context.TEXTS_PATTERN.matcher(value).matches();
            checkArgument(canEncode, "cannot encode parameter: " + key + "=" + value);
            var oldParm = this.parms.putIfAbsent(replaceUpperToLower(key, 0, key.length()), value);
            checkArgument(oldParm == null, "parameter key exists: " + key);
            return this;
        }

        /**
         * Specify the filename of the {@link ContentDisposition} instance built by the builder.
         * The {@code "filename"} parameter and the {@code "filename*"} parameter will be specified.
         *
         * @param filename the filename
         * @return the builder itself
         * @throws IllegalArgumentException if the {@code "filename"} or {@code "filename*"} key
         *                                  has already been added
         */
        public Builder filename(String filename) {
            checkArgument(!this.parms.containsKey("filename"), "parameter key exists: filename");
            checkArgument(!this.parms.containsKey("filename*"), "parameter key exists: filename*");
            this.parms.put("filename", replaceNonText(filename));
            this.parms.put("filename*", filename);
            return this;
        }

        /**
         * Build the {@link ContentDisposition} instance
         *
         * @return the instance
         */
        public ContentDisposition build() {
            return new ContentDisposition(this);
        }
    }

    private final String type;
    private final Map<String, String> parms;

    private static final int STATE_END = -1;
    private static final int STATE_EXPECT_TYPE = 0;
    private static final int STATE_EXPECT_SEMICOLON_AND_END = 1;
    private static final int STATE_EXPECT_PARM_KEY = 2;
    private static final int STATE_EXPECT_EQUALS = 3;
    private static final int STATE_EXPECT_PARM_VALUE = 4;

    private static final class Context {
        private static final Pattern WORDS_OR_SEPARATORS_PATTERN = Pattern.compile("\\G(?:(?:\\r\\n)?[\\u0020\\u0009" +
                "]+)?(?:(?<z>\\z)|(?<s>[;=])|(?<qs>\"(?:[\\u0080-\\u00FF0-9A-Za-z./:;<=>?@\\u005B\\u005D^_`{|}!#$%&'" +
                "()*+,-]|\\\\[\\u0020-\\u007E]|(?:\\r\\n)?[\\u0020\\u0009]+)*+\")|(?<t>[0-9A-Za-z.^_`|~!#$%&'*+-]+))");
        private static final Pattern EXT_VALUES_PATTERN = Pattern.compile("(?<c>[0-9A-Za-z^_`{}~!#$%&+-]*)'(?<l>[A-Z" +
                "a-z]{4,8}|[A-Za-z]{2,3}(?:-[A-Za-z]{3}){0,3})?'(?<vc>(?:[0-9A-Za-z.^_`|~!#$&+-]|%[0-9A-Fa-f]{2})++)");
        private static final Pattern TEXTS_PATTERN = Pattern.compile("[\\u0020-\\u007E\\u0080-\\u00FF]+");
        private static final Pattern TOKENS_PATTERN = Pattern.compile("[0-9A-Za-z.^_`|~!#$%&'*+-]+");

        private final Map<String, String> parms = new LinkedHashMap<>();
        private String parmKey = "", type = "";
        private final Matcher matcher;
        private final String text;
        private int prevEnd = 0;

        private Context(String text) {
            this.matcher = WORDS_OR_SEPARATORS_PATTERN.matcher(text);
            this.text = text;
        }

        private int read(int state) {
            checkArgument(this.matcher.find(), "unrecognized token after index " + this.prevEnd);
            this.prevEnd = this.matcher.end();
            switch (state) {
                case STATE_EXPECT_TYPE: {
                    var tf = this.matcher.start("t");
                    checkArgument(tf >= 0, "disposition type expected at index " + this.matcher.start());
                    this.type = replaceUpperToLower(this.text, tf, this.matcher.end("t"));
                    return STATE_EXPECT_SEMICOLON_AND_END;
                }
                case STATE_EXPECT_SEMICOLON_AND_END: {
                    var zf = this.matcher.start("z");
                    if (zf >= 0) {
                        return STATE_END;
                    }
                    var sf = this.matcher.start("s");
                    var isSemicolon = sf >= 0 && this.text.charAt(sf) == ';';
                    checkArgument(isSemicolon, "semicolon expected at index " + this.matcher.start());
                    return STATE_EXPECT_PARM_KEY;
                }
                case STATE_EXPECT_PARM_KEY: {
                    var tf = this.matcher.start("t");
                    checkArgument(tf >= 0, "parameter expected at index " + this.matcher.start());
                    this.parmKey = replaceUpperToLower(this.text, tf, this.matcher.end("t"));
                    return STATE_EXPECT_EQUALS;
                }
                case STATE_EXPECT_EQUALS: {
                    var sf = this.matcher.start("s");
                    var isEquals = sf >= 0 && this.text.charAt(sf) == '=';
                    checkArgument(isEquals, "equals symbol expected at index " + this.matcher.start());
                    return STATE_EXPECT_PARM_VALUE;
                }
                case STATE_EXPECT_PARM_VALUE: {
                    var tf = this.matcher.start("t");
                    if (tf >= 0) {
                        var t = this.matcher.group("t");
                        if (!this.parmKey.endsWith("*")) {
                            this.parms.putIfAbsent(this.parmKey, t);
                            return STATE_EXPECT_SEMICOLON_AND_END;
                        }
                        var matcher = EXT_VALUES_PATTERN.matcher(t);
                        if (matcher.matches()) {
                            var c = matcher.group("c");
                            var ev = unwrapExtValue(c, t, matcher.start("vc"), matcher.end("vc"));
                            this.parms.putIfAbsent(this.parmKey, ev);
                            return STATE_EXPECT_SEMICOLON_AND_END;
                        }
                    }
                    var qsf = this.matcher.start("qs");
                    var isNotEncoded = qsf >= 0 && !this.parmKey.endsWith("*");
                    checkArgument(isNotEncoded, "parameter value expected at index " + this.matcher.start());
                    var qv = unwrapQuoted(this.text, qsf, this.matcher.end("qs"));
                    this.parms.putIfAbsent(this.parmKey, qv);
                    return STATE_EXPECT_SEMICOLON_AND_END;
                }
                default: {
                    throw new IllegalStateException("unreachable");
                }
            }
        }
    }

    private static String unwrapExtValue(String charset, String input, int valueCharsFrom, int valueCharsTo) {
        var charsetObject = charset.isEmpty() ? StandardCharsets.ISO_8859_1 : Charset.forName(charset);
        var checkNonLatin1Bytes = StandardCharsets.ISO_8859_1.equals(charsetObject);
        var buffer = ByteBuffer.allocate(valueCharsTo - valueCharsFrom);
        var builder = new StringBuilder(valueCharsTo - valueCharsFrom);
        for (var i = valueCharsFrom; i < valueCharsTo; ++i) {
            var valueChar = input.charAt(i);
            if (valueChar == '%') {
                var valueHex1 = Character.digit(input.charAt(++i), 16);
                var valueHex2 = Character.digit(input.charAt(++i), 16);
                valueChar = (char) (valueHex1 * 16 + valueHex2);
            }
            if (checkNonLatin1Bytes && Character.isISOControl(valueChar)) {
                // the standard does not accept iso control characters for iso-8859-1 charset
                builder.append(new String(buffer.array(), 0, buffer.position(), charsetObject));
                builder.append('\uFFFD');
                buffer.rewind();
                continue;
            }
            buffer.put((byte) valueChar);
        }
        return builder.append(new String(buffer.array(), 0, buffer.position(), charsetObject)).toString();
    }

    private static String unwrapQuoted(String input, int quotedStringFrom, int quotedStringTo) {
        var builder = new StringBuilder(quotedStringTo - quotedStringFrom);
        for (var i = quotedStringFrom + 1; i < quotedStringTo - 1; ++i) {
            var quotedChar = input.charAt(i);
            if (quotedChar == '\r') {
                var lwsIndex = i + 1;
                if (input.charAt(lwsIndex) == '\n') {
                    quotedChar = input.charAt(++lwsIndex);
                    while (quotedChar == ' ' || quotedChar == '\t') {
                        quotedChar = input.charAt(++lwsIndex);
                    }
                    builder.append(' ');
                    i = lwsIndex - 1;
                }
            } else {
                builder.append(quotedChar == '\\' ? input.charAt(++i) : input.charAt(i));
            }
        }
        return builder.toString();
    }

    private static void appendUTF8Bytes(StringBuilder builder, byte[] valueChars) {
        builder.append("UTF-8''");
        for (byte valueChar : valueChars) {
            if (valueChar >= 0x20 && valueChar <= 0x7E) {
                builder.append((char) valueChar);
            } else {
                var valueHexChar1 = Character.forDigit((valueChar >> 4) & 0xF, 16);
                var valueHexChar2 = Character.forDigit(valueChar & 0xF, 16);
                builder.append('%').append(valueHexChar1).append(valueHexChar2);
            }
        }
    }

    private static void appendQuotedBytes(StringBuilder builder, String unquotedChars) {
        var count = unquotedChars.length();
        builder.append('"');
        for (var i = 0; i < count; ++i) {
            var unquotedChar = unquotedChars.charAt(i);
            if (unquotedChar == '"' || unquotedChar == '\\') {
                builder.append('\\');
            }
            builder.append(unquotedChar);
        }
        builder.append('"');
    }

    private static String replaceUpperToLower(String input, int from, int to) {
        var builder = new StringBuilder(to - from);
        for (var i = from; i < to; ++i) {
            var inputChar = input.charAt(i);
            if (inputChar >= 'A' && inputChar <= 'Z') {
                builder.append((char) (inputChar + 'a' - 'A'));
            } else {
                builder.append(inputChar);
            }
        }
        return builder.toString();
    }

    private static String replaceNonText(String filename) {
        var matcher = Context.TEXTS_PATTERN.matcher(filename);
        var size = filename.length();
        if (matcher.find()) {
            var end = matcher.end();
            var start = matcher.start();
            if (end == size && start == 0) {
                return filename;
            }
            var builder = new StringBuilder(size);
            builder.append("?".repeat(start)).append(filename, start, end);
            while (matcher.find()) {
                start = matcher.start();
                builder.append("?".repeat(start - end));
                end = matcher.end();
                builder.append(filename, start, end);
            }
            return builder.append("?".repeat(size - end)).toString();
        }
        return "?".repeat(size);
    }

    private static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
