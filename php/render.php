<?php
// Render an XRechnung XML file into a human-readable PDF using the InvoiceXML API.
// The rendered PDF is a preview for people to read: the XML file remains the
// authoritative invoice for compliance and tax purposes.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

$ch = curl_init('https://api.invoicexml.com/v1/render/xrechnung/to/pdf');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file' => new CURLFile('invoice.xml', 'application/xml'),
        // Label language for the rendered PDF: en, de, or fr. Defaults to en.
        'language' => 'de',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);

$pdf    = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    fwrite(STDERR, "InvoiceXML API error $status: $pdf\n");
    exit(1);
}

file_put_contents('invoice-preview.pdf', $pdf);
echo "Saved invoice-preview.pdf (" . strlen($pdf) . " bytes)\n";
