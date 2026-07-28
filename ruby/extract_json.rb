# Extract XRechnung invoice data as JSON using the InvoiceXML API.
# Upload the XRechnung XML (CII or UBL syntax) and receive the EN 16931
# model as JSON. Deterministic XML parsing, no AI involved.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/extract/json
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby extract_json.rb [path/to/invoice.xml]

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

def extract_invoice(path)
  uri = URI("#{BASE_URL}/v1/extract/json")
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

data = extract_invoice(ARGV.fetch(0, "invoice-xrechnung.xml"))

File.write("invoice.json", JSON.pretty_generate(data))

# The invoice document sits under the "invoice" key of the response.
invoice = data["invoice"]
puts "Invoice #{invoice["invoiceNumber"]} from #{invoice.dig("seller", "name")}"
puts "Leitweg-ID (BT-10): #{invoice["buyerReference"]}"
puts "Grand total: #{invoice.dig("totals", "grandTotalAmount")} #{invoice["currency"]}"
puts "Saved invoice.json"
