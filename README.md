# Content Disposition

[![](https://jitpack.io/v/org.teacon/content-disposition.svg)](https://jitpack.io/#org.teacon/content-disposition)

Java implementation of parsing and constructing content-disposition ([RFC6266](https://datatracker.ietf.org/doc/html/rfc6266)) header values.

This library requires Java 11 or above.

## Include Dependency

```groovy
// Add it in your root build.gradle at the end of repositories
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
// Add the dependency
dependencies {
    implementation 'org.teacon:content-disposition:1.0.0'
}
```

## API Usage

```java
import org.teacon.content_disposition.ContentDisposition;

// parse a content-disposition header
var obj = ContentDisposition.parse("attachment; filename=\"foo.html\" filename*=UTF-8''foo.html");

// construct a content-disposition header directly
var obj = ContentDisposition.inline(); // inline
var obj = ContentDisposition.attachment(); // attachment
var obj = ContentDisposition.inline("foo.html"); // inline; filename="foo.html" filename*=UTF-8''foo.html
var obj = ContentDisposition.attachment("bar.html"); // attachment; filename="bar.html" filename*=UTF-8''bar.html

// construct a content-disposition header by builder
var obj = ContentDisposition.type("custom").parameter("foo", "bar").build(); // custom; foo=bar

// utility methods
var obj = ContentDisposition.type("attachment").parameter("foo", "bar").filename("baz.txt").build();
obj.getType(); // custom
obj.getParameters(); // {foo=bar, filename=baz.txt}
obj.isInline(); // false
obj.isAttachment(); // true
obj.getFilename(); // Optional[baz.txt]
obj.equals(ContentDisposition.parse("attachment; foo=bar, filename=\"baz.txt\", filename*=UTF-8''baz.txt")); // true
obj.toString(); // attachment; foo=bar, filename="baz.txt", filename*=UTF-8''baz.txt
```
