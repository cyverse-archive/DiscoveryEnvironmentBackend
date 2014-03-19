(ns kifshare.test.ui-template
  (:use [kifshare.ui-template]
        [midje.sweet])
  (:require [kifshare.config :as cfg]))

(fact "test-unit-test"
      (+ 1 1) => 2)

(fact "Clear div"
      (clear) => "<div class=\"clear\"></div>")

(fact "Section spacer"
      (section-spacer) => "<div class=\"section-spacer\"></div>")

(fact "AVU table row"
      (irods-avu-row {:attr "attr" :value "value" :unit "unit"}) =>
      "<tr><td>attr</td><td>value</td><td>unit</td></tr>")

(fact "AVU table"
      (irods-avu-table [{:attr "attr" :value "value" :unit "unit"}]) =>
      (str "<div id=\"irods-avus\">"
             "<div id=\"irods-avus-header\">"
               "<h2>Metadata</h2>"
             "</div>"
             "<table id=\"irods-avus-data\">"
               "<thead>"
                 "<tr>"
                   "<th>Attribute</th>"
                   "<th>Value</th>"
                   "<th>Unit</th>"
                 "</tr>"
               "</thead>"
               "<tbody>"
                 "<tr>"
                   "<td>attr</td>"
                   "<td>value</td>"
                   "<td>unit</td>"
                 "</tr>"
               "</tbody>"
             "</table>"
             "<div class=\"section-spacer\"></div>"
           "</div>"))

(fact "Last modified date"
      (lastmod {:lastmod "foo-blippy-bar"}) =>
      (str "<div id=\"lastmod-detail\">"
             "<div id=\"lastmod-label\">"
               "<p>Last Modified:</p>"
             "</div>"
             "<div id=\"lastmod\">"
               "<p>foo-blippy-bar</p>"
             "</div>"
           "</div>"))

(fact "Calculate file size"
      (calc-filesize {:filesize "1024"}) => "1 KB")

(fact "File size details"
      (filesize {:filesize "1024"}) =>
      (str "<div id=\"size-detail\">"
             "<div id=\"size-label\">"
               "<p>File Size:</p>"
             "</div>"
             "<div id=\"size\">"
               "<p>1 KB</p>"
             "</div>"
             "</div>"))

(fact "ticket info map"
      (with-redefs [cfg/de-import-flags #(str "import flags!")
                    cfg/wget-flags #(str "wget flags!")
                    cfg/curl-flags #(str "curl flags!")
                    cfg/iget-flags #(str "iget flags!")]
        (ui-ticket-info {}) =>
        {:import_template "import flags!"
         :wget_template "wget flags!"
         :curl_template "curl flags!"
         :iget_template "iget flags!"}))

(fact "template map: JSON is not generated in a predictable way, so we're using a regex to test"
      (with-redefs [cfg/de-import-flags #(str "import flags!")
                    cfg/wget-flags #(str "wget flags!")
                    cfg/curl-flags #(str "curl flags!")
                    cfg/iget-flags #(str "iget flags!")]
        (template-map {}) =>
        (re-pattern (str
                     "<span id=\"ticket-info\" style=\"display: none;\">"
                     "<div id=\"ticket-info-map\">"
                     ".*"
                     "</div>"
                     "</span>"))))

(fact "input display"
      (input-display "foo") =>
      "<input id=\"foo\" type=\"text\" value=\"\" />")

(fact "iRODs instructions"
  (irods-instr {}) =>
  (str
   "<div id=\"irods-instructions\">"
     "<div id=\"irods-instructions-label\">"
       "<h2>iRODS icommands</h2>"
     "</div>"
     "<div id=\"clippy-irods-instructions\">"
       "<input id=\"irods-command-line\" type=\"text\" value=\"\" />"
       "<span title=\"copy to clipboard\">"
         "<button class=\"clippy-irods\" id=\"clippy-irods-wrapper\" title=\"Copy\">Copy</button>"
       "</span>"
     "</div>"
   "</div>"))

(fact "URL import instructions"
  (de-import-instr {}) =>
  (str
   "<div id=\"de-import-instructions\">"
     "<div id=\"de-import-instructions-label\">"
       "<h2>DE Import URL</h2>"
     "</div>"
     "<div id=\"clippy-import-instructions\">"
       "<input id=\"de-import-url\" type=\"text\" value=\"\" />"
       "<span title=\"copy to clipboard\">"
         "<button class=\"clippy-import\" id=\"clippy-import-wrapper\" title=\"Copy\">Copy</button>"
       "</span>"
     "</div>"
   "</div>"))

(fact "downloader instructions"
  (downloader-instr "lol-id" {}) =>
  (str
   "<div id=\"wget-instructions\">"
     "<div id=\"wget-instructions-label\">"
       "<p>Wget</p>"
     "</div>"
     "<div id=\"clippy-wget-instructions\">"
       "<input id=\"wget-command-line\" type=\"text\" value=\"\" />"
       "<span title=\"copy to clipboard\">"
         "<button class=\"clippy-wget\" id=\"clippy-wget-wrapper\" title=\"Copy\">Copy</button>"
       "</span>"
     "</div>"
   "</div>"
   "<div id=\"curl-instructions\">"
     "<div id=\"curl-instructions-label\">"
       "<p>cURL</p>"
     "</div>"
     "<div id=\"clippy-curl-instructions\">"
       "<input id=\"curl-command-line\" type=\"text\" value=\"\" />"
       "<span title=\"copy to clipboard\">"
         "<button class=\"clippy-curl\" id=\"clippy-curl-wrapper\" title=\"Copy\">Copy</button>"
       "</span>"
     "</div>"
     "</div>"))

(fact "menu generation"
  (with-redefs [cfg/logo-path (fn [] "/tmp/logo-path")]
    (menu {:filename "foo" :ticket-id "a-ticket"})) =>
    (str
     "<div id=\"menu\">"
       "<ul>"
         "<li>"
           "<div id=\"logo-container\">"
             "<img id=\"logo\" src=\"/tmp/logo-path\" />"
           "</div>"
         "</li>"
         "<li>"
           "<div>"
             "<h1 id=\"filename\" title=\"foo\">foo</h1>"
           "</div>"
         "</li>"
         "<li>"
           "<div id=\"download-container\">"
             "<a href=\"d/a-ticket/foo\" id=\"download-link\">"
               "<div id=\"download-link-area\">Download!</div>"
             "</a>"
           "</div>"
         "</li>"
       "</ul>"
       "</div>"))

(fact "details section"
  (details {:filesize "1024" :lastmod "1024"}) =>
  (re-pattern
   (str
    "<div id=\"details\">"
      "<a name=\"details-section\"></a>"
      "<div id=\"details-header\">"
        "<h2>File Details</h2>"
        ".*"
      "</div>"
      "<div class=\"section-spacer\"></div>"
    "</div>")))

(fact "alternative downloads section"
  (alt-downloads {}) =>
  (re-pattern
   (str
    "<div id=\"alt-downloads-header\">"
      "<h2>Alternative Download Methods</h2>"
    "</div>"
    "<div id=\"alt-downloads\">"
      "<div id=\"de-import-instructions\">"
        ".*"
      "</div>"
      "<div id=\"irods-instructions\">"
        ".*"
      "</div>"
      "<div id=\"wget-instructions\">"
        ".*"
      "</div>"
      "<div id=\"curl-instructions\">"
        ".*"
      "</div>"
    "</div>")))

(fact "footer test"
      (with-redefs [cfg/footer-text #(str "footer!")]
        (footer) =>
        (str "<div id=\"footer\">"
               "<p>footer!</p>"
             "</div>")))

#_(fact "twitter title"
      (twitter-title {:filename "filename"} {}) => "filename")

#_(fact "twitter description"
      (twitter-description {:filesize "1024" :lastmod "last!"} {}) =>
      "File Size: 1 KB\nLast Modified: last!")


#_(fact "twitter card"
  (twitter-card {:filesize "1024" :lastmod "last!" :filename "filename"} {}) =>
  (str "<meta content=\"summary\" name=\"twitter:card\" />"
       "<meta content=\"filename\" name=\"twitter:title\" />"
       "<meta content=\"File Size: 1 KB\nLast Modified: last!\" name=\"twitter:description\" />"))