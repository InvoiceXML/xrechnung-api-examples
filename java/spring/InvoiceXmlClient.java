// Spring RestClient wrapper around the InvoiceXML API for XRechnung.
//
// Every XRechnung operation the OkHttp examples in the parent folder cover, written
// against Spring's RestClient (Spring Framework 6.1+ / Spring Boot 3.2+) so it drops
// straight into a Spring Boot service without adding a second HTTP client.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api
//
// Change the package to your own. Wiring: see InvoiceXmlConfig.java.

package com.example.invoicexml;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InvoiceXmlClient {

    private final RestClient http;

    public InvoiceXmlClient(RestClient.Builder builder, String apiKey) {
        this.http = builder
                .baseUrl("https://api.invoicexml.com")
                // Raw key only. The "Bearer " prefix is added here, so passing a key that
                // already starts with "Bearer " would send it twice and fail with a 401.
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Build an XRechnung 3.0 invoice from a JSON invoice model. The default syntax is
     * UBL 2.1; set options.syntax to "cii" in the payload when a receiver insists on the
     * CII syntax. Both carry the same BT-24 and pass the same KoSIT BR-DE rule set.
     *
     * POST /v1/create/xrechnung
     */
    public byte[] createXRechnung(String invoiceJson) {
        return http.post()
                .uri("/v1/create/xrechnung")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invoiceJson)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(byte[].class);
    }

    /**
     * Validate an XRechnung XML against the official KoSIT rules and get a JSON report.
     * Unlike the other calls a failed validation is still a 200: read the "valid" field
     * of the report rather than catching an exception.
     *
     * POST /v1/validate/xrechnung
     */
    public String validateXRechnung(byte[] xml) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", part(xml, "invoice.xml"));

        return http.post()
                .uri("/v1/validate/xrechnung")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(String.class);
    }

    /**
     * Parse an XRechnung XML into the JSON invoice model, ready to deserialise with
     * Jackson. The response is an envelope: { "invoice": { ... } }.
     *
     * POST /v1/extract/json
     */
    public String extractJson(byte[] xml) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", part(xml, "invoice.xml"));

        return http.post()
                .uri("/v1/extract/json")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(String.class);
    }

    /**
     * Render an XRechnung XML into a human-readable PDF, for a customer portal preview
     * or an email attachment alongside the XML. Language is "en", "de" or "fr".
     *
     * POST /v1/render/xrechnung/to/pdf
     */
    public byte[] renderPdf(byte[] xml, String language) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", part(xml, "invoice.xml"));
        form.add("language", language);

        return http.post()
                .uri("/v1/render/xrechnung/to/pdf")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(byte[].class);
    }

    // Errors come back as application/problem+json carrying an "errorCode" field.
    // The ones worth branching on: 4001 the XML failed validation (every failed rule is
    // listed), 4002 the JSON model is incomplete, 4004 the upload was not an XML file.
    private void fail(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        throw new InvoiceXmlApiException(response.getStatusCode().value(), body);
    }

    // Spring only writes a filename into the multipart Content-Disposition when the
    // resource exposes one, and the API keys off the .xml extension. Without this
    // override the upload is rejected with 4004 (unsupported content type).
    private static ByteArrayResource part(byte[] data, String filename) {
        return new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    public static class InvoiceXmlApiException extends RuntimeException {
        private final int status;
        private final String problemJson;

        public InvoiceXmlApiException(int status, String problemJson) {
            super("InvoiceXML API error " + status + ": " + problemJson);
            this.status = status;
            this.problemJson = problemJson;
        }

        public int getStatus() {
            return status;
        }

        /** The raw application/problem+json body, including the errorCode field. */
        public String getProblemJson() {
            return problemJson;
        }
    }
}
