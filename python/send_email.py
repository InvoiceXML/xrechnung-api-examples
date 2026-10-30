# Create an XRechnung invoice and email it with the InvoiceXML API, in one call.
# Builds and validates the invoice exactly like POST /v1/create/xrechnung, then
# sends it as an attachment from your own domain. This sends a real email.
# The answer is the message id of the email, not the file.
#
# Before the first run, add and verify your domain:
# https://www.invoicexml.com/account/sending-domains
# Your plan must include email delivery: https://www.invoicexml.com/pricing
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/create/xrechnung/email
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
    "email": {
        "from":     "invoices@yourcompany.com",  # any address on your verified domain
        "fromName": "Acme Billing",
        "to":       "you@yourcompany.com",       # send the first test to yourself
        "subject":  "Invoice XR-2026-001",
        "message":  "Hello,\n\nPlease find attached invoice XR-2026-001.\n\nKind regards",
        # Optional: cc and bcc (2 addresses together) and replyTo.
    },
}

response = requests.post(
    "https://api.invoicexml.com/v1/create/xrechnung/email",
    headers={"Authorization": f"Bearer {api_key}"},
    json=payload,
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

print(f"Email accepted for delivery, message id {response.json()['messageId']}")
