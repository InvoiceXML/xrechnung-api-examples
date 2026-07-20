# XRechnung for JavaScript in the Browser: InvoiceXML API Examples

Browser-side JavaScript examples for creating, validating, and parsing **XRechnung** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Self-contained HTML files using vanilla JavaScript with native `fetch` and `FormData`. Compatible with **modern browsers**: Chrome, Firefox, Safari, Edge. **No build step, no framework, no dependencies.**

For background on the XRechnung standard itself (what it is, the Leitweg-ID, legal status), see the [main repository README](../README.md).

## ⚠️ Security warning: API keys in browser code

API keys embedded directly in browser-side JavaScript are visible to every visitor through View Source or DevTools. Anyone can extract the key and use it against your InvoiceXML quota.

These examples are appropriate for:

- **Local prototyping** and testing
- **Internal tools** behind your company's authentication
- **Single-user desktop apps** wrapped in Electron or Tauri

For **public-facing production websites**, route requests through your own backend so the API key stays server-side. See the [Node.js examples](../nodejs) or [PHP examples](../php) for server-side integration patterns.

## Get your API key

Sign up and generate a key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Set `apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- Any modern browser:
  - Chrome 95+
  - Firefox 90+
  - Safari 14+
  - Edge 95+
- An InvoiceXML API key
- Optional: a local web server (`python -m http.server`, `npx serve`, etc.) for testing. The examples also work by opening the `.html` files directly via `file://`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.html`](./create.html) | Build an XRechnung 3.0 XML invoice | [`POST /v1/create/xrechnung`](https://www.invoicexml.com/docs/api/create/xrechnung) |
| [`validate.html`](./validate.html) | Validate an XRechnung file against the KoSIT rules | [`POST /v1/validate/xrechnung`](https://www.invoicexml.com/docs/api/validate/xrechnung) |
| [`extract-json.html`](./extract-json.html) | Parse an XRechnung XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ai-convert.html`](./ai-convert.html) | (Experimental) Convert a plain PDF to XRechnung with AI | [`POST /v1/transform/to/xrechnung`](https://www.invoicexml.com/docs/api/transform/to/xrechnung) |
| [`render.html`](./render.html) | Render XRechnung XML into a human-readable PDF | [`POST /v1/render/xrechnung/to/pdf`](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf) |

Each file is a complete, runnable HTML page. Open it in a browser, paste your API key into the `<script>` block, and click the button.

---

## Create an XRechnung invoice in the browser

```html
<button id="run">Generate invoice</button>
<script>
const payload = {
    invoice: {
        invoiceNumber: 'XR-2026-001',
        issueDate:     '2026-05-18',
        currency:      'EUR',
        buyerReference: '991-12345-67',   // the Leitweg-ID (BT-10)
        seller: {
            name: 'Acme GmbH', vatIdentifier: 'DE123456789',
            contact: { name: 'Max Mustermann', phone: '+49 30 12345678', email: 'billing@acme.de' },
            electronicAddress: { identifier: 'DE123456789', schemeId: '9930' }, /* ... */
        },
        buyer: {
            name: 'Bundesamt für Musterverwaltung',
            electronicAddress: { identifier: '991-12345-67', schemeId: '0204' }, /* ... */
        },
        paymentDetails: { paymentAccountIdentifier: 'DE89370400440532013000' },
        lines: [{ quantity: 10, priceDetails: { netPrice: 150.00 },
                  vatInformation: { rate: 19.00 }, item: { name: 'Senior consulting' } }],
    },
    options: { version: '3.0' },
};

document.getElementById('run').addEventListener('click', async () => {
    const response = await fetch('https://api.invoicexml.com/v1/create/xrechnung', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + apiKey,
        },
        body: JSON.stringify(payload),
    });
    const xml = await response.text();
    const blob = new Blob([xml], { type: 'application/xml' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'invoice-xrechnung.xml';
    a.click();
});
</script>
```

The XML is downloaded directly to the user's machine via `URL.createObjectURL` and a triggered `<a download>`. `buyerReference` carries the Leitweg-ID (BT-10), and the seller `contact` and `electronicAddress` groups are what the XRechnung CIUS requires on top of plain EN 16931.

[Full example: `create.html`](./create.html) | [API reference](https://www.invoicexml.com/docs/api/create/xrechnung)

---

## Validate an XRechnung file in the browser

```html
<input type="file" id="file" accept=".xml,application/xml,text/xml">
<button id="run">Validate</button>
<pre id="output"></pre>

<script>
document.getElementById('run').addEventListener('click', async () => {
    const file = document.getElementById('file').files[0];
    const form = new FormData();
    form.append('file', file);

    const response = await fetch('https://api.invoicexml.com/v1/validate/xrechnung', {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + apiKey },
        body: form,
    });
    document.getElementById('output').textContent = await response.text();
});
</script>
```

The file is read directly from the `<input type="file">` element. No FileReader or Blob conversion needed: `FormData.append(file)` accepts the `File` object directly.

[Full example: `validate.html`](./validate.html) | [API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)

---

## Extract XRechnung data as JSON in the browser

Useful for displaying invoice details in a web UI, populating form fields from an uploaded XRechnung file, or building drag-and-drop invoice review tools.

```js
const form = new FormData();
form.append('file', fileInput.files[0]);

const response = await fetch('https://api.invoicexml.com/v1/extract/json', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});
const { invoice } = await response.json();
// The invoice document sits under the "invoice" key of the response.
console.log(invoice.seller.name, invoice.totals.grandTotalAmount);
```

[Full example: `extract-json.html`](./extract-json.html) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)


---

## (Experimental) Convert a plain PDF to XRechnung with AI

> **Experimental feature. Human verification required before any production use.**
>
> AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting. Always review the output PDF before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-xrechnung-conversion-experimental).

[Full example: `ai-convert.html`](./ai-convert.html) | [API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)

---

## Render XRechnung as a readable PDF in the browser

XRechnung has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```js
const form = new FormData();
form.append('file', fileInput.files[0]);
form.append('language', 'de');   // en, de, or fr

const response = await fetch('https://api.invoicexml.com/v1/render/xrechnung/to/pdf', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});

const blob = await response.blob();
const a = document.createElement('a');
a.href = URL.createObjectURL(blob);
a.download = 'invoice-preview.pdf';
a.click();
```

[Full example: `render.html`](./render.html) | [API reference](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf)

---

## Framework integration

The patterns below assume the API key is held server-side and proxied through your backend. Replace `/api/xrechnung/...` with your own proxy endpoints.

### React

```jsx
function CreateXRechnungButton() {
    const [status, setStatus] = useState('');

    async function handleClick() {
        setStatus('Generating...');
        const response = await fetch('/api/xrechnung/create', { method: 'POST' });
        const blob = await response.blob();
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'invoice-xrechnung.xml';
        a.click();
        setStatus('Done');
    }

    return <button onClick={handleClick}>Generate invoice</button>;
}
```

### Vue 3

```vue
<template>
    <button @click="handleClick">Generate invoice</button>
    <p>{{ status }}</p>
</template>

<script setup>
import { ref } from 'vue';

const status = ref('');

async function handleClick() {
    status.value = 'Generating...';
    const response = await fetch('/api/xrechnung/create', { method: 'POST' });
    const blob = await response.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'invoice-xrechnung.xml';
    a.click();
    status.value = 'Done';
}
</script>
```

### Svelte

```svelte
<script>
    let status = '';

    async function handleClick() {
        status = 'Generating...';
        const response = await fetch('/api/xrechnung/create', { method: 'POST' });
        const blob = await response.blob();
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'invoice-xrechnung.xml';
        a.click();
        status = 'Done';
    }
</script>

<button on:click={handleClick}>Generate invoice</button>
<p>{status}</p>
```

### Angular

Use `HttpClient` with `responseType: 'blob'`:

```typescript
this.http.post('/api/xrechnung/create', payload, { responseType: 'blob' })
    .subscribe(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'invoice-xrechnung.xml';
        a.click();
    });
```

---

## Common issues

- **CORS errors in the browser console**: if the browser blocks the request, route through your own backend (proxy) instead of calling the API directly from the browser.
- **API key visible in DevTools**: this is expected when the key is embedded in client-side code. For anything beyond local prototyping, move the call to a server-side proxy.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication). A frequent cause: setting `apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **File input always empty**: `fileInput.files[0]` is `undefined` until the user actually picks a file. Always check before reading.
- **Mixed content warnings**: if your page is served over `https://`, all API calls must also be `https://`. The examples use `https://api.invoicexml.com` so this is fine, just do not change it to `http://`.
- **`file://` protocol limitations**: opening the HTML directly via `file://` works for these examples but breaks some browser APIs in stricter modes. Use a local server (`python -m http.server`, `npx serve`) if you hit issues.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [Validate XRechnung API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to XRechnung API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)
- [MDN: Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
- [MDN: FormData](https://developer.mozilla.org/en-US/docs/Web/API/FormData)
- [Main repository README](../README.md)
