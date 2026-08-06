# Create an XRechnung 3.0 invoice using the InvoiceXML API.
# Sends a JSON invoice model and receives the XRechnung XML.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/create/xrechnung
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

payload = {
    "invoice": {
        "invoiceNumber": "XR-2026-001",
        "issueDate":     "2026-05-18",
        "currency":      "EUR",
        # The Leitweg-ID (BT-10), required for German B2G invoicing.
        "buyerReference": "991-12345-67",
        "seller": {
            "name":              "Acme GmbH",
            "vatIdentifier":     "DE123456789",
            "legalRegistration": {"identifier": "HRB 12345"},
            "postalAddress": {
                "line1":    "Hauptstraße 12",
                "city":     "Berlin",
                "postCode": "10115",
                "country":  "DE",
            },
            # XRechnung requires the seller contact group and an electronic address.
            "contact": {
                "name":  "Max Mustermann",
                "phone": "+49 30 12345678",
                "email": "billing@acme.de",
            },
            "electronicAddress": {"identifier": "DE123456789", "schemeId": "9930"},
        },
        "buyer": {
            "name": "Bundesamt für Musterverwaltung",
            "postalAddress": {
                "line1":    "Behördenstraße 5",
                "city":     "Bonn",
                "postCode": "53113",
                "country":  "DE",
            },
            "electronicAddress": {"identifier": "991-12345-67", "schemeId": "0204"},
        },
        "paymentDetails": {"paymentAccountIdentifier": "DE89370400440532013000"},
        "lines": [
            {
                "quantity":       10,
                "priceDetails":   {"netPrice": 150.00},
                "vatInformation": {"rate": 19.00},
                "item":           {"name": "Senior consulting"},
            },
        ],
    },
    "options": {"syntax": "ubl"},
}

response = requests.post(
    "https://api.invoicexml.com/v1/create/xrechnung",
    headers={"Authorization": f"Bearer {api_key}"},
    json=payload,
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("invoice-xrechnung.xml", "w", encoding="utf-8") as f:
    f.write(response.text)

print(f"Saved invoice-xrechnung.xml ({len(response.text)} chars)")
