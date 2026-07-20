# Render an XRechnung XML file into a human-readable PDF using the InvoiceXML API.
# The rendered PDF is a preview for people to read: the XML file remains the
# authoritative invoice for compliance and tax purposes.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

response = requests.post(
    "https://api.invoicexml.com/v1/render/xrechnung/to/pdf",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
    # Label language for the rendered PDF: en, de, or fr. Defaults to en.
    data={"language": "de"},
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("invoice-preview.pdf", "wb") as f:
    f.write(response.content)

print(f"Saved invoice-preview.pdf ({len(response.content)} bytes)")
