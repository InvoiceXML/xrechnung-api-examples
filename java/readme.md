# XRechnung for Java: InvoiceXML API Examples

Java code samples for creating, validating, and parsing **XRechnung** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Requires **Java 15 or later** (Java 17 or 21 LTS recommended): `Create.java` and `SendEmail.java` use text blocks, which were introduced in Java 15. The other examples also compile on Java 8. Runs in Spring Boot, Quarkus, Micronaut, Jakarta EE, Android, AWS Lambda, or plain `public static void main` console apps.

For background on the XRechnung standard itself (what it is, the Leitweg-ID, legal status), see the [main repository README](../README.md).

> **Using Spring Boot?** The examples in this folder use OkHttp. There is a parallel set written against Spring's own `RestClient`, so you do not have to add a second HTTP client to your dependency tree: **[Spring Boot examples →](./spring)**

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Java 15 or later** (Java 17 or 21 LTS recommended). `Create.java` and `SendEmail.java` use text blocks, a Java 15+ feature; the other examples also compile on Java 8.
- **OkHttp 4.x** for clean multipart handling and bearer auth

### Maven

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

OkHttp is the de facto standard HTTP client for modern Java. Java's built-in `java.net.http.HttpClient` does not support multipart out of the box, so doing this in pure JDK would triple the line count of every example.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`Create.java`](./Create.java) | Build an XRechnung 3.0 XML invoice | [`POST /v1/create/xrechnung`](https://www.invoicexml.com/docs/api/create/xrechnung) |
| [`SendEmail.java`](./SendEmail.java) | Create an XRechnung invoice and email it from your own domain | [`POST /v1/create/xrechnung/email`](https://www.invoicexml.com/docs/api/create/xrechnung/email) |
| [`Validate.java`](./Validate.java) | Validate an XRechnung file against the KoSIT rules | [`POST /v1/validate/xrechnung`](https://www.invoicexml.com/docs/api/validate/xrechnung) |
| [`ExtractJson.java`](./ExtractJson.java) | Parse an XRechnung XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`AiConvert.java`](./AiConvert.java) | (Experimental) Convert a plain PDF to XRechnung with AI | [`POST /v1/transform/to/xrechnung`](https://www.invoicexml.com/docs/api/transform/to/xrechnung) |
| [`Render.java`](./Render.java) | Render XRechnung XML into a human-readable PDF | [`POST /v1/render/xrechnung/to/pdf`](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf) |

Each file is a standalone `public class` with a `main` method, runnable with `javac` and `java` directly, or as part of any Maven or Gradle project.

> **Note on the snippets below:** they are excerpts from those files. The `try` blocks have no `catch` clause, so the enclosing method must declare `throws Exception` (or at least `throws IOException`), exactly as the full files do with `public static void main(String[] args) throws Exception`. Pasting a snippet into a method without that `throws` clause will not compile ("unreported exception IOException"). When in doubt, copy the complete file.

---

## Create an XRechnung invoice in Java

```java
String json = """
    {
      "invoice": {
        "invoiceNumber": "XR-2026-001",
        "issueDate": "2026-05-18",
        "currency": "EUR",
        "buyerReference": "991-12345-67",
        "seller": {
          "name": "Acme GmbH",
          "vatIdentifier": "DE123456789",
          "legalRegistration": { "identifier": "HRB 12345" },
          "postalAddress": { "line1": "Hauptstraße 12", "city": "Berlin", "postCode": "10115", "country": "DE" },
          "contact": { "name": "Max Mustermann", "phone": "+49 30 12345678", "email": "billing@acme.de" },
          "electronicAddress": { "identifier": "DE123456789", "schemeId": "9930" }
        },
        "buyer": {
          "name": "Bundesamt für Musterverwaltung",
          "postalAddress": { "line1": "Behördenstraße 5", "city": "Bonn", "postCode": "53113", "country": "DE" },
          "electronicAddress": { "identifier": "991-12345-67", "schemeId": "0204" }
        },
        "paymentDetails": { "paymentAccountIdentifier": "DE89370400440532013000" },
        "lines": [{
          "quantity": 10,
          "priceDetails": { "netPrice": 150.00 },
          "vatInformation": { "rate": 19.00 },
          "item": { "name": "Senior consulting" }
        }]
      },
      "options": { "syntax": "ubl" }
    }
    """;

RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/create/xrechnung")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    String xml = response.body().string();
    Files.write(Paths.get("invoice-xrechnung.xml"), xml.getBytes("UTF-8"));
}
```

`buyerReference` carries the Leitweg-ID (BT-10), and the seller `contact` and `electronicAddress` groups are what the XRechnung CIUS requires on top of plain EN 16931. Omit any of them and the API returns a 400 naming the `BR-DE-*` rule you missed.

The response is the XRechnung 3.0 XML document, validated against the KoSIT rules before delivery.

[Full example: `Create.java`](./Create.java) | [API reference](https://www.invoicexml.com/docs/api/create/xrechnung)

---

## Validate an XRechnung file in Java

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.xml",
        RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
    .build();

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/validate/xrechnung")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    System.out.println(response.body().string());
}
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the German BR-DE-* rules).

[Full example: `Validate.java`](./Validate.java) | [API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)

---

## Extract XRechnung data as JSON in Java

Useful for feeding XRechnung invoices into Spring Boot services, message queues, or any pipeline that prefers JSON over XML.

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.xml",
        RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
    .build();

try (Response response = new OkHttpClient().newCall(
        new Request.Builder()
            .url("https://api.invoicexml.com/v1/extract/json")
            .header("Authorization", "Bearer " + apiKey)
            .post(body).build()
    ).execute()) {
    String json = response.body().string();
    // Deserialize with Jackson, Gson, or your preferred JSON library.
    // The invoice document sits under the "invoice" key: seller, buyer, lines, totals.
}
```

[Full example: `ExtractJson.java`](./ExtractJson.java) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## (Experimental) Convert a plain PDF to XRechnung with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before submitting it to a public authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-xrechnung-conversion-experimental).

The endpoint takes the PDF plus a `buyerReference` form field: the Leitweg-ID cannot be inferred from the source document, so you must supply it.

[Full example: `AiConvert.java`](./AiConvert.java) | [API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)

---

## Render XRechnung as a readable PDF in Java

XRechnung has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.xml",
        RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
    .addFormDataPart("language", "de")   // en, de, or fr
    .build();

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/render/xrechnung/to/pdf")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    Files.write(Paths.get("invoice-preview.pdf"), response.body().bytes());
}
```

[Full example: `Render.java`](./Render.java) | [API reference](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)

---

## Framework integration

### Spring Boot

Return an XRechnung invoice from a REST controller:

```java
@RestController
public class XRechnungController {

    @GetMapping(value = "/invoices/{id}/xrechnung", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String id) throws Exception {
        byte[] pdf = xrechnungService.create(id);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .body(pdf);
    }
}
```

Inject the API key from `application.properties` via `@Value("${invoicexml.api-key}")`.

To drop OkHttp entirely and call the API through Spring's own `RestClient`, use the [Spring Boot examples](./spring) instead: an injectable client, a `@Configuration` bean with connection pooling, and the multipart filename detail that trips most first integrations.

### Quarkus

```java
@Path("/invoices")
public class XRechnungResource {

    @GET
    @Path("/{id}/xrechnung")
    @Produces("application/pdf")
    public Response download(@PathParam("id") String id) throws Exception {
        byte[] pdf = xrechnungService.create(id);
        return Response.ok(pdf)
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .build();
    }
}
```

### Android

OkHttp is also Square's library for Android, so the examples run on Android 5.0+ unchanged. Move the call off the main thread using `enqueue()`, `CoroutineScope`, or `RxJava`. For Android 9+ ensure your network security config allows TLS to `api.invoicexml.com` (it does by default).

### AWS Lambda

Bundle OkHttp into your Lambda deployment package or layer. Cold-start latency is minimal because OkHttp is small (~700 KB).

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: pasting the whole `Bearer xxx` value as `apiKey`, which sends `Bearer Bearer xxx`. Set `apiKey` to the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`RequestBody.create()` argument order**: OkHttp 4.x reversed the parameter order from 3.x. Use `RequestBody.create(File, MediaType)` for 4.x. The 3.x signature `(MediaType, File)` is deprecated but still callable.
- **`Files.writeString` not found**: that method requires Java 11+. The examples use `Files.write(Path, byte[])` which works on Java 8+.
- **TLS handshake failures on old JDKs**: enable TLS 1.2 explicitly on Java 8 with `-Dhttps.protocols=TLSv1.2,TLSv1.3`, or upgrade to Java 11+.
- **BR-DE-* failures on Validate**: an XRechnung-specific field is missing. The most common are BR-DE-15 (no Leitweg-ID in `buyerReference`), BR-DE-2 (no seller contact group), and BR-DE-1 (no seller electronic address).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [Validate XRechnung API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to XRechnung API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)
- [OkHttp documentation](https://square.github.io/okhttp/)
- [Main repository README](../README.md)
