# Table of Contents

* [App Metadata Listing Services](#app-metadata-listing-services)
    * [Listing Workflow Elements](#listing-workflow-elements)
    * [Search Deployed Components](#search-deployed-components)
    * [Listing Analysis Identifiers](#listing-analysis-identifiers)
    * [Listing Data Objects in an Analysis](#listing-data-objects-in-an-analysis)
    * [Listing Analysis Groups](#listing-analysis-groups)
    * [Listing Individual Analyses](#listing-individual-analyses)
    * [Listing Analyses in an Analysis Group](#listing-analyses-in-an-analysis-group)
    * [Listing Deployed Components in an Analysis](#listing-deployed-components-in-an-analysis)
    * [Searching for Analyses](#searching-for-analyses)

# App Metadata Listing Services

## Listing Workflow Elements

*Unsecured Endpoint:* GET /get-workflow-elements/{element-type}

The `/get-workflow-elements/{element-type}` endpoint is used by Tito to obtain
lists of elements that may be included in an app. The following element types
are currently supported:

<table "border=1">
    <tr><th>Element Type</th><th>Description</th></tr>
    <tr><td>components</td><td>Registered deployed components</td></tr>
    <tr><td>formats</td><td>Known file formats</td></tr>
    <tr><td>info-types</td><td>Known types of data</td></tr>
    <tr><td>property-types</td><td>Known types of parameters</td></tr>
    <tr><td>rule-types</td><td>Known types of validation rules</td></tr>
    <tr><td>value-types</td><td>Known types of parameter values</td></tr>
    <tr><td>data-sources</td><td>Known sources for data objects</td></tr>
    <tr><td>tool-types</td><td>Known types of deployed components</td></tr>
    <tr><td>all</td><td>All workflow element types</td></tr>
</table>

The response format varies depending on the type of information that is being
returned.

Deployed components represent tools (usually, command-line tools) that can be
executed from within the discovery environment. Here's an example deployed
components listing:

```
$ curl -s http://by-tor:8888/get-workflow-elements/components | python -mjson.tool
{
    "components": [
        {
            "attribution": "Insane Membranes, Inc.",
            "description": "You'll find out!",
            "hid": 320,
            "id": "c718a4715484949a1bf0892e28324f64f",
            "location": "/usr/blah/bin",
            "name": "foo.pl",
            "type": "executable",
            "version": "0.0.1"
        },
        ...
    ],
    "success": true
}
```

The known file formats can be used to describe supported input or output formats
for a deployed component. For example, tools in the FASTX toolkit may support
FASTA files, several different varieties of FASTQ files and Barcode files, among
others. Here's an example listing:

```
$ curl -s http://by-tor:8888/get-workflow-elements/formats | python -mjson.tool
{
    "formats": [
        {
            "hid": 1,
            "id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
            "label": "Unspecified Data Format",
            "name": "Unspecified"
        },
        {
            "hid": 3,
            "id": "6C4D09B3-0108-4DD3-857A-8225E0645A0A",
            "label": "FASTX toolkit barcode file",
            "name": "Barcode-0"
        },
        ...
    ],
    "success": true
}
```

The known information types can be used to describe the type of information
consumed or produced by a deployed component. This is distinct from the data
format because some data formats may contain multiple types of information and
some types of information can be described using multiple data formats. For
example, the Nexus format can contain multiple types of information, including
phylogenetic trees. And phylogenetic trees can also be represented in PhyloXML
format, and a large number of other formats. The file format and information
type together identify the type of input consumed by a deployed component or the
type of output produced by a deployed component. here's an example information
type listing:

```
$ curl -s http://by-tor:8888/get-workflow-elements/info-types | python -mjson.tool
{
    "info_types": [
        {
            "hid": 3,
            "id": "0900E992-3BBD-4F4B-8D2D-ED289CA4E4F1",
            "label": "Unspecified",
            "name": "File"
        },
        {
            "hid": 6,
            "id": "0E3343E3-C59A-44C4-B5EE-D4501EC3A898",
            "label": "Reference Sequence and Annotations",
            "name": "ReferenceGenome"
        },
        ...
    ],
    "success": true
}
```

Property types represent the types of information that can be passed to a
deployed component. For command-line tools, a property generally represents a
command-line option and the property type represents the type of data required
by the command-line option. For example a `Boolean` property generally
corresponds to a single command-line flag that takes no arguments. A `Text`
property, on the other hand, generally represents some sort of textual
information. Some property types are not supported by all tool types, so it is
helpful in some cases to filter property types either by the tool type or
optionally by the deployed component (which is used to determine the tool type).

Here's an example that is not filtered by tool type:

```
$ curl -s http://by-tor:8888/get-workflow-elements/property-types | python -mjson.tool
{
    "property_types": [
        {
            "description": "A text box (no caption or number check)",
            "hid": 12,
            "id": "ptffeca61a-f1b9-43ba-b6ff-fa77bb34f396",
            "name": "Text",
            "value_type": "String"
        },
        {
            "description": "A text box that checks for valid number input",
            "hid": 1,
            "id": "ptd2340f11-d260-41b4-93fd-c1d695bf6fef",
            "name": "Number",
            "value_type": "Number"
        },
        ...
    ],
    "success": true
}
```

Here's an example that is filtered by tool type explicitly:

```
$ curl -s http://by-tor:8888/get-workflow-elements/property-types?tool-type=fAPI | python -mjson.tool
{
    "property_types": [
        {
            "description": "A text box that checks for valid number input",
            "hid": 1,
            "id": "ptd2340f11-d260-41b4-93fd-c1d695bf6fef",
            "name": "Number",
            "value_type": "Number"
        },
        {
            "description": "",
            "hid": 2,
            "id": "pt2cf37b0d-5463-4aef-98a2-4db63d2f3dbc",
            "name": "ClipperSelector",
            "value_type": null
        },
        ...
    ],
    "success": true
}
```

Here's an example that is filtered by component identifier:

```
$ curl -s http://by-tor:8888/get-workflow-elements/property-types?component-id=c1b9f95a766b64454a2570f5ddb255931 | python -mjson.tool
{
    "property_types": [
        {
            "description": "A text box that checks for valid number input",
            "hid": 1,
            "id": "ptd2340f11-d260-41b4-93fd-c1d695bf6fef",
            "name": "Number",
            "value_type": "Number"
        },
        {
            "description": "",
            "hid": 2,
            "id": "pt2cf37b0d-5463-4aef-98a2-4db63d2f3dbc",
            "name": "ClipperSelector",
            "value_type": null
        },
        ...
    ],
    "success": true
}
```

If you filter by both tool type and deployed component ID then the tool type
will take precedence. Including either an undefined tool type or an undefined
tool type name will result in an error:

```
$ curl -s http://by-tor:8888/get-workflow-elements/property-types?component-id=foo | python -mjson.tool
{
    "code": "UNKNOWN_DEPLOYED_COMPONENT",
    "id": "foo",
    "success": false
}
```

```
$ curl -s http://by-tor:8888/get-workflow-elements/property-types?tool-type=foo | python -mjson.tool
{
    "code": "UNKNOWN_TOOL_TYPE",
    "name": "foo",
    "success": false
}
```

Rule types represent types of validation rules that may be defined to validate
user input. For example, if a property value must be an integer between 1 and 10
then the `IntRange` rule type may be used. Similarly, if a property value must
contain data in a specific format, such as a phone number, then the `Regex` rule
type may be used. Here's an example listing:

```
$ curl -s http://by-tor:8888/get-workflow-elements/rule-types | python -mjson.tool
{
    "rule_types": [
        {
            "description": "Has a range of integers allowed",
            "hid": 3,
            "id": "rte04fb2c6-d5fd-47e4-ae89-a67390ccb67e",
            "name": "IntRange",
            "rule_description_format": "Value must be between: {Number} and {Number}.",
            "subtype": "Integer",
            "value_types": [
                "Number"
            ]
        },
        {
            "description": "Has a range of values allowed (non-integer)",
            "hid": 6,
            "id": "rt58cd8b75-5598-4490-a9c9-a6d7a8cd09dd",
            "name": "DoubleRange",
            "rule_description_format": "Value must be between: {Number} and {Number}.",
            "subtype": "Double",
            "value_types": [
                "Number"
            ]
        },
    ],
    "success": true
}
```

If you look closely at the example property type and rule type listings then
you'll notice that each property type has a single value type assocaited with it
and each rule type has one or more value types associated with it. The purpose
of value types is specifically to link property types and rule types. Tito uses
the value type to determine which types of rules can be applied to a property
that is being defined by the user. Here's an example value type listing:

```
$ curl -s http://by-tor:8888/get-workflow-elements/value-types | python -mjson.tool
{
    "value_types": [
        {
            "description": "Arbitrary text",
            "hid": 1,
            "id": "0115898A-F81A-4598-B1A8-06E538F1D774",
            "name": "String"
        },
        {
            "description": "True or false value",
            "hid": 2,
            "id": "E8E05E6C-5002-48C0-9167-C9733F0A9716",
            "name": "Boolean"
        },
        ...
    ],
    "success": true
}
```

Data sources are the known possible sources for data objects. In most cases,
data objects will come from a plain file. The only other options that are
currently available are redirected standard output and redirected standard
error output. Both of these options apply only to data objects that are
associated with an output. Here's an example:

```
$ curl -s http://by-tor:8888/get-workflow-elements/data-sources | python -mjson.tool
{
    "data_sources": [
        {
            "hid": 1,
            "id": "8D6B8247-F1E7-49DB-9FFE-13EAD7C1AED6",
            "label": "File",
            "name": "file"
        },
        ...
    ],
    "success": true
}
```

Tool types are known types of deployed components in the Discovery
Environment. Generally, there's a different tool type for each execution
environment that is supported by the Discovery Environment. Here's an
example:

```
$ curl -s http://by-tor:8888/get-workflow-elements/tool-types | python -mjson.tool
{
    "success": true,
    "tool_types": [
        {
            "description": "Run at the University of Arizona",
            "id": 1,
            "label": "UA",
            "name": "executable"
        },
        ...
    ]
}
```

As a final option, it is possible to get all types of workflow elements at
once using an element type of `all`. Here's an example listing:

```
$ curl -s http://by-tor:8888/get-workflow-elements/all | python -mjson.tool
{
    "components": [
        {
            "attribution": "Insane Membranes, Inc.",
            "description": "You'll find out!",
            "hid": 320,
            "id": "c718a4715484949a1bf0892e28324f64f",
            "location": "/usr/local2/bin",
            "name": "foo.pl",
            "type": "executable",
            "version": "0.0.1"
        },
        ...
    ],
    "data_sources": [
        {
            "hid": 1,
            "id": "8D6B8247-F1E7-49DB-9FFE-13EAD7C1AED6",
            "label": "File",
            "name": "file"
        },
        ...
    ],
    "formats": [
        {
            "hid": 1,
            "id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
            "label": "Unspecified Data Format",
            "name": "Unspecified"
        },
        ...
    ],
    "info_types": [
        {
            "hid": 3,
            "id": "0900E992-3BBD-4F4B-8D2D-ED289CA4E4F1",
            "label": "Unspecified",
            "name": "File"
        },
        ...
    ],
    "property_types": [
        {
            "description": "A text box (no caption or number check)",
            "hid": 12,
            "id": "ptffeca61a-f1b9-43ba-b6ff-fa77bb34f396",
            "name": "Text",
            "value_type": "String"
        },
        ...
    ],
    "rule_types": [
        {
            "description": "Has a range of integers allowed",
            "hid": 3,
            "id": "rte04fb2c6-d5fd-47e4-ae89-a67390ccb67e",
            "name": "IntRange",
            "rule_description_format": "Value must be between: {Number} and {Number}.",
            "subtype": "Integer",
            "value_types": [
                "Number"
            ]
        },
        ...
    ],
    "success": true,
    "tool_types": [
        {
            "description": "Run at the University of Arizona",
            "id": 1,
            "label": "UA",
            "name": "executable"
        },
        ...
    ],
    "value_types": [
        {
            "description": "Arbitrary text",
            "hid": 1,
            "id": "0115898A-F81A-4598-B1A8-06E538F1D774",
            "name": "String"
        },
        ...
    ]
}
```

## Search Deployed Components

*Unsecured Endpoint:* GET /search-deployed-components/{search-term}

The `/search-deployed-components/{search-term}` endpoint is used by Tito to
search for a deployed component with a name or description that contains the
given search-term.

The response format is the same as the /get-workflow-elements/components
endpoint:

```
$ curl -s http://by-tor:8888/search-deployed-components/example | python -mjson.tool
{
    "components": [
        {
            "name": "foo-example.pl",
            "description": "You'll find out!",
            ...
        },
        {
            "name": "foo-bar.pl",
            "description": "Another Example Script",
            ...
        },
        ...
    ]
}
```

## Listing Analysis Identifiers

*Unsecured Endpoint:* GET /get-all-analysis-ids

The export script needs to have a way to obtain the identifiers of all of the
analyses in the Discovery Environment, deleted or not. This service provides
that information. Here's an example listing:

```
$ curl -s http://by-tor:8888/get-all-analysis-ids | python -mjson.tool
{
    "analysis_ids": [
        "19F78CC1-7E14-481B-9D80-85EBCCBFFCAF",
        "C5FF73E8-157F-47F0-978C-D4FAA12C2D58",
        ...
    ]
}
```

## Listing Data Objects in an Analysis

*Unsecured Endpoint:* GET /analysis-data-objects/{analysis-id}

When a pipeline is being created, the UI needs to know what types of files are
consumed by and what types of files are produced by each analysis in the
pipeline. This service provides that information. The response body contains the
analysis identifier, the analysis name, a list of inputs (types of files
consumed by the service) and a list of outputs (types of files produced by the
service). The response format is:

```json
{
    "id": "analysis-id",
    "inputs": [
        {
            "data_object": {
                "cmdSwitch": "command-line-switch",
                "description": "description",
                "file_info_type": "info-type-name",
                "format": "data-format-name",
                "id": "data-object-id",
                "multiplicity": "multiplicity-name",
                "name": "data-object-name",
                "required": "required-data-object-flag",
                "retain": "retain-file-flag",
            },
            "description": "property-description",
            "id": "property-id",
            "isVisible": "visibility-flag",
            "label": "property-label",
            "name": "property-name",
            "type": "Input",
            "value": "default-property-value"
        },
        ...
    ]
    "name": analysis-name,
    "outputs": [
        {
            "data_object": {
                "cmdSwitch": "command-line-switch",
                "description": "description",
                "file_info_type": "info-type-name",
                "format": "data-format-name",
                "id": "data-object-id",
                "multiplicity": "multiplicity-name",
                "name": "data-object-name",
                "required": "required-data-object-flag",
                "retain": "retain-file-flag",
            },
            "description": "property-description",
            "id": "property-id",
            "isVisible": "visibility-flag",
            "label": "property-label",
            "name": "property-name",
            "type": "Output",
            "value": "default-property-value"
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s http://by-tor:8888/analysis-data-objects/19F78CC1-7E14-481B-9D80-85EBCCBFFCAF | python -mjson.tool
{
    "id": "19F78CC1-7E14-481B-9D80-85EBCCBFFCAF",
    "inputs": [
        {
            "data_object": {
                "cmdSwitch": "",
                "description": "",
                "file_info_type": "File",
                "format": "Unspecified",
                "id": "A6210636-E3EC-4CD3-97B4-CAD15CAC0913",
                "multiplicity": "One",
                "name": "Input File",
                "order": 1,
                "required": true,
                "retain": false
            },
            "description": "",
            "id": "A6210636-E3EC-4CD3-97B4-CAD15CAC0913",
            "isVisible": true,
            "label": "Input File",
            "name": "",
            "type": "Input",
            "value": ""
        }
    ],
    "name": "Jills Extract First Line",
    "outputs": [
        {
            "data_object": {
                "cmdSwitch": "",
                "description": "",
                "file_info_type": "File",
                "format": "Unspecified",
                "id": "FE5ACC01-0B31-4611-B81E-26E532B459E3",
                "multiplicity": "One",
                "name": "head_output.txt",
                "order": 3,
                "required": true,
                "retain": true
            },
            "description": "",
            "id": "FE5ACC01-0B31-4611-B81E-26E532B459E3",
            "isVisible": false,
            "label": "head_output.txt",
            "name": "",
            "type": "Output",
            "value": ""
        }
    ]
}
```

## Listing Analysis Groups

*Unsecured Endpoint:* GET /public-app-groups

*Secured Endpoint:* GET /secured/app-groups

This service is used by the DE to obtain the list of analysis groups that are
visible to the user. This list includes analysis groups that are in the user's
workspace along with any analysis groups that are in a workspace that is marked
as public in the database. The `workspace-id` argument is the user's numeric
workspace identifier. The response is in the following format:

```json
{
    "groups": [
        {
            "description": "analysis-group-description",
            "groups": [
               ...
            ],
            "id": "analysis-group-id",
            "is_public": "public-flag",
            "name": "analysis-group-name",
            "template_count": "template-count",
            "workspace_id": "workspace-id"
        }
    ]
}
```

Note that this data structure is recursive; each analysis group may contain zero
or more other analysis groups.

Here's an example call for getting only the public app groups:

```
$ curl -s http://by-tor:8888/public-app-groups | python -mjson.tool
{
    "groups": [
        {
            "description": "",
            "groups": [
                {
                    "description": "",
                    "id": "g5401bd146c144470aedd57b47ea1b979",
                    "is_public": true,
                    "name": "Beta",
                    "template_count": 47,
                    "workspace_id": 0
                },
                ...
            ],
            "id": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
            "is_public": true,
            "name": "Public Apps",
            "template_count": "325",
            "workspace_id": 0
        },
        ...
    ]
}
```

Here's an example using the secured endpoint:

```
$ curl -s "http://by-tor:8888/secured/app-groups?user=nobody&email=nobody@iplantcollaborative.org" | python -mjson.tool
{
    "groups": [
        {
            "description": "",
            "groups": [
                {
                    "description": "",
                    "id": "b9a1a3b8-fef6-4576-bbfe-9ad17eb4c2ab",
                    "is_public": false,
                    "name": "Apps Under Development",
                    "template_count": 0,
                    "workspace_id": 42
                },
                {
                    "description": "",
                    "id": "2948ed96-9564-489f-ad73-e099b171a9a5",
                    "is_public": false,
                    "name": "Favorite Apps",
                    "template_count": 0,
                    "workspace_id": 42
                }
            ],
            "id": "57a39832-3577-4ee3-8ff4-3fc9d1cf9e34",
            "is_public": false,
            "name": "Workspace",
            "template_count": 0,
            "workspace_id": 42
        },
        ...
    ]
}
```

## Listing Individual Analyses

*Unsecured Endpoint:* GET /list-analysis/{analysis-id}

This service lists information about a single analysis if that analysis exists.
Here are some examples:

```
$ curl -s http://by-tor:8888/list-analysis/00102BE0-A7D7-4CC8-89F0-B1DB84B79018 | python -mjson.tool
{
    "analyses": [
        {
            "deleted": false,
            "description": "",
            "disabled": false,
            "edited_date": "",
            "id": "00102BE0-A7D7-4CC8-89F0-B1DB84B79018",
            "integration_date": "",
            "integrator_email": "mherde@iplantcollaborative.org",
            "integrator_name": "mherde",
            "is_favorite": false,
            "is_public": false,
            "name": "Copy of FASTX Barcode Splitter (Single End)",
            "pipeline_eligibility": {
                "is_valid": true,
                "reason": ""
            },
            "rating": {
                "average": 0
            },
            "wiki_url": ""
        }
    ]
}
```

```
$ curl -s http://by-tor:8888/list-analysis/I-DO-NOT-EXIST | python -mjson.tool
{
    "analyses": []
}
```

## Listing Analyses in an Analysis Group

*Secured Endpoint:* GET /secured/get-analyses-in-group/{group-id}

This service lists all of the analyses within an analysis group or any of its
descendents. The DE uses this service to obtain the list of analyses when a user
clicks on a group in the _Apps_ window.

This endpoint accepts optional URL query parameters to limit and sort Apps,
which will allow pagination of results.

<table "border=1">
    <tr><th>Parameter</th><th>Description</th></tr>
    <tr>
        <td>limit=X</td>
        <td>
            Limits the response to X number of results in the "templates" array.
            See
            http://www.postgresql.org/docs/8.4/interactive/queries-limit.html
        </td>
    </tr>
    <tr>
        <td>offset=X</td>
        <td>
            Skips the first X number of results in the "templates" array. See
            http://www.postgresql.org/docs/8.4/interactive/queries-limit.html
        </td>
    </tr>
    <tr>
        <td>sortField=X</td>
        <td>
            Sorts the results in the "templates" array by the field X, before
            limits and offsets are applied. This field can be any one of the
            simple fields of the "templates" objects, or `average_rating` or
            `user_rating` for ratings sorting. See
            http://www.postgresql.org/docs/8.4/interactive/queries-order.html
        </td>
    </tr>
    <tr>
        <td>sortDir=[ASC|DESC]</td>
        <td>
            Only used when sortField is present. Sorts the results in either
            ascending (`ASC`) or descending (`DESC`) order, before limits and
            offsets are applied. Defaults to `ASC`.
            See
            http://www.postgresql.org/docs/8.4/interactive/queries-order.html
        </td>
    </tr>
</table>

The response body for this service is in the following format:

```json
{
    "description": "analysis-group-description",
    "id": "analysis-group-id",
    "is_public": "public-group-flag",
    "name": "analysis-group-name",
    "template_count": "number-of-analyses-in-group-and-descendents",
    "templates": [
        {
            "can_favor": "analysis-can-favor-flag",
            "can_rate": "analysis-can-rate-flag",
            "can_run": "analysis-can-run-flag",
            "deleted": "analysis-deleted-flag",
            "description": "analysis-description",
            "disabled": "analysis-disabled-flag",
            "id": "analysis-id",
            "integrator_email": "integrator-email-address",
            "integrator_name": "integrator-name",
            "is_favorite": "favorite-analysis-flag",
            "is_public": "public-analysis-flag",
            "name": "analysis-name",
            "pipeline_eligibility": {
                "is_valid": "valid-for-pipelines-flag",
                "reason": "reason-for-exclusion-from-pipelines-if-applicable",
            },
            "rating": {
                "average": "average-rating",
                "comment-id": "comment-id",
                "user": "user-rating"
            },
            "wiki_url": "documentation-link"
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/get-analyses-in-group/6A1B9EBD-4950-4F3F-9CAB-DD12A1046D9A?user=snow-dog&email=sd@example.org&limit=1&sortField=name&sortDir=DESC" | python -mjson.tool
{
    "description": "",
    "id": "C3DED4E2-EC99-4A54-B0D8-196112D1BB7B",
    "is_public": true,
    "name": "Some Group",
    "template_count": 100,
    "templates": [
        {
            "can_favor": true,
            "can_rate": true,
            "can_run": true,
            "deleted": false,
            "description": "Some app description.",
            "disabled": false,
            "id": "81C0CCEE-439C-4516-805F-3E260E336EE4",
            "integrator_email": "nobody@iplantcollaborative.org",
            "integrator_name": "Nobody",
            "is_favorite": false,
            "is_public": true,
            "name": "Z-AppName",
            "pipeline_eligibility": {
                "is_valid": true,
                "reason": ""
            },
            "rating": {
                "average": 4,
                "comment_id": 27,
                "user": 4
            },
            "wiki_url": "https://pods.iplantcollaborative.org/wiki/some/doc/link"
        }
    ]
}
```

The `can_run` flag is calculated by comparing the number of steps in the app to
the number of steps that have deployed component associated with them. If the
numbers are different then this flag is set to `false`. The idea is that every
step in the analysis has to have, at the very least, a deployed component
associated with it in order to run successfully.

## Listing Deployed Components in an Analysis

*Secured Endpoint:* GET /secured/get-components-in-analysis/{analysis-id}

This service lists information for all of the deployed components that are
associated with an analysis. This information used to be included in the results
of the analysis listing service. The response body is in the following format:

```json
{
    "deployed_components": [
        {
            "attribution": "attribution-1",
            "description": "description-1",
            "id": "id-1",
            "location": "location-1",
            "name": "name-1",
            "type": "type-1",
            "version": "version-1"
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/get-components-in-analysis/0BA04303-F0CB-4A34-BACE-7090F869B332?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "deployed_components": [
        {
            "attribution": "",
            "description": "",
            "id": "c73ef66158ef94f1bb90689ff813629f5",
            "location": "/usr/local2/muscle3.8.31",
            "name": "muscle",
            "type": "executable",
            "version": ""
        },
        {
            "attribution": "",
            "description": "",
            "id": "c2d79e93d83044a659b907764275248ef",
            "location": "/usr/local2/phyml-20110304",
            "name": "phyml",
            "type": "executable",
            "version": ""
        }
    ]
}
```

## Searching for Analyses

*Secured Endpoint:* GET /secured/search-analyses

This service allows users to search for analyses based on a part of the analysis
name or description. The response body contains a "templates" array that is in
the same format as the "templates" array in the /secured/get-analyses-in-group
endpoint response (see the next section):

```json
{
    "templates": [
        {
            "id": "analysis-id",
            "description": "analysis-description",
            "name": "analysis-name",
            "group_id": "analysis-group-id",
            "group_name": "analysis-group-name",
            ...
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/search-analyses?user=snow-dog&email=sd@example.org&search=ranger" | python -mjson.tool
{
    "templates": [
        {
            "id": "9D221848-1D12-4A31-8E93-FA069EEDC151",
            "name": "Ranger",
            "description": "Some Description",
            "group_id": "99F2E2FE-9931-4154-ADDB-28386027B19F",
            "group_name": "Some Group Name",
            ...
        }
    ]
}
```
