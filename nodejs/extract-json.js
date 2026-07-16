// Extract XRechnung invoice data as JSON using the InvoiceXML API.
// Upload the XRechnung XML and receive the EN 16931 model as JSON.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/extract/json
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('file', new Blob([fs.readFileSync('invoice.xml')], { type: 'application/xml' }), 'invoice.xml');

    const response = await fetch('https://api.invoicexml.com/v1/extract/json', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    const json = await response.text();
    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${json}`);
        process.exit(1);
    }

    fs.writeFileSync('invoice.json', json);
    console.log(json);
})();
