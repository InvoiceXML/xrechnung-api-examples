// Render an XRechnung XML file into a human-readable PDF using the InvoiceXML
// REST API. The rendered PDF is a preview for people to read: the XML file
// remains the authoritative invoice for compliance and tax purposes.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class RenderXRechnung
{
    private const string Endpoint = "https://api.invoicexml.com/v1/render/xrechnung/to/pdf";

    public static async Task<byte[]> RunAsync(
        string xmlPath = "invoice.xml",
        string outputPath = "invoice-preview.pdf",
        // Label language for the rendered PDF: en, de, or fr. Defaults to en.
        string language = "de")
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
            // POST the XML; the API parses it (auto-detecting CII or UBL syntax),
            // maps every EN 16931 business term to a readable layout, and returns
            // the rendered PDF.
            var pdfBytes = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", xmlPath, "application/xml")
                    .AddString("language", language)
                )
                .ReceiveBytes();

            await File.WriteAllBytesAsync(outputPath, pdfBytes);
            Console.WriteLine($"XRechnung preview saved: {outputPath} ({pdfBytes.Length:N0} bytes)");
            return pdfBytes;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
