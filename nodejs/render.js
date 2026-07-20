// Render an XRechnung XML file into a human-readable PDF using the InvoiceXML API.
// The rendered PDF is a preview for people to read: the XML file remains the
// authoritative invoice for compliance and tax purposes.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('file', new Blob([fs.readFileSync('invoice.xml')], { type: 'application/xml' }), 'invoice.xml');
    // Label language for the rendered PDF: en, de, or fr. Defaults to en.
    form.append('language', 'de');

    const response = await fetch('https://api.invoicexml.com/v1/render/xrechnung/to/pdf', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${await response.text()}`);
        process.exit(1);
    }

    const buffer = Buffer.from(await response.arrayBuffer());
    fs.writeFileSync('invoice-preview.pdf', buffer);
    console.log(`Saved invoice-preview.pdf (${buffer.length} bytes)`);
})();
