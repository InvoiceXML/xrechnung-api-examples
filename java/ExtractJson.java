// Extract XRechnung invoice data as JSON using the InvoiceXML API.
// Upload the XRechnung XML and receive the EN 16931 model as JSON.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/extract/json
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExtractJson {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "invoice.xml",
                RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/extract/json")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String json = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + json);
                System.exit(1);
            }
            Files.write(Paths.get("invoice.json"), json.getBytes());
            System.out.println(json);
        }
    }
}
