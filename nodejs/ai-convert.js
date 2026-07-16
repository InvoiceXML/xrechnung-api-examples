// Experimental: convert a plain PDF invoice to XRechnung using AI.
// Review output manually before sending to a customer or tax authority.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/transform/to/xrechnung
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('file', new Blob([fs.readFileSync('plain-invoice.pdf')], { type: 'application/pdf' }), 'plain-invoice.pdf');
    // The Leitweg-ID (BT-10) is required: it identifies the German public authority
    // you are invoicing and cannot be inferred from the PDF.
    form.append('buyerReference', '991-12345-67');

    const response = await fetch('https://api.invoicexml.com/v1/transform/to/xrechnung', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    const xml = await response.text();
    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${xml}`);
        process.exit(1);
    }

    fs.writeFileSync('converted-xrechnung.xml', xml);
    console.log(`Saved converted-xrechnung.xml (${xml.length} chars)`);
    console.log('Review the output manually before sending.');
})();
