// End-to-end XRechnung flow for a Spring Boot backend: JSON invoice model in,
// XRechnung 3.0 UBL out, optionally validated and rendered for a human reader.
//
// Requires Java 15+ for the text block. Change the package to your own.

package com.example.invoicexml;

import java.nio.file.Files;
import java.nio.file.Path;

public class XRechnungFlow {

    private final InvoiceXmlClient api;

    public XRechnungFlow(InvoiceXmlClient api) {
        this.api = api;
    }

    /** The XML you send to the buyer's portal or Peppol access point. */
    public byte[] create(String invoiceJson) {
        return api.createXRechnung(invoiceJson);
    }

    /** Optional: a human-readable PDF of the same invoice, for your own records. */
    public byte[] preview(byte[] xrechnungXml) {
        return api.renderPdf(xrechnungXml, "de");
    }

    // Map your own invoice model onto this shape. buyerReference carries the Leitweg-ID
    // (BT-10) and is required for German B2G. Full field reference at
    // https://www.invoicexml.com/docs/api/create/xrechnung
    public static final String EXAMPLE_JSON = """
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
              "postalAddress": {
                "line1": "Hauptstraße 12",
                "city": "Berlin",
                "postCode": "10115",
                "country": "DE"
              },
              "contact": {
                "name": "Max Mustermann",
                "phone": "+49 30 12345678",
                "email": "billing@acme.de"
              },
              "electronicAddress": { "identifier": "DE123456789", "schemeId": "9930" }
            },
            "buyer": {
              "name": "Bundesamt für Musterverwaltung",
              "postalAddress": {
                "line1": "Behördenstraße 5",
                "city": "Bonn",
                "postCode": "53113",
                "country": "DE"
              },
              "electronicAddress": { "identifier": "991-12345-67", "schemeId": "0204" }
            },
            "paymentDetails": { "paymentAccountIdentifier": "DE89370400440532013000" },
            "lines": [
              {
                "quantity": 10,
                "priceDetails": { "netPrice": 150.00 },
                "vatInformation": { "rate": 19.00 },
                "item": { "name": "Senior consulting" }
              }
            ]
          },
          "options": { "syntax": "ubl" }
        }
        """;

    // Standalone runner. In a Spring Boot application you would inject InvoiceXmlClient
    // from InvoiceXmlConfig instead of building one here.
    public static void main(String[] args) throws Exception {
        var api = new InvoiceXmlClient(
                org.springframework.web.client.RestClient.builder(),
                System.getenv("INVOICEXML_API_KEY"));

        var flow = new XRechnungFlow(api);

        byte[] xml = flow.create(EXAMPLE_JSON);
        Files.write(Path.of("invoice-xrechnung.xml"), xml);
        System.out.println("Saved invoice-xrechnung.xml (" + xml.length + " bytes)");

        // The create endpoint already validated the document, so this only ever prints
        // a passing report. It is here to show the call shape for invoices produced
        // elsewhere, where the report is the point.
        System.out.println(api.validateXRechnung(xml));

        Files.write(Path.of("invoice-xrechnung.pdf"), flow.preview(xml));
        System.out.println("Saved invoice-xrechnung.pdf");
    }
}
