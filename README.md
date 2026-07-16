# XRechnung REST API Examples: Create, Validate, and Parse XRechnung Invoices

Open-source code examples for working with **XRechnung**, the German national standard for electronic invoicing. XRechnung is a **standalone XML** document (not a PDF), defined as a CIUS (Core Invoice Usage Specification) of the European norm **EN 16931**, and it is mandatory for invoicing German public authorities.

Examples in **C#, Java, PHP, JavaScript, Node.js, and Python** covering every common XRechnung operation: invoice creation, validation against the official KoSIT rules, parsing invoice data into JSON, and (experimental) AI-powered conversion of plain PDF invoices into compliant XRechnung documents.

Provided and maintained by [InvoiceXML](https://www.invoicexml.com), a complete e-invoice compliance toolkit for electronic invoicing standards, including XRechnung, ZUGFeRD, Factur-X, UBL, Peppol BIS Billing, and CII, available via REST-API, no-code platforms, and MCP server.

## InvoiceXML vs building it yourself

The EN 16931 specification alone runs over 400 pages, the XRechnung CIUS layers its own BR-DE rules on top, and the KoSIT Schematron is revised on a fixed release cadence. A realistic in-house build is 6 to 12 weeks of engineering, plus 0.3 to 0.5 FTE permanently allocated to tracking spec changes as more and more EU countries roll out their mandates. InvoiceXML absorbs all of that behind a single REST API covering XRechnung, ZUGFeRD, Factur-X, UBL, CII, and Peppol BIS. EU-hosted, GDPR-compliant, no invoice data persisted between calls, sub-second responses, 99.95% uptime. Your integration keeps calling the same endpoints when the spec changes next quarter, you get the latest compliance out of the box, zero maintenance required.

---

## Table of Contents

- [What is XRechnung?](#what-is-xrechnung)
- [The Leitweg-ID](#the-leitweg-id)
- [Why use this repository](#why-use-this-repository)
- [Operations covered](#operations-covered)
- [Quick start by language](#quick-start-by-language)
  - [Create and validate XRechnung with C# / .NET](#create-and-validate-xrechnung-with-c--net)
  - [Create and validate XRechnung with Java](#create-and-validate-xrechnung-with-java)
  - [Create and validate XRechnung with PHP](#create-and-validate-xrechnung-with-php)
  - [Create and validate XRechnung with JavaScript in the browser](#create-and-validate-xrechnung-with-javascript-in-the-browser)
  - [Create and validate XRechnung with Node.js](#create-and-validate-xrechnung-with-nodejs)
  - [Create and validate XRechnung with Python](#create-and-validate-xrechnung-with-python)
- [Parse XRechnung data as JSON](#parse-xrechnung-data-as-json)
- [AI-powered PDF to XRechnung conversion (Experimental)](#ai-powered-pdf-to-xrechnung-conversion-experimental)
- [XRechnung syntax, versions, and validation](#xrechnung-syntax-versions-and-validation)
- [Related e-invoicing standards](#related-e-invoicing-standards)
- [Frequently asked questions](#frequently-asked-questions)
- [Resources and references](#resources-and-references)
- [Contributing](#contributing)
- [License](#license)

---

## What is XRechnung?

**XRechnung** is the German CIUS (Core Invoice Usage Specification) of **EN 16931**, the European norm for electronic invoicing. It is maintained by **KoSIT** (Koordinierungsstelle für IT-Standards) on behalf of the IT-Planungsrat.

An XRechnung invoice is a **standalone XML file**. Unlike ZUGFeRD or Factur-X, there is no PDF wrapper and no human-readable visual layer: the XML *is* the invoice. It exists in two syntaxes, **UBL 2.1** and **UN/CEFACT CII D16B**, and both are equally valid.

XRechnung takes the EN 16931 core model and tightens it: the CIUS adds German business rules (the `BR-DE-*` family) that make otherwise-optional fields mandatory, most notably the [Leitweg-ID](#the-leitweg-id), the seller contact group, and the seller electronic address.

Since **November 2020**, XRechnung has been mandatory for invoices sent to German federal authorities, with state and municipal authorities following on their own timelines. It is the format German B2G portals such as **ZRE** (Zentrale Rechnungseingangsplattform) and **OZG-RE** expect, and it is also routed over the **Peppol** network.

For a longer introduction including history, versions, and legal status, see the [XRechnung overview guide on invoicexml.com](https://invoicexml.com/xrechnung).

## The Leitweg-ID

The **Leitweg-ID** is the routing identifier of the German public authority you are invoicing. It is the single field that trips up most first-time XRechnung integrations, because it has no equivalent in ordinary commercial invoicing.

- It goes in the **buyer reference** field (**BT-10**), which the examples send as `invoice.buyerReference`.
- It is **mandatory**: rule `BR-DE-15` fails the invoice without it.
- The authority tells you their Leitweg-ID; you cannot derive or guess it. It looks like `991-12345-67`.
- It is often also used as the buyer's electronic address, with the Peppol EAS scheme code `0204`.

```json
"buyerReference": "991-12345-67"
```

## Why use this repository

Most existing XRechnung libraries are tied to a single language or vendor, and the specification documents are only published in German. This repository provides **side-by-side, copy-pasteable code examples** for the languages most commonly used in invoicing, ERP, and accounting systems, so developers can:

- Pick the language already used in their stack
- See exactly which fields XRechnung requires beyond plain EN 16931
- Run the same operations (create, validate, parse to JSON, AI-convert) in any of the supported languages
- Compare behavior and output across implementations
- Use the produced invoices as test fixtures for their own systems

Every example in this repository produces or consumes invoices that conform to the official XRechnung specification, validated against the KoSIT rules before delivery.

## Operations covered

| Operation | Description |
|---|---|
| **[Create XRechnung](https://www.invoicexml.com/docs/api/create/xrechnung)** | Build an XRechnung 3.0 XML invoice from a structured JSON request |
| **[Validate XRechnung](https://www.invoicexml.com/docs/api/validate/xrechnung)** | Verify an XRechnung file against the XSD, the EN 16931 Schematron, and the KoSIT BR-DE rules |
| **[Extract JSON](https://www.invoicexml.com/docs/api/extract/json)** | Parse an XRechnung XML file into structured invoice data as JSON for downstream systems |
| **[AI Convert](https://www.invoicexml.com/docs/api/transform/to/xrechnung)** *(experimental)* | Convert a plain, unstructured PDF invoice into a compliant XRechnung document using a large language model |

> **Looking for hybrid PDF invoices?** XRechnung is pure XML. If you need a PDF/A-3 with embedded XML for B2B exchange, see the [ZUGFeRD examples](https://github.com/InvoiceXML/zugferd-api-examples) (Germany) or the [Factur-X examples](https://github.com/InvoiceXML/facturx-api-examples) (France).

---

## Quick start by language

Each language has its own folder containing all four operations as standalone, runnable files. Open the folder for your language to see installation steps, dependencies, and usage.

### Create and validate XRechnung with C# / .NET

```
/csharp
  Create.cs         # Build an XRechnung 3.0 XML invoice
  Validate.cs       # Validate against the KoSIT XRechnung rules
  ExtractJson.cs    # Parse an XRechnung XML into JSON
  AiConvert.cs      # (Experimental) Convert plain PDF to XRechnung using AI
```

[Open the C# / .NET examples →](./csharp)

### Create and validate XRechnung with Java

```
/java
  Create.java
  Validate.java
  ExtractJson.java
  AiConvert.java
```

[Open the Java examples →](./java)

### Create and validate XRechnung with PHP

```
/php
  create.php
  validate.php
  extract-json.php
  ai-convert.php
```

[Open the PHP examples →](./php)

### Create and validate XRechnung with JavaScript in the browser

Browser-side examples for generating and inspecting XRechnung invoices client-side, useful for web-based invoicing apps that want to avoid round-trips to a server.

```
/javascript
  create.html
  validate.html
  extract-json.html
  ai-convert.html
```

[Open the JavaScript examples →](./javascript)

### Create and validate XRechnung with Node.js

```
/nodejs
  create.js
  validate.js
  extract-json.js
  ai-convert.js
```

[Open the Node.js examples →](./nodejs)

### Create and validate XRechnung with Python

```
/python
  create.py
  validate.py
  extract_json.py
  ai_convert.py
```

[Open the Python examples →](./python)

---

## Parse XRechnung data as JSON

Many downstream systems (ERPs, accounting software, expense management tools) work natively with JSON rather than XML. The `ExtractJson` examples in every language upload an XRechnung XML file and receive the EN 16931 model back as clean JSON: seller, buyer, line items, tax breakdown, totals, and payment details.

The invoice fields sit at the **root** of the response, so you read `seller.name` or `totals.grandTotalAmount` (BT-112) directly, with no wrapper object to unpack.

This is the fastest way to get invoice data out of an XRechnung file and into a modern data pipeline.

---

## AI-powered PDF to XRechnung conversion (Experimental)

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, multilingual, or missing fields that EN 16931 requires. Producing a fully compliant XRechnung document from such an input is non-trivial, and AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Output from the `AiConvert` examples **must always be reviewed by a human** before the invoice is submitted to a public authority. This is a developer convenience for bootstrapping test data and prototyping, **not** an unattended production pipeline.

The `AiConvert` examples in each language demonstrate the same pattern:

1. Read a plain (non-XRechnung) PDF invoice
2. Send it to the API together with the **Leitweg-ID**, which cannot be inferred from the PDF
3. A large language model reads the document and infers the invoice fields (seller, buyer, line items, tax breakdown, totals, payment terms)
4. The API maps those fields to the EN 16931 semantic model and generates XRechnung XML
5. The XML is validated against the EN 16931 Schematron and the KoSIT BR-DE rules before delivery

If validation fails at step 5, the API surfaces the specific rule that was violated so a human reviewer can correct it. This is intentional. Silent failures are worse than visible ones in a tax-relevant document.

---

## XRechnung syntax, versions, and validation

XRechnung has no profile ladder of the kind ZUGFeRD and Factur-X use. There is one CIUS, and two syntax choices:

| Aspect | Detail |
|---|---|
| **Syntax** | **UBL 2.1** or **UN/CEFACT CII D16B**. Both are valid XRechnung; pick whichever your recipient or portal integration expects. The examples in this repository produce CII. |
| **Version** | **3.0**, in force since February 2024, declared as `urn:cen.eu:en16931:2017#compliant#urn:xeinkauf.de:kosit:xrechnung_3.0`. Documents declaring a retired 2.x identifier are no longer compliant and must be re-issued. |
| **Validation** | Three layers: the XSD for the detected syntax, the EN 16931 core Schematron, and the German `BR-DE-*` rules from the KoSIT XRechnung Schematron, the same rules ZRE and OZG-RE enforce. |

To create XRechnung in UBL syntax instead of CII, use [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) with `options.profile` set to `xrechnung`.

---

## Related e-invoicing standards

XRechnung is one of several formats in the European e-invoicing landscape. If you work with invoices, you will likely encounter the others:

- **ZUGFeRD 2.x**: German hybrid format, a PDF/A-3 with embedded CII XML. The B2B counterpart to XRechnung's B2G role. See the [ZUGFeRD examples](https://github.com/InvoiceXML/zugferd-api-examples).
- **Factur-X**: French equivalent of ZUGFeRD 2.x. Technically identical specification, different branding. See the [Factur-X examples](https://github.com/InvoiceXML/facturx-api-examples).
- **UBL 2.1**: Universal Business Language. One of the two syntaxes XRechnung can be expressed in, and the native format of Peppol.
- **Peppol BIS Billing 3.0**: UBL-based subset exchanged over the Peppol network, the channel many German authorities receive XRechnung through.
- **EN 16931**: the European norm that underlies XRechnung, ZUGFeRD, Factur-X, and Peppol BIS.

For a side-by-side comparison of when to use which format, see the [e-invoicing standards comparison on invoicexml.com](https://invoicexml.com/standards).

---

## Frequently asked questions

**Is XRechnung a PDF?**
No. XRechnung is a standalone XML file with no PDF wrapper and no visual layer. If you need a human-readable PDF with the structured data embedded, you want ZUGFeRD (Germany) or Factur-X (France) instead.

**What is the difference between XRechnung and ZUGFeRD?**
XRechnung produces a standalone XML for B2G portals such as ZRE and OZG-RE, and applies the stricter German BR-DE rules. ZUGFeRD produces a hybrid PDF/A-3 with embedded XML, primarily for B2B exchange. Both satisfy EN 16931.

**Do I really need a Leitweg-ID?**
Yes, for B2G. Rule BR-DE-15 requires it in the buyer reference (BT-10). The authority you are invoicing issues it; you cannot derive it. See [The Leitweg-ID](#the-leitweg-id).

**Which XRechnung version do these examples produce?**
XRechnung 3.0, the version in force since February 2024, validated against the matching KoSIT rules before delivery.

**UBL or CII: which syntax should I use?**
Both are valid. These examples produce CII. Use whichever your recipient or portal integration expects; if you need UBL, call `/v1/create/ubl` with `options.profile` set to `xrechnung`.

**Is XRechnung mandatory for B2B invoicing in Germany?**
The German B2B mandate (reception since January 2025, issuance phasing in from 2027) accepts any EN 16931 compliant format. XRechnung qualifies, but most B2B senders choose ZUGFeRD because it also gives the recipient a readable PDF. XRechnung's mandate is B2G.

**Where do I submit the generated XRechnung?**
Via the Peppol network, or by uploading directly to the ZRE or OZG-RE portals for German federal, state, and municipal authorities.

---

## Resources and references

- [XRechnung specification (KoSIT)](https://xeinkauf.de/xrechnung/)
- [XRechnung validator and Schematron (KoSIT, GitHub)](https://github.com/itplr-kosit/validator)
- [Leitweg-ID guidance (KoSIT)](https://xeinkauf.de/leitweg-id/)
- [EN 16931 (European Committee for Standardization)](https://www.cencenelec.eu/)
- [Peppol BIS Billing 3.0](https://docs.peppol.eu/poacc/billing/3.0/)
- [invoicexml.com](https://invoicexml.com): guides, tutorials, validators, and standard comparisons for XRechnung, ZUGFeRD, Factur-X, UBL, and Peppol

---

## Contributing

Pull requests are welcome. Useful contributions include:

- Additional languages (Go, Rust, Ruby, Kotlin, Swift)
- Alternative library implementations in existing languages
- Sample invoice fixtures covering edge cases (reverse charge, zero-rated supplies, credit notes, UBL syntax)
- Translations of the README and inline comments
- Bug fixes and validation rule clarifications

Please include a working example, pinned dependency versions, and a short note in the relevant language folder's README.
