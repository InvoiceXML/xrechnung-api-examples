# Parse an old-school invoice PDF (typed, scanned, or photographed, no
# structured XML) into the EN 16931 JSON model with AI using the InvoiceXML API.
# Returns the same InvoiceDocument shape as extract_json.rb, plus a
# "confidence" object with an overall score and four area scores
# (seller, buyer, tax calculation, line items), each from 0.0 to 1.0.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/parse/json
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby parse_json.rb path/to/invoice.pdf

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

def parse_invoice_pdf(path)
  uri = URI("#{BASE_URL}/v1/parse/json")
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

result     = parse_invoice_pdf(ARGV.fetch(0) { abort "Usage: ruby parse_json.rb path/to/invoice.pdf" })
invoice    = result["invoice"]
confidence = result.dig("confidence", "overall")

puts "Invoice #{invoice["invoiceNumber"]} from #{invoice.dig("seller", "name")}"
puts "Overall confidence: #{confidence}"

# Automate the confident reads, route the shaky ones to a human.
if confidence < 0.7
  puts "Low confidence: route this invoice to human review before import."
  puts JSON.pretty_generate(result["confidence"])
else
  puts "High confidence: safe to import automatically."
  # The invoice hash matches the /v1/create/xrechnung request body. Note that
  # the Leitweg-ID is routing infrastructure and appears on no source document,
  # so inject it from your own buyer master data before re-creating an invoice.
end
