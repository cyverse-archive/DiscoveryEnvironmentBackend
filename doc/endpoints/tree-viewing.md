# Table of Contents

* [Phylogenetic Tree Rendering Endpoints](#phylogenetic-tree-rendering-endpoints)
    * [Obtaining Tree Viewer URLs for a POST Body](#obtaining-tree-viewer-urls-for-a-post-body)
    * [Obtaining Tree Viewer URLs](#obtaining-tree-viewer-urls)

# Phylogenetic Tree Rendering Endpoints

## Obtaining Tree Viewer URLs for a POST Body

Unsecured Endpoint: POST /tree-viewer-urls

This service is a slight variation of the secured GET /secured/tree-viewer-urls
service and is intended primarily for debugging. The request body should contain
the contents of the file for which you want to view trees. The response body
consists of a JSON object listing the paths to the TREE urls:

```json
{
    "action": "tree_manifest",
    "success": true,
    "tree-urls": [
        {
            "label": "tree-label-1",
            "url": "tree-url-1"
        },
        {
            "label": "tree-label-2",
            "url": "tree-url-2"
        },
        {
            "label": "tree-label-n",
            "url": "tree-url-n"
        }
    ]
}
```

If the tree file contains labels for each of its trees then those labels will be
used. Otherwise, a generic label, tree\__n_, where _n_ is a sequential number.
Here's an example of a successful call:

```
$ curl -s --data-binary @Aquilegia.nex http://by-tor:8888/tree-viewer-urls | python -mjson.tool
{
    "action": "tree_manifest",
    "success": true,
    "tree-urls": [
        {
            "label": "anthocyanin gene expression - ultrametric",
            "url": "http://by-tor/view/tree/27c1db94fe8dafe006a113d8fbfcb4c7"
        },
        {
            "label": "floral traits tree - aflp branchlengths",
            "url": "http://by-tor/view/tree/94d8339e4be13c3dd9d8f357572590b3"
        },
        {
            "label": "floral traits tree - ultrametric",
            "url": "http://by-tor/view/tree/cd1c8ea9cc3792cbc4cf2e2d8353b7f1"
        }
    ]
}
```

## Obtaining Tree Viewer URLs

Secured Endpoint: GET /secured/tree-viewer-urls

This service is used to obtain tree viewer URLs for a single file in the iPlant
data store. This URL requires one query-string parameter, `path`, in addition to
the usual `proxyToken` parameter that is required by all secured endpoints.  The
`path` query-string parameter should contain the path to the file. If the
service call is successful then the response body will look something like this:

```json
{
    "action": "tree_manifest",
    "success": true,
    "tree-urls": [
        {
            "label": tree-label-1,
            "url": tree-url-1
        },
        {
            "label": tree-label-2,
            "url": tree-url-2
        },
        ...,
        {
            "label": tree-label-n,
            "url": tree-url-n
        }
    ]
}
```

Otherwise, the response body will contain information about the cause of the
failure.

Here's an example of a successful service call:

```
$ curl -s "http://by-tor:8888/secured/tree-viewer-urls?proxyToken=$(cas-ticket)&path=/iplant/home/nobody/sample1.newick" | python -mjson.tool
{
    "action": "tree_manifest",
    "success": true,
    "tree-urls": [
        {
            "label": "tree_0",
            "url": "http://by-tor/view/tree/d0f44d9cc8cd27ad060fbc2616ba2247"
        }
    ]
}
```
