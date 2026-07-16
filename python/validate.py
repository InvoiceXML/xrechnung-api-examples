# Validate an XRechnung XML file using the InvoiceXML API.
# Runs the XSD, the EN 16931 Schematron, and the KoSIT BR-DE rules.
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/validate/xrechnung
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

response = requests.post(
    "https://api.invoicexml.com/v1/validate/xrechnung",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

print(response.text)
