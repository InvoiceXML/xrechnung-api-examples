# XRechnung for Python: InvoiceXML API Examples

Python code samples for creating, validating, and parsing **XRechnung** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Python 3.7+** (3.10+ recommended). Runs in Django, Flask, FastAPI, Pandas pipelines, Jupyter notebooks, AWS Lambda, Google Cloud Functions, Azure Functions, or plain scripts.

For background on the XRechnung standard itself (what it is, the Leitweg-ID, legal status), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `api_key` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Python 3.7 or later** (3.10+ recommended)
- The **`requests`** library

```bash
pip install requests
```

Or with [uv](https://github.com/astral-sh/uv) (the modern alternative):

```bash
uv pip install requests
```

`requests` is the standard HTTP client for Python: clean multipart support, automatic JSON decoding, and the most familiar API in the ecosystem. If you prefer the modern async alternative, `httpx` translates almost line-for-line.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.py`](./create.py) | Build an XRechnung 3.0 XML invoice | [`POST /v1/create/xrechnung`](https://www.invoicexml.com/docs/api/create/xrechnung) |
| [`validate.py`](./validate.py) | Validate an XRechnung file against the KoSIT rules | [`POST /v1/validate/xrechnung`](https://www.invoicexml.com/docs/api/validate/xrechnung) |
| [`extract_json.py`](./extract_json.py) | Parse an XRechnung XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ai_convert.py`](./ai_convert.py) | (Experimental) Convert a plain PDF to XRechnung with AI | [`POST /v1/transform/to/xrechnung`](https://www.invoicexml.com/docs/api/transform/to/xrechnung) |

Each file is standalone and runnable with `python create.py`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `api_key` is already defined. When in doubt, copy the complete file.

---

## Create an XRechnung invoice in Python

```python
import requests

payload = {
    "invoice": {
        "invoiceNumber": "XR-2026-001",
        "issueDate":     "2026-05-18",
        "currency":      "EUR",
        "buyerReference": "991-12345-67",
        "seller": {
            "name":              "Acme GmbH",
            "vatIdentifier":     "DE123456789",
            "legalRegistration": {"identifier": "HRB 12345"},
            "postalAddress": {"line1": "Hauptstraße 12", "city": "Berlin", "postCode": "10115", "country": "DE"},
            "contact": {"name": "Max Mustermann", "phone": "+49 30 12345678", "email": "billing@acme.de"},
            "electronicAddress": {"identifier": "DE123456789", "schemeId": "9930"},
        },
        "buyer": {
            "name": "Bundesamt für Musterverwaltung",
            "postalAddress": {"line1": "Behördenstraße 5", "city": "Bonn", "postCode": "53113", "country": "DE"},
            "electronicAddress": {"identifier": "991-12345-67", "schemeId": "0204"},
        },
        "paymentDetails": {"paymentAccountIdentifier": "DE89370400440532013000"},
        "lines": [{
            "quantity":       10,
            "priceDetails":   {"netPrice": 150.00},
            "vatInformation": {"rate": 19.00},
            "item":           {"name": "Senior consulting"},
        }],
    },
    "options": {"version": "3.0"},
}

response = requests.post(
    "https://api.invoicexml.com/v1/create/xrechnung",
    headers={"Authorization": f"Bearer {api_key}"},
    json=payload,
)

with open("invoice-xrechnung.xml", "w", encoding="utf-8") as f:
    f.write(response.text)
```

`buyerReference` carries the Leitweg-ID (BT-10), and the seller `contact` and `electronicAddress` groups are what the XRechnung CIUS requires on top of plain EN 16931. Omit any of them and the API returns a 400 naming the `BR-DE-*` rule you missed.

The response is the XRechnung 3.0 XML document, validated against the KoSIT rules before delivery.

[Full example: `create.py`](./create.py) | [API reference](https://www.invoicexml.com/docs/api/create/xrechnung)

---

## Validate an XRechnung file in Python

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/validate/xrechnung",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
)
print(response.text)
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the German BR-DE-* rules).

[Full example: `validate.py`](./validate.py) | [API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)

---

## Extract XRechnung data as JSON in Python

Useful for Pandas pipelines, Django/Flask models, or any system that prefers JSON over XML.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/extract/json",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
)

invoice = response.json()
# The invoice fields sit at the root of the response.
print(invoice["seller"]["name"], invoice["totals"]["grandTotalAmount"])
```

[Full example: `extract_json.py`](./extract_json.py) | [API reference](https://www.invoicexml.com/docs/api/extract/json)

---

## (Experimental) Convert a plain PDF to XRechnung with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before submitting it to a public authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-xrechnung-conversion-experimental).

The endpoint takes the PDF plus a `buyerReference` form field: the Leitweg-ID cannot be inferred from the source document, so you must supply it.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/transform/to/xrechnung",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("plain-invoice.pdf", open("plain-invoice.pdf", "rb"), "application/pdf")},
    data={"buyerReference": "991-12345-67"},
)

with open("converted-xrechnung.xml", "w", encoding="utf-8") as f:
    f.write(response.text)
```

[Full example: `ai_convert.py`](./ai_convert.py) | [API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)

---

## Framework integration

### Django

Return an XRechnung invoice from a view:

```python
from django.http import HttpResponse

def download_xrechnung(request, invoice_id):
    pdf_bytes = create_xrechnung_for(invoice_id)
    response = HttpResponse(pdf_bytes, content_type="application/pdf")
    response["Content-Disposition"] = f'attachment; filename="invoice-{invoice_id}.pdf"'
    return response
```

Store the API key in `settings.py` via `INVOICEXML_API_KEY = os.environ["INVOICEXML_API_KEY"]`.

### Flask

```python
from flask import send_file
from io import BytesIO

@app.route("/invoices/<int:invoice_id>/xrechnung")
def get_xrechnung(invoice_id):
    pdf_bytes = create_xrechnung_for(invoice_id)
    return send_file(BytesIO(pdf_bytes), mimetype="application/pdf",
                     as_attachment=True, download_name=f"invoice-{invoice_id}.pdf")
```

### FastAPI

```python
from fastapi.responses import Response

@app.get("/invoices/{invoice_id}/xrechnung")
async def get_xrechnung(invoice_id: int):
    pdf_bytes = create_xrechnung_for(invoice_id)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="invoice-{invoice_id}.pdf"'},
    )
```

### Pandas and data pipelines

The `extract_json.py` example fits naturally into a Pandas pipeline: walk a folder of XRechnung PDFs, extract each to JSON, and load into a DataFrame for analysis or bulk archival.

```python
import pandas as pd, requests, os

rows = []
for pdf in os.listdir("invoices/"):
    with open(f"invoices/{pdf}", "rb") as f:
        data = requests.post(
            "https://api.invoicexml.com/v1/extract/json",
            headers={"Authorization": f"Bearer {api_key}"},
            files={"file": (pdf, f, "application/pdf")},
        ).json()
    rows.append(data)

df = pd.DataFrame(rows)
```

### AWS Lambda

The `requests` library works in Lambda directly. Include it via Lambda layers or in your deployment zip.

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `api_key` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`SSL: CERTIFICATE_VERIFY_FAILED` on macOS**: run `/Applications/Python\ 3.x/Install\ Certificates.command` to install Python's CA bundle. Do not disable SSL verification in production.
- **`ConnectionError` or timeout**: add an explicit `timeout=30` parameter to `requests.post()`. AI conversion in particular can take 10 to 30 seconds for larger PDFs.
- **File handle warnings**: the examples use inline `open()` for brevity. For production code, prefer `with open(...) as f:` to ensure file handles close deterministically.
- **BR-DE-* failures on Validate**: an XRechnung-specific field is missing. The most common are BR-DE-15 (no Leitweg-ID in `buyerReference`), BR-DE-2 (no seller contact group), and BR-DE-1 (no seller electronic address).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create XRechnung API reference](https://www.invoicexml.com/docs/api/create/xrechnung)
- [Validate XRechnung API reference](https://www.invoicexml.com/docs/api/validate/xrechnung)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to XRechnung API reference](https://www.invoicexml.com/docs/api/transform/to/xrechnung)
- [requests library documentation](https://requests.readthedocs.io/)
- [Main repository README](../README.md)
