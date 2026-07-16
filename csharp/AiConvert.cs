// EXPERIMENTAL: Convert a plain (unstructured) PDF invoice into a compliant
// XRechnung 3.0 XML document, using the InvoiceXML AI-powered transform API.
//
// WARNING: Real-world PDF invoices are often messy: scanned, irregularly
// formatted, multi-page, multilingual, or missing fields that EN 16931
// requires. AI extraction can make subtle mistakes (wrong tax codes,
// transposed amounts, missing VAT IDs, currency issues) that automated
// validators may not catch. ALWAYS review the output before sending
// it to a customer or tax authority.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/transform/to/xrechnung
//
// Required NuGet package:
//   dotnet add package Flurl.Http


using Flurl.Http;

public static class AiConvert
{
    private const string Endpoint = "https://api.invoicexml.com/v1/transform/to/xrechnung";

    public static async Task<string> RunAsync(
        string inputPdfPath = "plain-invoice.pdf",
        string outputPath = "converted-xrechnung.xml",
        // The Leitweg-ID (BT-10) is required: it identifies the German public
        // authority you are invoicing and cannot be inferred from the PDF.
        string buyerReference = "991-12345-67")
    {
        // Get an InvoiceXML API key at: https://www.invoicexml.com/account/authentication
        // Raw key only, without the "Bearer " prefix (WithOAuthBearerToken adds it).
        var apiKey = Environment.GetEnvironmentVariable("INVOICEXML_API_KEY")
            ?? throw new InvalidOperationException(
                "Set INVOICEXML_API_KEY environment variable. " +
                "Get an InvoiceXML API key at " +
                "https://www.invoicexml.com/account/authentication");

        if (!File.Exists(inputPdfPath))
            throw new FileNotFoundException($"Input PDF not found: {inputPdfPath}");

        try
        {
            // POST the plain PDF; InvoiceXML extracts the invoice fields with AI,
            // builds an EN 16931 XML payload, and validates it against the
            // XRechnung rules. Parameter reference:
            // https://www.invoicexml.com/docs/api/transform/to/xrechnung
            var xml = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", inputPdfPath, "application/pdf")
                    .AddString("buyerReference", buyerReference)
                )
                .ReceiveString();

            await File.WriteAllTextAsync(outputPath, xml);
            Console.WriteLine($"Converted XRechnung saved: {outputPath} ({xml.Length:N0} chars)");
            Console.WriteLine("REMINDER: review the output manually before sending to a customer or tax authority.");
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
