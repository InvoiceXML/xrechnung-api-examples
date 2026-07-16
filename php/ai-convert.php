<?php
// Experimental: convert a plain PDF invoice to XRechnung using AI.
// Review output manually before sending to a customer or tax authority.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/transform/to/xrechnung

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

$ch = curl_init('https://api.invoicexml.com/v1/transform/to/xrechnung');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file' => new CURLFile('plain-invoice.pdf', 'application/pdf'),
        // The Leitweg-ID (BT-10) is required: it identifies the German public
        // authority you are invoicing and cannot be inferred from the PDF.
        'buyerReference' => '991-12345-67',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);

$xml    = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    fwrite(STDERR, "InvoiceXML API error $status: $xml\n");
    exit(1);
}

file_put_contents('converted-xrechnung.xml', $xml);
echo "Saved converted-xrechnung.xml (" . strlen($xml) . " bytes)\n";
echo "Review the output manually before sending.\n";
