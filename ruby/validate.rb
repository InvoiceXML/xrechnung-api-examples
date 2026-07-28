# Validate an XRechnung XML file using the InvoiceXML API.
# Runs the XSD, the EN 16931 Schematron, and the KoSIT BR-DE rules.
# Uploads the file as multipart/form-data via Net::HTTP's built-in set_form.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/validate/xrechnung
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby validate.rb [path/to/invoice.xml]

require "net/http"
require "json"
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

def validate_xrechnung(path)
  uri = URI("#{BASE_URL}/v1/validate/xrechnung")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    response = api_http(uri).request(request)

    unless response.is_a?(Net::HTTPSuccess)
      warn "InvoiceXML API error #{response.code}: #{response.body}"
      exit 1
    end

    JSON.parse(response.body)
  end
end

report = validate_xrechnung(ARGV.fetch(0, "invoice-xrechnung.xml"))

# A finished validation always answers HTTP 200; the verdict lives in "valid".
if report["valid"]
  puts "Valid XRechnung document"
else
  report["errors"].each do |finding|
    # A "cius" failure means the document is a fine EN 16931 invoice that
    # misses a German requirement. The classic case is BR-DE-15, a missing Leitweg-ID.
    layer = finding["layer"] # "xsd", "en16931", or "cius"
    puts "[#{finding["rule"]}] (#{layer}) #{finding["message"]}"
    puts "  fields: #{Array(finding["fields"]).join(", ")}"
  end
  exit 1
end
