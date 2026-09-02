# XRechnung with Spring Boot: RestClient examples

The same XRechnung operations as the [OkHttp examples](../readme.md) in the parent folder, written against **Spring's `RestClient`** instead. If your backend is Spring Boot, this version drops in without adding a second HTTP client to your dependency tree.

Requires **Spring Framework 6.1+ / Spring Boot 3.2+** (when `RestClient` was introduced) and **Java 17+**.

For background on the XRechnung standard itself (what it is, the Leitweg-ID, legal status), see the [main repository README](../../README.md).

## Get your API key

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Set `invoicexml.api-key` in `application.properties` to the **raw key only**, without the `Bearer ` prefix. The client adds the prefix itself:

```properties
invoicexml.api-key=ixml_a1b2c3...
```

## Files in this folder

| File | Purpose |
|---|---|
| [`InvoiceXmlClient.java`](./InvoiceXmlClient.java) | The client. One method per endpoint, with problem+json error handling. |
| [`XRechnungFlow.java`](./XRechnungFlow.java) | Create, validate and render, with a sample B2G payload. |
| [`InvoiceXmlConfig.java`](./InvoiceXmlConfig.java) | Spring wiring, including a pooled connection manager for batch load. |

Change the package declaration (`com.example.invoicexml`) to your own before using them.

## Dependencies

`spring-boot-starter-web` already brings `RestClient`. The only addition is Apache HttpClient 5, used by [`InvoiceXmlConfig.java`](./InvoiceXmlConfig.java) for connection pooling. Skip it if you are happy with the default request factory and low concurrency.

### Maven

```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

### Gradle

```groovy
implementation 'org.apache.httpcomponents.client5:httpclient5'
```

---

## Create an XRechnung invoice

```java
byte[] xml = invoiceXmlClient.createXRechnung(invoiceJson);
```

`buyerReference` in the payload carries the Leitweg-ID (BT-10) and is required for German B2G. The default output syntax is UBL 2.1; set `options.syntax` to `"cii"` when a receiver insists on the CII syntax. Both carry the same BT-24 and pass the same KoSIT BR-DE rule set, so the choice is only about which dialect the receiver prefers.

The endpoint validates before it answers, so anything that comes back is already conformant.

[`POST /v1/create/xrechnung`](https://www.invoicexml.com/docs/api/create/xrechnung)

## Validate an XRechnung invoice

```java
String report = invoiceXmlClient.validateXRechnung(xmlBytes);
```

Returns a JSON report against the official KoSIT rules. A **failing** validation is still a `200` with `"valid": false` in the body, so read the field rather than catching an exception.

[`POST /v1/validate/xrechnung`](https://www.invoicexml.com/docs/api/validate/xrechnung)

## Render a readable PDF

XRechnung is XML only, with no visual layer of its own. When you need something a human can read, render one. Language is `"en"`, `"de"` or `"fr"`.

```java
byte[] pdf = invoiceXmlClient.renderPdf(xmlBytes, "de");
```

[`POST /v1/render/xrechnung/to/pdf`](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)

## Parse an XRechnung into JSON

```java
String json = invoiceXmlClient.extractJson(xmlBytes);
```

The response is an envelope: `{ "invoice": { ... } }`. Deserialise with Jackson into your own model.

[`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json)

---

## Multipart uploads: the one Spring gotcha

Spring writes a `filename` into the multipart `Content-Disposition` **only when the resource exposes one**, and the API keys off the `.xml` extension. A plain `ByteArrayResource` has no filename, so the upload is rejected with `errorCode` 4004. Override `getFilename()`:

```java
private static ByteArrayResource part(byte[] data, String filename) {
    return new ByteArrayResource(data) {
        @Override
        public String getFilename() {
            return filename;
        }
    };
}

MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
form.add("file", part(xmlBytes, "invoice.xml"));

String report = http.post()
        .uri("/v1/validate/xrechnung")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(form)
        .retrieve()
        .body(String.class);
```

---

## Error handling

Errors are `application/problem+json` with an `errorCode` field. `RestClient` throws on 4xx and 5xx by default, but the default handler discards the body, which is where the useful part lives: on a rule failure the response lists **every** failed rule, not just the first. The client captures it with `onStatus`:

```java
.retrieve()
.onStatus(HttpStatusCode::isError, (request, response) -> {
    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    throw new InvoiceXmlApiException(response.getStatusCode().value(), body);
})
.body(byte[].class);
```

Codes worth branching on:

| Code | Meaning |
|---|---|
| 4001 | The XML failed validation. The response lists every failed rule. |
| 4002 | The JSON invoice model is incomplete or malformed. |
| 4004 | The upload was not recognised as an XML file (see the filename note above). |

---

## Concurrency and timeouts

The default `RestClient` request factory caps parallel connections low enough to serialise a batch. For sustained parallel load, [`InvoiceXmlConfig.java`](./InvoiceXmlConfig.java) swaps in a pooled Apache HttpClient 5 factory:

```java
ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(10))
        .setSocketTimeout(Timeout.ofSeconds(60))
        .build();

PoolingHttpClientConnectionManager pool = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig)
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(100)
        .build();
```

Timeouts sit on the connection manager rather than on `HttpComponentsClientHttpRequestFactory`, whose own `setReadTimeout` is deprecated from Spring Framework 6.2. A 60 second socket timeout is comfortable: validation runs the full KoSIT schematron and rendering builds a whole PDF, so those two are the slowest calls in the set.

---

## Using it from a controller

```java
@RestController
public class XRechnungController {

    private final InvoiceXmlClient invoiceXmlClient;

    public XRechnungController(InvoiceXmlClient invoiceXmlClient) {
        this.invoiceXmlClient = invoiceXmlClient;
    }

    @GetMapping(value = "/invoices/{id}/xrechnung", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        String json = invoiceMapper.toInvoiceXmlModel(invoiceRepository.get(id));
        byte[] xml = invoiceXmlClient.createXRechnung(json);

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".xml\"")
            .body(xml);
    }
}
```

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [Validate XRechnung API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)
- [Render XRechnung to PDF API reference](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Spring RestClient documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
- [OkHttp examples for plain Java](../readme.md)
- [Main repository README](../../README.md)
