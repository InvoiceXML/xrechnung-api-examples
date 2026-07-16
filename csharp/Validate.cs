// Validate an XRechnung XML file using the InvoiceXML REST API. Runs the XSD for
// the detected syntax (UBL 2.1 or CII D16B), the EN 16931 Schematron, and the
// German BR-DE rules from the KoSIT XRechnung Schematron.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/validate/xrechnung
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class ValidateXRechnung
{
    private const string Endpoint = "https://api.invoicexml.com/v1/validate/xrechnung";

    public static async Task<string> RunAsync(string xmlPath = "invoice.xml")
    {
        // Get an InvoiceXML API key at: https://www.invoicexml.com/account/authentication
        // Raw key only, without the "Bearer " prefix (WithOAuthBearerToken adds it).
        var apiKey = Environment.GetEnvironmentVariable("INVOICEXML_API_KEY")
            ?? throw new InvalidOperationException(
                "Set INVOICEXML_API_KEY environment variable. " +
                "Get an InvoiceXML API key at " +
                "https://www.invoicexml.com/account/authentication");

        if (!File.Exists(xmlPath))
            throw new FileNotFoundException($"Input XML not found: {xmlPath}");

        try
        {
            // POST the XML as multipart/form-data. This endpoint always applies the
            // XRechnung rule set, so no profile parameter is needed. The response is
            // a JSON validation report. Response structure:
            // https://www.invoicexml.com/docs/api/validate/xrechnung
            var report = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", xmlPath, "application/xml"))
                .ReceiveString();

            Console.WriteLine(report);
            return report;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
