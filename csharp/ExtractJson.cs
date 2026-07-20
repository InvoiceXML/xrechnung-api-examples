// Extract structured invoice data from an XRechnung XML file as JSON using the
// InvoiceXML REST API. Useful for feeding XRechnung invoices into REST
// pipelines, ERPs, or any system that prefers JSON over XML.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/extract/json
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class ExtractJson
{
    private const string Endpoint = "https://api.invoicexml.com/v1/extract/json";

    public static async Task<string> RunAsync(
        string xmlPath = "invoice.xml",
        string outputPath = "invoice.json")
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
            // POST the XML and receive a JSON response with the parsed EN 16931
            // invoice document (seller, buyer, lines, tax breakdown, totals)
            // under the "invoice" key. Response schema:
            // https://www.invoicexml.com/docs/api/extract/json
            var json = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", xmlPath, "application/xml"))
                .ReceiveString();

            await File.WriteAllTextAsync(outputPath, json);
            Console.WriteLine($"XRechnung data extracted to {outputPath}");
            Console.WriteLine(json);
            return json;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
