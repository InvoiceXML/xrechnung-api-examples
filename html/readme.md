# XRechnung HTML Invoice Form: InvoiceXML API Example

A complete, self-contained HTML invoice builder that calls the **[InvoiceXML API](https://www.invoicexml.com)** to create an **XRechnung 3.0** XML document (EN 16931 compliant, KoSIT validated) and demonstrates how to map server-side validation errors back onto the form fields.

The bare-minimum browser examples live in [`/javascript`](../javascript). This folder shows the next step up: an actual UI with structured error handling, ready to drop into a prototype or internal tool.

For background on the XRechnung standard itself, see the [main repository README](../README.md).

## What this example shows

- A working invoice form (seller, buyer, line item, payment) styled with Tailwind via CDN, no build step.
- A client-side `Authorization: Bearer <token>` flow with a visible API token field so you can test from your own dashboard token.
- The full RFC 7807 Problem Details error-handling path the API returns on `4001` (Schematron) and `4002` (model validation) responses.
- A one-line trick to map every API error onto its form input: **input `name` attributes are the API's JSON paths**, so a `querySelector` is all you need.

## File

| File | Description |
|---|---|
| [`create-xrechnung-form.html`](./create-xrechnung-form.html) | Self-contained invoice builder with Tailwind styling, vanilla JS, API token input, and validation-error mapping. |

Open the file in any modern browser, paste your API token, and click **Generate & download XML**. The form is pre-filled with a valid sample invoice, including a Leitweg-ID and the seller contact and electronic address groups the XRechnung CIUS requires, so the first click should produce a downloadable XML immediately.

## Why input `name=` matches the API JSON path

The InvoiceXML create endpoint accepts a JSON payload like:

```json
{
    "invoice": {
        "seller": {
            "postalAddress": { "line1": "Hauptstraße 12", "city": "Berlin" }
        },
        "lines": [
            { "priceDetails": { "netPrice": 150.00 } }
        ]
    }
}
```

Every form input in `create-xrechnung-form.html` is named with the dotted path to its target field:

```html
<input name="seller.postalAddress.line1" ...>
<input name="seller.postalAddress.city"  ...>
<input name="lines[0].priceDetails.netPrice" ...>
```

This mirroring drives the two key behaviors:

1. **Building the payload** is a single loop over `form.querySelectorAll('[name]')`, writing each value into a nested object via the dotted path. No per-field mapping table.
2. **Mapping errors back** is also direct. When the API returns a validation failure, each finding includes a `fields` array of the same dotted paths. Highlighting the offending input is `form.querySelector('[name="' + path + '"]')`.

## API error shape

A failed `POST /v1/create/xrechnung` returns RFC 7807 JSON. For validation errors (`errorCode` 4001 or 4002), the body looks like:

```json
{
    "type":   "https://www.invoicexml.com/errors/4002",
    "title":  "Model validation failed",
    "status": 400,
    "errorCode": 4002,
    "errors": [
        {
            "message": "Seller name (BT-27) is required.",
            "fields":  ["seller.name"]
        },
        {
            "message": "Line 1 net price (BT-146) must be greater than zero.",
            "fields":  ["lines[0].priceDetails.netPrice"]
        }
    ]
}
```

The example walks `problem.errors`, lists every `message` in a red banner at the top of the page, and adds a red border plus inline error text on each field referenced by `finding.fields`.

For non-validation errors (`401`, `403`, `429`, `5xx`), the example shows `problem.title` and `problem.detail` in a single status banner. No field highlighting is attempted because the failure isn't tied to a specific input.

## The error-mapping loop

The interesting part of the JavaScript, in full:

```js
function handleApiError(httpStatus, problem) {
    const code = problem.errorCode;

    if ((code === 4001 || code === 4002) && Array.isArray(problem.errors)) {
        errorBanner.classList.remove('hidden');
        problem.errors.forEach(finding => {
            // 1. Show the human message in the banner.
            const li = document.createElement('li');
            li.textContent = finding.message;
            errorList.appendChild(li);

            // 2. Highlight every input the finding points at.
            (finding.fields || []).forEach(path => {
                const el = form.querySelector('[name="' + path + '"]');
                if (!el) return;
                el.closest('.field').classList.add('has-error');
            });
        });
        return;
    }

    // Auth / quota / server errors: title + detail only.
    showStatus('error', (problem.title || 'HTTP ' + httpStatus)
                      + ': ' + (problem.detail || 'Request failed.'));
}
```

That's the whole contract. The rest of the file is form markup and a small `setDeep` helper that turns `lines[0].priceDetails.netPrice` into nested objects when building the request body.

## Requirements

- Any modern browser: Chrome 95+, Firefox 90+, Safari 14+, Edge 95+.
- An InvoiceXML API token. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication). Paste the raw token only, without the `Bearer ` prefix; the form adds the prefix itself.
- No server, no build step, no npm install. Open the file directly via `file://` or serve it with `python -m http.server` / `npx serve` if you prefer.

## Security warning

The API token is entered into a browser input and sent from the browser. That's fine for **local prototyping**, **internal tools**, or **single-user desktop apps** (Electron, Tauri). For **public-facing production sites**, route requests through your own backend so the token stays server-side. See the [Node.js examples](../nodejs) or [PHP examples](../php) for server-side patterns.

## Adapting the example

- **Add more fields**: copy any input, set its `name` to the API JSON path you want to populate, and you're done. No JS change required.
- **Add more line items**: increase the index in `lines[N].…` (e.g. `lines[1].item.name`) and add `lines[1].…` to the `NUMERIC_NAMES` set inside the script so numeric fields are coerced. Or wire up an "Add line" button that clones the line markup with the next index.
- **Change styling**: every visible class is Tailwind. Swap the colors, spacing, or layout without touching the JS.
- **Replace the API token field with a server proxy**: drop the token input, change `API_URL` to your own endpoint (e.g. `/api/xrechnung/create`), and remove the `Authorization` header. The error-mapping pattern is unchanged as long as your proxy forwards the API's Problem Details response verbatim.

## Resources

- [Get an InvoiceXML API token](https://www.invoicexml.com/account/authentication)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [XRechnung creation API reference](https://www.invoicexml.com/docs/api/create/xrechnung) (the InvoiceXML web app uses the identical field-name / error-mapping approach)
- [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807)
- [Main repository README](../README.md)
