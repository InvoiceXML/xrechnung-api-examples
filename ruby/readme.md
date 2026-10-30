# XRechnung for Ruby: InvoiceXML API Examples

Ruby code samples for creating, validating, parsing, and rendering **XRechnung** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Ruby 3.0 and later**, using only `Net::HTTP` from the standard library. Runs in Rails, Sinatra, Hanami, Sidekiq jobs, Rake tasks, or plain scripts. **Zero gems.**

For background on XRechnung itself (what it is, the KoSIT rules, why German public authorities require it), see the [main repository README](../README.md). A full walkthrough with Rails patterns is in the guide to [ZUGFeRD and XRechnung in Ruby](https://www.invoicexml.com/blog/zugferd-xrechnung-ruby) on invoicexml.com.

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

The examples read the key from the `INVOICEXML_API_KEY` environment variable:

```bash
export INVOICEXML_API_KEY=YOUR_API_KEY
```

Or on Windows (PowerShell):

```powershell
$env:INVOICEXML_API_KEY = "YOUR_API_KEY"
```

**Important:** set the variable to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Ruby 3.0 or later** (any currently supported Ruby)
- No gems, no Gemfile needed

The examples use `Net::HTTP`, `JSON`, and `URI` from the standard library. Ruby has a small advantage here over most languages: `Net::HTTP` supports multipart uploads natively via `request.set_form`, so no multipart helper gem is needed. In a Rails app you can swap in Faraday later without changing the shape of the calls.

There is a reason nothing here runs locally. No maintained XRechnung gem exists, and validation is the wall: the official KoSIT artifacts and the EN 16931 Schematron compile to XSLT 2.0, while Ruby's Nokogiri wraps libxslt and speaks XSLT 1.0 only. The KoSIT validator itself is a Java application, so Ruby teams determined to validate locally end up managing a JVM subprocess and parsing its XML reports. Keeping the rule sets server-side avoids that entirely.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.rb`](./create.rb) | Build an XRechnung 3.0 XML invoice | [`POST /v1/create/xrechnung`](https://www.invoicexml.com/docs/api/create/xrechnung) |
| [`send_email.rb`](./send_email.rb) | Create an XRechnung invoice and email it from your own domain | [`POST /v1/create/xrechnung/email`](https://www.invoicexml.com/docs/api/create/xrechnung/email) |
| [`validate.rb`](./validate.rb) | Validate against the XSD, EN 16931, and the KoSIT BR-DE rules | [`POST /v1/validate/xrechnung`](https://www.invoicexml.com/docs/api/validate/xrechnung) |
| [`extract_json.rb`](./extract_json.rb) | Parse an XRechnung XML into JSON (deterministic, no AI) | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`render.rb`](./render.rb) | Render XRechnung XML into a human-readable PDF | [`POST /v1/render/xrechnung/to/pdf`](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf) |
| [`parse_json.rb`](./parse_json.rb) | Parse an old-school invoice PDF (scan, photo) with AI, with confidence scores | [`POST /v1/parse/json`](https://www.invoicexml.com/docs/api/parse/json) |
| [`extract_attachments.rb`](./extract_attachments.rb) | Extract embedded supporting documents (BG-24) as a ZIP | [`POST /v1/extract/attachments`](https://www.invoicexml.com/docs/api/extract/attachments) |

Each file is standalone and runnable with `ruby create.rb`. Set `INVOICEXML_API_KEY` in your environment and execute. The files that take an input document default to `invoice-xrechnung.xml` (the output of `create.rb`), or accept a path as the first argument.

> **Note on the snippets below:** they are excerpts from those files and assume `BASE_URL`, `API_KEY`, and the `api_http` helper are already defined. When in doubt, copy the complete file.

All files share this small setup block:

```ruby
require "net/http"
require "json"
require "uri"

BASE_URL = "https://api.invoicexml.com"
API_KEY  = ENV.fetch("INVOICEXML_API_KEY")

def api_http(uri)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  http.open_timeout = 10
  http.read_timeout = 120
  http
end
```

---

## Create an XRechnung invoice in Ruby

The payload is a plain Ruby hash serialized with `JSON.generate`. Two things are XRechnung-specific and base EN 16931 does not require them:

**`buyerReference` carries the Leitweg-ID.** This maps to BT-10 and is checked by rule BR-DE-15. The public sector buyer assigns it during onboarding; store it per buyer and inject it into every request. It is routing infrastructure, not invoice data, so no source document will ever contain it.

**Seller contact and electronic addresses are mandatory.** XRechnung requires the full `seller.contact` group (name, phone, email) and electronic addresses for both parties.

```ruby
payload = {
  invoice: {
    invoiceNumber: "XR-2026-001",
    issueDate: "2026-05-18",
    currency: "EUR",
    buyerReference: "991-12345-67",   # Leitweg-ID (BT-10)
    seller: {
      name: "Acme GmbH",
      vatIdentifier: "DE123456789",
      legalRegistration: { identifier: "HRB 12345" },
      postalAddress: { line1: "Hauptstraße 12", city: "Berlin", postCode: "10115", country: "DE" },
      contact: { name: "Max Mustermann", phone: "+49 30 12345678", email: "billing@acme.de" },
      electronicAddress: { identifier: "DE123456789", schemeId: "9930" }
    },
    buyer: {
      name: "Bundesamt für Musterverwaltung",
      postalAddress: { line1: "Behördenstraße 5", city: "Bonn", postCode: "53113", country: "DE" },
      electronicAddress: { identifier: "991-12345-67", schemeId: "0204" }
    },
    paymentDetails: { paymentAccountIdentifier: "DE89370400440532013000" },
    lines: [
      { quantity: 10, priceDetails: { netPrice: 150.00 }, vatInformation: { rate: 19.00 }, item: { name: "Senior consulting" } }
    ]
  },
  options: { syntax: "ubl" }
}

uri = URI("#{BASE_URL}/v1/create/xrechnung")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"
request["Content-Type"]  = "application/json"
request.body = JSON.generate(payload)

response = api_http(uri).request(request)
raise response.body unless response.is_a?(Net::HTTPSuccess)

File.binwrite("invoice-xrechnung.xml", response.body)
```

The response is XRechnung 3.0 in UBL syntax, already checked against the EN 16931 Schematron and the KoSIT rules. XRechnung also permits a CII binding: request it via `options: { syntax: "cii" }` with the same invoice hash. Both outputs are legally equivalent.

[Full example: `create.rb`](./create.rb) | [API reference](https://www.invoicexml.com/docs/api/create/xrechnung)

---

## Validate an XRechnung file in Ruby

The endpoint auto-detects CII or UBL syntax and runs three layers: the XSD, the EN 16931 core Schematron, and the German BR-DE rules from the KoSIT Schematron, the same rule set ZRE and OZG-RE enforce on submission. It accepts XML from any source, so it doubles as an independent check on hand-built or third-party output.

```ruby
def validate_xrechnung(path)
  uri = URI("#{BASE_URL}/v1/validate/xrechnung")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    JSON.parse(api_http(uri).request(request).body)
  end
end

report = validate_xrechnung("invoice-xrechnung.xml")

unless report["valid"]
  report["errors"].each do |finding|
    layer = finding["layer"] # "xsd", "en16931", or "cius"
    puts "[#{finding["rule"]}] (#{layer}) #{finding["message"]}"
  end
end
```

A finished validation always answers HTTP 200: the verdict lives in `valid`, and non-2xx statuses mean transport problems. The `layer` tag is worth surfacing to users: a `cius` failure means the document is a perfectly fine EN 16931 invoice that specifically misses a German requirement, and the classic example is BR-DE-15 firing on a missing Leitweg-ID.

Note this endpoint accepts XML only. A ZUGFeRD hybrid PDF declaring the XRechnung reference profile goes to `/v1/validate/zugferd` instead, and its embedded XML is routed to the same KoSIT rules automatically.

[Full example: `validate.rb`](./validate.rb) | [API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)

---

## Parse XRechnung data as JSON in Ruby

Upload the XML and get the EN 16931 model back as clean JSON, ready for your models or an ERP import. Deterministic parsing, no AI involved.

```ruby
def extract_invoice(path)
  uri = URI("#{BASE_URL}/v1/extract/json")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    JSON.parse(api_http(uri).request(request).body)
  end
end

# The invoice document sits under the "invoice" key of the response.
invoice = extract_invoice("invoice-xrechnung.xml")["invoice"]
puts "#{invoice["invoiceNumber"]} from #{invoice.dig("seller", "name")}"
puts invoice.dig("totals", "grandTotalAmount")  # BT-112
```

[Full example: `extract_json.rb`](./extract_json.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Render XRechnung as a readable PDF in Ruby

XRechnung has no visual layer, so render it before submission, or when an incoming one needs human review. GoBD archiving is one reason this matters on the B2G side: the XML is what you keep, but people still need to read the invoice.

```ruby
def render_xrechnung(xml_path, pdf_path)
  uri = URI("#{BASE_URL}/v1/render/xrechnung/to/pdf")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(xml_path, "rb") do |file|
    # Label language for the rendered PDF: en, de, or fr. Defaults to en.
    request.set_form([["file", file], ["language", "de"]], "multipart/form-data")
    File.binwrite(pdf_path, api_http(uri).request(request).body)
  end
end

render_xrechnung("invoice-xrechnung.xml", "invoice-preview.pdf")
```

The preview is for human eyes only. The XML remains the legal document, and the rendered PDF should never be submitted to a portal.

[Full example: `render.rb`](./render.rb) | [API reference](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)

---

## Parse old-school invoice PDFs with AI in Ruby

Receiving is the half of the German mandate that arrived first: since January 2025 you cannot refuse a compliant e-invoice, but suppliers will keep sending ordinary PDFs for years. For typed, scanned, or photographed PDFs with no embedded XML, `POST /v1/parse/json` reads the document with AI and returns the same `InvoiceDocument` shape as the extract endpoint, plus a `confidence` object: an overall score and four area scores (seller identification, buyer identification, tax calculation, line items), each from 0.0 to 1.0. Automate the confident reads, route the shaky ones to a human:

```ruby
result     = parse_invoice_pdf("supplier-scan.pdf")
invoice    = result["invoice"]
confidence = result.dig("confidence", "overall")

if confidence < 0.7
  queue_for_human_review(invoice, result["confidence"])
else
  import_into_erp(invoice)
end
```

The two intake endpoints compose into one path: try `/v1/extract/json` first, and when it answers 400 with error code 4006 (no structured XML), hand the file to the AI parser instead.

[Full example: `parse_json.rb`](./parse_json.rb) | [API reference](https://www.invoicexml.com/docs/api/parse/json)

---

## Extract embedded attachments in Ruby

E-invoices can carry their supporting documents inside the invoice itself: delivery notes, timesheets, or the original order, embedded base64 in the EN 16931 attachment group (BG-24). XRechnung makes this routine, because German B2G portals transmit supporting documents inside the XML rather than as separate files. `POST /v1/extract/attachments` collects every embedded payload and returns the original files as one ZIP. An invoice without embedded attachments answers 404 rather than an empty archive, so the example treats that status as a normal outcome.

[Full example: `extract_attachments.rb`](./extract_attachments.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/attachments)

---

## Rails integration

In a Rails app, promote the calls into a service object. Faraday with `faraday-multipart` is the idiomatic client, and Rails credentials keep the key out of environment-variable sprawl:

```ruby
# Gemfile
gem "faraday"
gem "faraday-multipart"

# app/services/xrechnung_service.rb
class XrechnungService
  BASE_URL = "https://api.invoicexml.com"

  def create(invoice_attributes)
    response = connection.post("/v1/create/xrechnung") do |req|
      req.headers["Content-Type"] = "application/json"
      req.body = JSON.generate(invoice: invoice_attributes, options: { syntax: "ubl" })
    end
    raise XrechnungError, response.body unless response.success?
    response.body
  end

  def validate(xml)
    part = Faraday::Multipart::FilePart.new(
      StringIO.new(xml), "application/xml", "invoice.xml"
    )
    JSON.parse(connection.post("/v1/validate/xrechnung", { file: part }).body)
  end

  private

  def connection
    @@connection ||= Faraday.new(url: BASE_URL) do |f|
      f.request :multipart
      f.headers["Authorization"] =
        "Bearer #{Rails.application.credentials.dig(:invoicexml, :api_key)}"
    end
  end
end

class XrechnungError < StandardError; end
```

Run generation in an ActiveJob rather than a request cycle, store the returned XML with ActiveStorage, and retry only transport failures and 5xx responses: an HTTP 400 carries validation findings about your data and will not change on resend. Store the Leitweg-ID on the buyer record and treat a missing one as a data error before the job even enqueues.

---

## Common issues

- **`KeyError: key not found: "INVOICEXML_API_KEY"`**: the environment variable is not set in the shell running the script. Export it first (see [Get your API key](#get-your-api-key)), and remember that `ENV.fetch` reads the environment of the current process, so set it in the same terminal session or in your process manager.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting the variable to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`BR-DE-15` on Validate**: the Leitweg-ID is missing. It goes in `invoice.buyerReference`, and the buyer assigns it during onboarding: it is not derivable from any document you hold.
- **`BR-DE-2` / `BR-DE-3` and friends**: the seller contact group (name, phone, email) or an electronic address is missing. XRechnung makes these mandatory where base EN 16931 leaves them optional.
- **Validate "fails" with HTTP 200**: not a failure. Valid and invalid documents both answer 200; branch on the `valid` flag in the JSON body. Non-2xx statuses mean transport problems, not rule violations.
- **Encoding trouble with umlauts**: write the returned XML with `File.binwrite` as the examples do. The API returns UTF-8 bytes, and `binwrite` stores them unchanged; `File.write` on a string tagged with a different external encoding can transcode them.
- **Multipart upload sends an empty file**: open the file in binary mode (`File.open(path, "rb")`) and call `set_form` inside the block, as the examples do, so the handle is still open when the request body is read.
- **`Net::ReadTimeout`**: AI parsing in particular can take 10 to 30 seconds for larger PDFs. The `api_http` helper sets `read_timeout = 120`; keep a generous value for `/v1/parse/json`.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [ZUGFeRD and XRechnung in Ruby: complete guide](https://www.invoicexml.com/blog/zugferd-xrechnung-ruby)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [Validate XRechnung API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Render XRechnung to PDF API reference](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)
- [Parse JSON (AI) API reference](https://www.invoicexml.com/docs/api/parse/json)
- [Extract attachments API reference](https://www.invoicexml.com/docs/api/extract/attachments)
- [Ruby Net::HTTP documentation](https://docs.ruby-lang.org/en/master/Net/HTTP.html)
- [Main repository README](../README.md)
