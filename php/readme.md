# XRechnung for PHP: InvoiceXML API Examples

PHP code samples for creating, validating, and parsing **XRechnung** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **PHP 7.0+** (PHP 8.x recommended), works in plain scripts, Laravel, Symfony, WordPress, WooCommerce, Drupal, Magento, or any other PHP framework. **Zero Composer dependencies**: uses PHP's built-in cURL extension.

For background on the XRechnung standard itself (what it is, the Leitweg-ID, legal status), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `$apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **PHP 7.0 or later** (PHP 8.2+ recommended)
- The **cURL extension** (`php-curl`), bundled with most PHP installations by default
- No Composer, no external libraries

These examples use the built-in `curl_*` functions and `CURLFile` (available since PHP 5.5), so they run on essentially any modern PHP install without `composer install`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.php`](./create.php) | Build an XRechnung 3.0 XML invoice | [`POST /v1/create/xrechnung`](https://www.invoicexml.com/docs/api/create/xrechnung) |
| [`send-email.php`](./send-email.php) | Create an XRechnung invoice and email it from your own domain | [`POST /v1/create/xrechnung/email`](https://www.invoicexml.com/docs/api/create/xrechnung/email) |
| [`validate.php`](./validate.php) | Validate an XRechnung file against the KoSIT rules | [`POST /v1/validate/xrechnung`](https://www.invoicexml.com/docs/api/validate/xrechnung) |
| [`extract-json.php`](./extract-json.php) | Parse an XRechnung XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ai-convert.php`](./ai-convert.php) | (Experimental) Convert a plain PDF to XRechnung with AI | [`POST /v1/transform/to/xrechnung`](https://www.invoicexml.com/docs/api/transform/to/xrechnung) |
| [`render.php`](./render.php) | Render XRechnung XML into a human-readable PDF | [`POST /v1/render/xrechnung/to/pdf`](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf) |

Each file is standalone and runnable with `php create.php`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `$apiKey` is already defined. When in doubt, copy the complete file.

---

## Create an XRechnung invoice in PHP

```php
$payload = [
    'invoice' => [
        'invoiceNumber' => 'XR-2026-001',
        'issueDate'     => '2026-05-18',
        'currency'      => 'EUR',
        'buyerReference' => '991-12345-67',
        'seller' => [
            'name'              => 'Acme GmbH',
            'vatIdentifier'     => 'DE123456789',
            'legalRegistration' => ['identifier' => 'HRB 12345'],
            'postalAddress'     => ['line1' => 'Hauptstraße 12', 'city' => 'Berlin', 'postCode' => '10115', 'country' => 'DE'],
            'contact'           => ['name' => 'Max Mustermann', 'phone' => '+49 30 12345678', 'email' => 'billing@acme.de'],
            'electronicAddress' => ['identifier' => 'DE123456789', 'schemeId' => '9930'],
        ],
        'buyer' => [
            'name'              => 'Bundesamt für Musterverwaltung',
            'postalAddress'     => ['line1' => 'Behördenstraße 5', 'city' => 'Bonn', 'postCode' => '53113', 'country' => 'DE'],
            'electronicAddress' => ['identifier' => '991-12345-67', 'schemeId' => '0204'],
        ],
        'paymentDetails' => ['paymentAccountIdentifier' => 'DE89370400440532013000'],
        'lines' => [[
            'quantity'       => 10,
            'priceDetails'   => ['netPrice' => 150.00],
            'vatInformation' => ['rate' => 19.00],
            'item'           => ['name' => 'Senior consulting'],
        ]],
    ],
    'options' => ['syntax' => 'ubl'],
];

$ch = curl_init('https://api.invoicexml.com/v1/create/xrechnung');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => json_encode($payload),
    CURLOPT_HTTPHEADER     => [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $apiKey,
    ],
]);
$xml = curl_exec($ch);
file_put_contents('invoice-xrechnung.xml', $xml);
```

`buyerReference` carries the Leitweg-ID (BT-10), and the seller `contact` and `electronicAddress` groups are what the XRechnung CIUS requires on top of plain EN 16931. Omit any of them and the API returns a 400 naming the `BR-DE-*` rule you missed.

The response is the XRechnung 3.0 XML document, validated against the KoSIT rules before delivery.

[Full example: `create.php`](./create.php) | [API reference](https://www.invoicexml.com/docs/api/create/xrechnung)

---

## Validate an XRechnung file in PHP

```php
$ch = curl_init('https://api.invoicexml.com/v1/validate/xrechnung');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file' => new CURLFile('invoice.xml', 'application/xml'),
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$report = curl_exec($ch);
echo $report;
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the German BR-DE-* rules).

[Full example: `validate.php`](./validate.php) | [API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)

---

## Extract XRechnung data as JSON in PHP

Useful for feeding XRechnung invoices into REST APIs, ERPs, accounting systems, or any pipeline that prefers JSON over XML.

```php
$ch = curl_init('https://api.invoicexml.com/v1/extract/json');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => ['file' => new CURLFile('invoice.xml', 'application/xml')],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$json = curl_exec($ch);
$data = json_decode($json, true);
// The invoice document sits under the "invoice" key of the response.
echo $data['invoice']['seller']['name'];
```

[Full example: `extract-json.php`](./extract-json.php) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

---

## (Experimental) Convert a plain PDF to XRechnung with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before submitting it to a public authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-xrechnung-conversion-experimental).

The endpoint takes the PDF plus a `buyerReference` form field: the Leitweg-ID cannot be inferred from the source document, so you must supply it.

```php
$ch = curl_init('https://api.invoicexml.com/v1/transform/to/xrechnung');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file'           => new CURLFile('plain-invoice.pdf', 'application/pdf'),
        'buyerReference' => '991-12345-67',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$xml = curl_exec($ch);
file_put_contents('converted-xrechnung.xml', $xml);
```

[Full example: `ai-convert.php`](./ai-convert.php) | [API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)

---

## Render XRechnung as a readable PDF in PHP

XRechnung has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```php
$ch = curl_init('https://api.invoicexml.com/v1/render/xrechnung/to/pdf');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file'     => new CURLFile('invoice.xml', 'application/xml'),
        'language' => 'de',   // en, de, or fr
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$pdf = curl_exec($ch);
file_put_contents('invoice-preview.pdf', $pdf);
```

[Full example: `render.php`](./render.php) | [API reference](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)

---

## Framework integration

### Laravel

Return an XRechnung invoice from a controller or route closure:

```php
use Illuminate\Support\Facades\Route;

Route::get('/invoices/{id}/xrechnung', function ($id) {
    $pdf = app(XRechnungService::class)->create($id);
    return response($pdf, 200, [
        'Content-Type'        => 'application/pdf',
        'Content-Disposition' => "attachment; filename=\"invoice-{$id}.pdf\"",
    ]);
});
```

Store the API key in `config/services.php` and read it via `config('services.invoicexml.key')`.

### Symfony

```php
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class InvoiceController
{
    #[Route('/invoices/{id}/xrechnung', name: 'xrechnung_download')]
    public function download(int $id): Response
    {
        $pdf = $this->xrechnungService->create($id);
        return new Response($pdf, 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => "attachment; filename=\"invoice-{$id}.pdf\"",
        ]);
    }
}
```

### WordPress and WooCommerce

Hook into WooCommerce order lifecycle events to generate XRechnung invoices automatically:

```php
add_action('woocommerce_order_status_completed', function ($order_id) {
    $order = wc_get_order($order_id);
    $pdf = create_xrechnung_from_order($order);
    file_put_contents(WP_CONTENT_DIR . "/invoices/{$order_id}.pdf", $pdf);
});
```

The same pattern works in Magento, Drupal Commerce, and PrestaShop.

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `$apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`Class "CURLFile" not found`**: PHP cURL extension is missing. Install with `apt-get install php-curl` (Debian/Ubuntu), `yum install php-curl` (RHEL), or enable the extension in `php.ini`.
- **`SSL certificate problem` errors**: the server's CA bundle is out of date. Update OpenSSL on the host, or set `CURLOPT_CAINFO` to a current `cacert.pem` from curl.se/ca/.
- **BR-DE-* failures on Validate**: an XRechnung-specific field is missing. The most common are BR-DE-15 (no Leitweg-ID in `buyerReference`), BR-DE-2 (no seller contact group), and BR-DE-1 (no seller electronic address).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [Validate XRechnung API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to XRechnung API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)
- [PHP cURL documentation](https://www.php.net/manual/en/book.curl.php)
- [Main repository README](../README.md)
