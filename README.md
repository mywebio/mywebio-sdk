# MyWeb.io SDK and client library
[![Build Status](https://travis-ci.org/mywebio/mywebio-sdk.svg)](https://travis-ci.org/mywebio/mywebio-sdk)

This library allows you to write web applications for Android.
It works with Android application available on [Google Play](https://play.google.com/store/apps/details?id=io.myweb.server.alpha).

To find out more please visit [myweb.io](http://www.myweb.io/)

## Building
You can build project by invoking on console:
```
 $ ./gradlew build
```

## API Overview
### Simple GET
```java
@GET("/mypath")
public String get() {
	return "Hello, World!";
}
```

### Getting data from URL
```java
@GET("/path/:name/:id")
public String dataFromUrl(int id, String name) {
	return name + id;
}
```

### Getting data from query string
```java
@GET("/query?:name=default&:id=0")
public String queryString(int id, String name) {
	return name + id;
}
```
Please note that if request comes without query string (just "/query") then in our example method queryString() receives values id=0 and name=default.

### Content-Type of response
```java
@GET("/produces")
@Produces("application/json")
public String produces() {
	return "{ id = 1 }";
}
```

### Inject android.content.Context
```java
@GET("/assets/:filename")
public InputStream assets(Context ctx, String filename) throws IOException {
	return ctx.getAssets().open(filename);
}
```

### Response as Response object
```java
@GET("/response/*filename")
public Response response(Context ctx, String filename) throws IOException {
  InputStream is = ctx.getAssets().open(filename);
  return Response.ok()
          .withBody(is)
          .withMimeTypeFromFilename(filename);
}
```

### Returning JSON
```java
@GET("/json/:name/:id")
public Response json(String name, int id) throws JSONException {
	JSONObject json = new JSONObject();
	json.put("name", name);
	json.put("id", id);
	return Response.ok().withBody(json);
}
```

## Building myweb.io-enabled project
In order to use myweb.io API you need to add few lines in build.gradle:
```groovy
dependencies {
  compile 'io.myweb:mywebio-api:0.1-SNAPSHOT'
  provided 'io.myweb:mywebio-compiler:0.1-SNAPSHOT'
}
android {
  lintOptions {
    disable 'InvalidPackage'
  }
}
```
That's it! Now you can enter
```
http://ip:8080/your.app.pkg/[your services]
```
in a web browser and see how it works.
