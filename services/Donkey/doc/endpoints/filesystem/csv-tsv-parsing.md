CSV/TSV Parsing
-------------------------

__URL Path__: /secured/filesystem/read-csv-chunk

__HTTP Method__: POST

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
    	"path" : "/iplant/home/testuser/test-tsv",
    	"separator" : "%09",
    	"page" : "4",
    	"chunk-size" : "400"
    }

__Response Body__:

{
    "chunk-size": "238",
    "csv": [
        {
            "0": "Number of mismatches allowed in each segment alignment for reads mapped independently",
            "1": "Integer",
            "2": "2"
        },
        {
            "0": "Minimum length of read segments",
            "1": "Integer",
            "2": "20"
        },
        {
            "0": "Number of threads",
            "1": "Integer",
            "2": "4"
        },
        {
            "0": "Tophat Version",
            "1": "TextSelection",
            "2": "1.4.1"
        },
        {
            "0": "Bowtie Version",
            "1": "TextSelection",
            "2": "0.12.7"
        }
    ],
    "file-size": "1041",
    "page" : "4",
    "max-cols": "3",
    "number-pages": "3",
    "path": "/iplant/home/testuser/test-tsv",
    "success": true,
    "user": "wregglej"
}

__CURL Command__:

    curl -d '{"path" : "/iplant/home/testuser/test-tsv", "separator" : "%09", "page" : "4", "chunk-size" : "400"}' http://127.0.0.1:31325/secured/filesystem/read-csv-chunk?proxyToken=adsfadsf

__JSON Field Descriptions__:

__path__ is the path to the file in iRODS that should be parsed as a CSV. Note that there isn't any checking in place to make sure that the file is actually a CSV or TSV. This is because we can't depend on the filetype detection to detect all possible types of CSV files (i.e. tab-delimited, pipe-delimited, hash-delimited, etc.).

__page__ is the page to start at in the file. Your first request should be at page 0 for any file so you can get back the maximum number of pages in the __number-pages__ field.

__chunk-size__ is the size to use when calculating the number of pages that a file contains. This should be in bytes.

__separator__ is a single character that the CSV parser uses to split fields. Common values are "," and "\t". We don't do any validation on this field so that we can support a wider-range of parsing options. The only constraints on this field is that it needs to be readable as a single char and it must be URL encoded.

__Notes and Limitations__:

This *should* work fine with files that use \r\n as the line ending, but it will not work correctly with files that use \r alone. It that case every page returned will be blank.

If the chunk-size is set so that it doesn't cover an entire line, a blank page will be returned.

The code that trims and resizes the pages to end on line breaks will detect '\n' that are embedded in double quoted cells as a line break. This is because we're not tracking opening and closing double quotes across pages. We're looking into ways of doing this, but it isn't in yet.

The URL encoded value for \t characters is '%09', without the quotes. If you aren't sending '%09' for the separator when you're trying to parse a TSV, then you're going to have a bad time.
