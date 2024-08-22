package org.teacon.content_disposition;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.teacon.content_disposition.ContentDisposition.*;

class ContentDispositionTest {
    @Test
    void testInline() {
        assertEquals(INLINE_TYPE, inline().getType());
        assertTrue(inline().isInline());
        assertFalse(inline().isAttachment());
        assertEquals(Map.of(), inline().getParameters());
        assertEquals(empty(), inline().getFilename());
        assertEquals(inline(), parse("inline"));
        assertThrows(IllegalArgumentException.class, () -> parse("\"inline\""));
        assertEquals(type(INLINE_TYPE).parameter("filename", "foo.html").build(), parse("inline; filename=\"foo.html\""));
        assertEquals(type(INLINE_TYPE).parameter("filename", "Not an attachment!").build(), parse("inline; filename=\"Not an attachment!\""));
        assertEquals(type(INLINE_TYPE).parameter("filename", "foo.pdf").build(), parse("inline; filename=\"foo.pdf\""));
    }

    @Test
    void testAttachment() {
        assertEquals(ATTACHMENT_TYPE, attachment().getType());
        assertFalse(attachment().isInline());
        assertTrue(attachment().isAttachment());
        assertEquals(Map.of(), attachment().getParameters());
        assertEquals(empty(), attachment().getFilename());
        assertEquals(attachment(), parse("attachment"));
        assertThrows(IllegalArgumentException.class, () -> parse("\"attachment\""));
        assertEquals(attachment(), parse("ATTACHMENT"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo.html").build(), parse("attachment; filename=\"foo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "0000000000111111111122222").build(), parse("attachment; filename=\"0000000000111111111122222\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "00000000001111111111222222222233333").build(), parse("attachment; filename=\"00000000001111111111222222222233333\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo.html").build(), parse("attachment; filename=\"f\\oo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "\"quoting\" tested.html").build(), parse("attachment; filename=\"\\\"quoting\\\" tested.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "Here's a semicolon;.html").build(), parse("attachment; filename=\"Here's a semicolon;.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar").parameter("filename", "foo.html").build(), parse("attachment; foo=\"bar\"; filename=\"foo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "\"\\").parameter("filename", "foo.html").build(), parse("attachment; foo=\"\\\"\\\\\";filename=\"foo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo.html").build(), parse("attachment; FILENAME=\"foo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo.html").build(), parse("attachment; filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo,bar.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo.html ;"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; ;filename=foo"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo bar.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "'foo.bar'").build(), parse("attachment; filename='foo.bar'"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo-ä.html").build(), parse("attachment; filename=\"foo-ä.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo-Ã¤.html").build(), parse("attachment; filename=\"foo-Ã¤.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo-%41.html").build(), parse("attachment; filename=\"foo-%41.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "50%.html").build(), parse("attachment; filename=\"50%.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo-%41.html").build(), parse("attachment; filename=\"foo-%\\41.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("name", "foo-%41.html").build(), parse("attachment; name=\"foo-%41.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "ä-%41.html").build(), parse("attachment; filename=\"ä-%41.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo-%c3%a4-%e2%82%ac.html").build(), parse("attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo.html").build(), parse("attachment; filename =\"foo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo.html").build(), parse("attachment; filename=\"foo.html\"; filename=\"bar.html\""));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo[1](2).html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo-ä.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo-Ã¤.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("x=y; filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("\"foo; filename=bar;baz\"; filename=qux"));
        assertThrows(IllegalArgumentException.class, () -> parse("filename=foo.html, filename=bar.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("; filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse(": inline; attachment; filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("inline; attachment; filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; inline; filename=foo.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=\"foo.html\".txt"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=\"bar"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo\"bar;baz\"qux"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=foo.html, attachment; filename=bar.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; foo=foo filename=bar"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename=bar foo=foo"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment filename=bar"));
        assertThrows(IllegalArgumentException.class, () -> parse("filename=foo.html; attachment"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("xfilename", "foo.html").build(), parse("attachment; xfilename=foo.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "/foo.html").build(), parse("attachment; filename=\"/foo.html\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "\\foo.html").build(), parse("attachment; filename=\"\\\\foo.html\""));
    }

    @Test
    void testAdditionalParameters() {
        assertEquals(type(ATTACHMENT_TYPE).parameter("creation-date", "Wed, 12 Feb 1997 16:29:51 -0500").build(), parse("attachment; creation-date=\"Wed, 12 Feb 1997 16:29:51 -0500\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("modification-date", "Wed, 12 Feb 1997 16:29:51 -0500").build(), parse("attachment; modification-date=\"Wed, 12 Feb 1997 16:29:51 -0500\""));
    }

    @Test
    void testDispositionTypeExtension() {
        assertEquals(type("foobar").build(), parse("foobar"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("example", "filename=example.txt").build(), parse("attachment; example=\"filename=example.txt\""));
    }

    @Test
    void testCharacterSets() {
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-ä.html").build(), parse("attachment; filename*=iso-8859-1''foo-%E4.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-ä-€.html").build(), parse("attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-Ã¤-â\uFFFD¬.html").build(), parse("attachment; filename*=''foo-%c3%a4-%e2%82%ac.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-ä.html").build(), parse("attachment; filename*=UTF-8''foo-a%cc%88.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-Ã¤-â\uFFFD¬.html").build(), parse("attachment; filename*=iso-8859-1''foo-%c3%a4-%e2%82%ac.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-\uFFFD.html").build(), parse("attachment; filename*=utf-8''foo-%E4.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename *=UTF-8''foo-%c3%a4.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-ä.html").build(), parse("attachment; filename*= UTF-8''foo-%c3%a4.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-ä.html").build(), parse("attachment; filename* =UTF-8''foo-%c3%a4.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename*=\"UTF-8''foo-%c3%a4.html\""));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename*=\"foo%20bar.html\""));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename*=UTF-8'foo-%c3%a4.html"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename*=UTF-8''foo%"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; filename*=UTF-8''f%oo.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "A-%41.html").build(), parse("attachment; filename*=UTF-8''A-%2541.html"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "\\foo.html").build(), parse("attachment; filename*=UTF-8''%5cfoo.html"));
    }

    @Test
    void testContinuations() {
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*0", "foo.").parameter("filename*1", "html").build(), parse("attachment; filename*0=\"foo.\"; filename*1=\"html\""));
        assertEquals(of("foo.html"), parse("attachment; filename*0=\"foo.\"; filename*1=\"html\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*0", "foo").parameter("filename*1", "bar.html").build(), parse("attachment; filename*0=\"foo\"; filename*1=\"\\b\\a\\r.html\""));
        assertEquals(of("foobar.html"), parse("attachment; filename*0=\"foo\"; filename*1=\"\\b\\a\\r.html\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*0*", "foo-ä").parameter("filename*1", ".html").build(), parse("attachment; filename*0*=UTF-8''foo-%c3%a4; filename*1=\".html\""));
        assertEquals(of("foo-ä.html"), parse("attachment; filename*0*=UTF-8''foo-%c3%a4; filename*1=\".html\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*0", "foo").parameter("filename*01", "bar").build(), parse("attachment; filename*0=\"foo\"; filename*01=\"bar\""));
        assertEquals(of("foo"), parse("attachment; filename*0=\"foo\"; filename*01=\"bar\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*0", "foo").parameter("filename*2", "bar").build(), parse("attachment; filename*0=\"foo\"; filename*2=\"bar\""));
        assertEquals(of("foo"), parse("attachment; filename*0=\"foo\"; filename*2=\"bar\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*1", "foo.").parameter("filename*2", "html").build(), parse("attachment; filename*1=\"foo.\"; filename*2=\"html\""));
        assertEquals(empty(), parse("attachment; filename*1=\"foo.\"; filename*2=\"html\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*1", "bar").parameter("filename*0", "foo").build(), parse("attachment; filename*1=\"bar\"; filename*0=\"foo\""));
        assertEquals(of("foobar"), parse("attachment; filename*1=\"bar\"; filename*0=\"foo\"").getFilename());
    }

    @Test
    void testFallbackBehavior() {
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "foo-ae.html").parameter("filename*", "foo-ä.html").build(), parse("attachment; filename=\"foo-ae.html\"; filename*=UTF-8''foo-%c3%a4.html"));
        assertEquals(of("foo-ä.html"), parse("attachment; filename=\"foo-ae.html\"; filename*=UTF-8''foo-%c3%a4.html").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*", "foo-ä.html").parameter("filename", "foo-ae.html").build(), parse("attachment; filename*=UTF-8''foo-%c3%a4.html; filename=\"foo-ae.html\""));
        assertEquals(of("foo-ä.html"), parse("attachment; filename*=UTF-8''foo-%c3%a4.html; filename=\"foo-ae.html\"").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename*0*", "euro-sign=€").parameter("filename*", "currency-sign=¤").build(), parse("attachment; filename*0*=ISO-8859-15''euro-sign%3d%a4; filename*=ISO-8859-1''currency-sign%3d%a4"));
        assertEquals(of("euro-sign=€"), parse("attachment; filename*0*=ISO-8859-15''euro-sign%3d%a4; filename*=ISO-8859-1''currency-sign%3d%a4").getFilename());
        assertEquals(type(ATTACHMENT_TYPE).parameter("foobar", "x").parameter("filename", "foo.html").build(), parse("attachment; foobar=x; filename=\"foo.html\""));
        assertEquals(of("foo.html"), parse("attachment; foobar=x; filename=\"foo.html\"").getFilename());
    }

    @Test
    void testRFC2047() {
        assertEquals(type(ATTACHMENT_TYPE).parameter("filename", "=?ISO-8859-1?Q?foo-=E4.html?=").build(), parse("attachment; filename=\"=?ISO-8859-1?Q?foo-=E4.html?=\""));
    }

    @Test
    void testLineBreak() {
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar").build(), parse("attachment;\r\n\tfoo=bar"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar").build(), parse("attachment; foo=\r\n\tbar"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar").build(), parse("attachment; foo=\r\n\tbar"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar").build(), parse("attachment; foo=bar\r\n\t"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; foo=bar\r\nbaz"));
        assertThrows(IllegalArgumentException.class, () -> parse("attachment; foo=bar\r\n\tbaz"));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar baz").build(), parse("attachment; foo=\"bar\r\n\tbaz\""));
        assertEquals(type(ATTACHMENT_TYPE).parameter("foo", "bar baz foo").build(), parse("attachment;\r\n\tfoo=\"bar\r\n baz\r\n \t \t \t foo\""));
    }

    @Test
    void testEquals() {
        assertTrue(Set.of(parse("inline"), parse("attachment")).contains(inline()));
        assertTrue(Set.of(parse("inline"), parse("attachment")).contains(attachment()));
        assertEquals(parse("attachment; foo=bar; baz=foo"), parse("attachment; baz=foo; foo=bar"));
        assertNotEquals(parse("attachment; foo=bar"), parse("attachment; foo=bar; baz=foo"));
        assertNotEquals(parse("inline; foo=bar; baz=foo"), parse("attachment; foo=bar; baz=foo"));
        assertNotEquals(parse("attachment; bar=foo; baz=foo"), parse("attachment; foo=bar; baz=foo"));
        assertTrue(parse("attachment; foo=bar; baz=foo").equals(parse("attachment; baz=foo; foo=bar")));
        assertFalse(parse("attachment; foo=bar; baz=foo").equals(parse("attachment; baz=foo; foo=bar"), true));
        assertTrue(parse("attachment; foo=bar; baz=foo").equals(parse("attachment; baz=foo; foo=bar"), false));
    }

    @Test
    void testStringify() {
        assertEquals("inline", inline().toString());
        assertEquals("attachment", attachment().toString());
        assertEquals("attachment; foo=\"bar\"", type(ATTACHMENT_TYPE).parameter("foo", "bar").build().toString());
        assertEquals("attachment; foo=\"\\\"\\\"\"", type(ATTACHMENT_TYPE).parameter("foo", "\"\"").build().toString());
        assertEquals("attachment; foo*=UTF-8''bar", type(ATTACHMENT_TYPE).parameter("foo*", "bar").build().toString());
        assertEquals("attachment; foo*=UTF-8''bar-%c3%a4", type(ATTACHMENT_TYPE).parameter("foo*", "bar-ä").build().toString());
        assertEquals("inline; filename=\"foo\"; filename*=UTF-8''foo", inline("foo").toString());
        assertEquals("inline; filename=\"foo-ä\"; filename*=UTF-8''foo-%c3%a4", inline("foo-ä").toString());
        assertEquals("inline; filename=\"bar-ä\"; filename*=UTF-8''bar-%c3%a4", inline("foo/bar-ä").toString());
        assertEquals("inline; filename=\"baz-ä\"; filename*=UTF-8''baz-%c3%a4", inline(Path.of("foo", "bar", "baz-ä")).toString());
        assertEquals("attachment; filename=\"???\"; filename*=UTF-8''%e2%82%ac%e2%82%ac%e2%82%ac", attachment("€€€").toString());
        assertEquals("attachment; filename=\"foo-?-ä\"; filename*=UTF-8''foo-%e2%82%ac-%c3%a4", attachment("foo-€-ä").toString());
        assertEquals("attachment; filename=\"bar-?-ä\"; filename*=UTF-8''bar-%e2%82%ac-%c3%a4", attachment("foo/bar-€-ä").toString());
        assertEquals("attachment; filename=\"baz-?-ä\"; filename*=UTF-8''baz-%e2%82%ac-%c3%a4", attachment(Path.of("foo", "bar", "baz-€-ä")).toString());
    }
}
