# Extract the embedded supporting documents (EN 16931 attachment group
# BG-24: delivery notes, timesheets, the original order) from an XRechnung
# XML e-invoice as a ZIP archive using the InvoiceXML API.
#
# This earns its keep on the German B2G side, because portals transmit
# supporting documents inside the XML rather than as separate files.
# Attachment bytes are decoded into the ZIP verbatim, never parsed or
# rendered server-side. Documents referenced only by external URI (BT-124)
# are not fetched. An invoice without embedded attachments answers 404
# rather than an empty archive.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/extract/attachments
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby extract_attachments.rb [path/to/invoice.xml] [attachments.zip]

require "net/http"
require "uri"

BASE_URL = "https://api.invoicexml.com"
API_KEY  = ENV.fetch("INVOICEXML_API_KEY")

def api_http(uri)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  http.open_timeout = 10
  http.read_timeout = 120
  http
end

def extract_attachments(path, zip_path)
  uri = URI("#{BASE_URL}/v1/extract/attachments")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    response = api_http(uri).request(request)

    return nil if response.is_a?(Net::HTTPNotFound) # no embedded attachments

    unless response.is_a?(Net::HTTPSuccess)
      warn "InvoiceXML API error #{response.code}: #{response.body}"
      exit 1
    end

    File.binwrite(zip_path, response.body)
    zip_path
  end
end

zip = extract_attachments(
  ARGV.fetch(0, "invoice-xrechnung.xml"),
  ARGV.fetch(1, "attachments.zip")
)

if zip
  puts "Saved #{zip}"
else
  puts "The invoice carries no embedded attachments."
end
