// Create an XRechnung 3.0 invoice (EN 16931 XML, KoSIT CIUS) using the
// InvoiceXML REST API. Sends a JSON invoice model and receives the XML.
//
// InvoiceXML:          https://www.invoicexml.com/
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/create/xrechnung
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class CreateXRechnung
{
    private const string Endpoint = "https://api.invoicexml.com/v1/create/xrechnung";

    public static async Task<string> RunAsync(string outputPath = "invoice-xrechnung.xml")
    {
        // Get an InvoiceXML API key at:
        // https://www.invoicexml.com/account/authentication
        // Raw key only, without the "Bearer " prefix (WithOAuthBearerToken adds it).
        var apiKey = Environment.GetEnvironmentVariable("INVOICEXML_API_KEY")
            ?? throw new InvalidOperationException(
                "Set INVOICEXML_API_KEY environment variable. " +
                "Get an InvoiceXML API key at " +
                "https://www.invoicexml.com/account/authentication");

        // Minimal XRechnung invoice payload. Full field reference:
        // https://www.invoicexml.com/docs/api/create/xrechnung
        var payload = new
        {
            invoice = new
            {
                invoiceNumber = "XR-2026-001",
                issueDate     = "2026-05-18",
                currency      = "EUR",
                // The Leitweg-ID (BT-10), required for German B2G invoicing.
                buyerReference = "991-12345-67",
                seller = new
                {
                    name              = "Acme GmbH",
                    vatIdentifier     = "DE123456789",
                    legalRegistration = new { identifier = "HRB 12345" },
                    postalAddress = new
                    {
                        line1    = "Hauptstraße 12",
                        city     = "Berlin",
                        postCode = "10115",
                        country  = "DE"
                    },
                    // XRechnung requires the seller contact group and an electronic address.
                    contact = new
                    {
                        name  = "Max Mustermann",
                        phone = "+49 30 12345678",
                        email = "billing@acme.de"
                    },
                    electronicAddress = new { identifier = "DE123456789", schemeId = "9930" }
                },
                buyer = new
                {
                    name = "Bundesamt für Musterverwaltung",
                    postalAddress = new
                    {
                        line1    = "Behördenstraße 5",
                        city     = "Bonn",
                        postCode = "53113",
                        country  = "DE"
                    },
                    electronicAddress = new { identifier = "991-12345-67", schemeId = "0204" }
                },
                paymentDetails = new { paymentAccountIdentifier = "DE89370400440532013000" },
                lines = new[]
                {
                    new
                    {
                        quantity       = 10,
                        priceDetails   = new { netPrice = 150.00m },
                        vatInformation = new { rate = 19.00m },
                        item           = new { name = "Senior consulting" }
                    }
                }
            },
            options = new { syntax = "ubl" }
        };

        try
        {
            var xml = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostJsonAsync(payload)
                .ReceiveString();

            await File.WriteAllTextAsync(outputPath, xml);
            Console.WriteLine($"XRechnung invoice saved: {outputPath} ({xml.Length:N0} chars)");
            return xml;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
