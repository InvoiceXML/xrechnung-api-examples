// Create an XRechnung 3.0 invoice using the InvoiceXML API.
// Sends a JSON invoice model and receives the XRechnung XML.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/create/xrechnung
//
// Requires Java 15+ (uses text blocks).
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Create {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        // buyerReference carries the Leitweg-ID (BT-10) and is required for German B2G.
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
              "options": { "version": "3.0" }
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
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + xml);
                System.exit(1);
            }
            Files.write(Paths.get("invoice-xrechnung.xml"), xml.getBytes("UTF-8"));
            System.out.println("Saved invoice-xrechnung.xml (" + xml.length() + " chars)");
        }
    }
}
