// Render an XRechnung XML file into a human-readable PDF using the InvoiceXML API.
// The rendered PDF is a preview for people to read: the XML file remains the
// authoritative invoice for compliance and tax purposes.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Render {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "invoice.xml",
                RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
            // Label language for the rendered PDF: en, de, or fr. Defaults to en.
            .addFormDataPart("language", "de")
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/render/xrechnung/to/pdf")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            byte[] pdf = response.body().bytes();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + new String(pdf));
                System.exit(1);
            }
            Files.write(Paths.get("invoice-preview.pdf"), pdf);
            System.out.println("Saved invoice-preview.pdf (" + pdf.length + " bytes)");
        }
    }
}
