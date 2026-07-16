// Validate an XRechnung XML file using the InvoiceXML API.
// Runs the XSD, the EN 16931 Schematron, and the KoSIT BR-DE rules.
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/validate/xrechnung
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;

public class Validate {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "invoice.xml",
                RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/validate/xrechnung")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String report = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + report);
                System.exit(1);
            }
            System.out.println(report);
        }
    }
}
