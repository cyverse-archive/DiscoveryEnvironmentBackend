# Table of Contents

* [Overview](#overview)
    * [Template JSON](#template-json)
        * [Template JSON - Template](#template-json---template)
        * [Template JSON - Property Group](#template-json---property-group)
        * [Template JSON - Property](#template-json---property)
        * [Template JSON - Data Object](#template-json---data-object)
        * [Template JSON - Validators](#template-json---validators)
    * [Template JSON Example](#template-json-example)
* [Updated Administrative Services](#updated-administrative-services)
    * [Obtaining an App Representation for Editing](#obtaining-an-app-representation-for-editing)
    * [Obtaining App Information for Job Submission](#obtaining-app-information-for-job-submission)

# Overview

The updated endpoints used by the DE accept and produce a somewhat simplified
JSON format. These endpoints currently only work with template JSON. Please use
the original endpoints, documented in the [admin ednpoints document](admin.md)
for more information about these endpoints.

## Template JSON

This section describes the JSON format accepted by the `/secured/update-app`
endpoint and produced by the `/export-app/{app-id}`, `/secured/app/{app-id}`,
and `/secured/edit-app/{app-id}` endpoints. The app integration utility in the
DE uses the `/secured/update-app` and `/secured/edit-app/{app-id}` endpoints to
update apps that are being edited and to retrieve the JSON representation of an
app to edit, respectively. The job launch utility in the DE uses the
`/secured/app/{app-id}` endpoint to retrieve a JSON representation of a single-
or multi-step app in preparation for job submission. This service produces JSON
in the same format as the `/secured/edit-app/{app-id}` endpoint, but it flattens
multi-step apps so that they appear to be single-step apps. This is done because
the job submission utility doesn't know or care how many steps are in an app.

### Template JSON - Template

<table>
    <thead>
        <tr>
            <th>Accepted Names</th>
            <th>Description</th>
            <th>Produced Name</th>
            <th>Required</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>id</td>
            <td>a UUID that is used to identify the template</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the name of the template</td>
            <td>name</td>
            <td>no</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the display name of the template</td>
            <td>label</td>
            <td>no</td>
        </tr>
        <tr>
            <td>component, component_id</td>
            <td>
                the identifier of the component used to execute the template
            </td>
            <td>component_id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the type of the template, which is a free-form string</td>
            <td>type</td>
            <td>no</td>
        </tr>
        <tr>
            <td>groups</td>
            <td>
                the list of property groups associated with the template (may be
                embedded within a `groups` JSON object
            </td>
            <td>groups</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

The `name` and `id` fields deserve some special attention. The `/update-app`
endpoint uses these fields to identify an app to update. If an identifier is
provided then the endpoint will attempt to match based on the identifier. If a
template with a matching identifier is found then that template will be updated.
If no identifier is provided then the service will attempt to find another
template with the same name. If exactly one template with the same name is found
then the endpoint will update the existing template. If multiple templates with
the same name are found then an error occurs. If no identifier is specified and
no template with the same name is found then the endpoint will generate a new ID
and import the template into the database as a new template. If an identifier of
`auto-gen` is specified then the endpoint willnot attempt to match by name or
ID. Instead, a new identifier will be generated and a new template will be
imported into the database.

The `label` field contains the display name to use for the template. If the
template label isn't provided then the template name will be used as its display
name. Strictly speaking neither the template name nor label is displayed in the
DE. Instead, the app name or label is displayed. Both import services use the
template name and label as the name and label of the automatically generated
single-step app corresponding to the template, however.

The `groups` field is required, but it's permitted to be an empty JSON array (or
an object containing an empty JSON array, also with the name `groups`).

### Template JSON - Property Group

<table>
    <thead>
        <tr>
            <th>Accepted Names</th>
            <th>Description</th>
            <th>Produced Names</th>
            <th>Required</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>name</td>
            <td>the name of the property group</td>
            <td>name</td>
            <td>no</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the label used to identify the property group in the UI</td>
            <td>label</td>
            <td>no</td>
        </tr>
        <tr>
            <td>id</td>
            <td>the property group identifier</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the type of the property group</td>
            <td>type</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>properties</td>
            <td>the list of properties in the property group</td>
            <td>properties</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>visible, isVisible</td>
            <td>
                a flag indicating whether or not the property group is displayed
                in the job submission UI
            </td>
            <td>isVisible</td>
            <td>no</td>
        </tr>
    </tbody>
</table>

As mentioned neither the name nor the label is required to import a property
group. It's a good idea to provide at least one of these fields, however,
because the UI attempts to display the label of the property group in the job
submission window. If no label was provided, it will display the name. Without a
name or a label, the panel devoted to the property group will not have a label
indicating its purpose.

As with most other objects imported into the database, the `id` field is
optional. If no identifier is provided, a new identifier will be generated for
the new property group.

The `type` field is required, but permitted to be empty. In fact, this field is
not currently used anywhere in the DE and is typically left blank. This field
will be made optional or removed at some point in the future. Similarly, the
`properties` field is required, but permitted to be an empty JSON array. The
`visible` or `isVisible` flag is optional and defaults to `true` if not
specified.

### Template JSON - Property

<table>
    <thead>
        <tr>
            <th>Accepted Names</th>
            <th>Description</th>
            <th>Produced Names</th>
            <th>Required</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>arguments</td>
            <td>described in detail below</td>
            <td>arguments</td>
            <td>no</td>
        </tr>
        <tr>
            <td>name</td>
            <td>described in detail below</td>
            <td>name</td>
            <td>no</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the prompt to display in the UI</td>
            <td>label</td>
            <td>no</td>
        </tr>
        <tr>
            <td>id</td>
            <td>the property identifier</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the property type name</td>
            <td>type</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>order</td>
            <td>the relative command-line order for the property</td>
            <td>order</td>
            <td>no</td>
        </tr>
        <tr>
            <td>defaultValue</td>
            <td>the default value of the property</td>
            <td>defaultValue</td>
            <td>no</td>
        </tr>
        <tr>
            <td>validators</td>
            <td>an array of validation rules</td>
            <td>validators</td>
            <td>no</td>
        </tr>
        <tr>
            <td>visible, isVisible</td>
            <td>indicates whether the property is displayed in the UI</td>
            <td>isVisible</td>
            <td>no</td>
        </tr>
        <tr>
            <td>omit_if_blank</td>
            <td>
                indicates whether the command-line option should be omitted if
                the property value is blank
            </td>
            <td>omit_if_blank</td>
            <td>no</td>
        </tr>
        <tr>
            <td>data_object</td>
            <td>the data object associated with an input or output property</td>
            <td>data_object</td>
            <td>no</td>
        </tr>
        <tr>
            <td>required</td>
            <td>
                a flag indicating whether or not a value is required for this
                property
            </td>
            <td>required</td>
            <td>no</td>
        </tr>
    </tbody>
</table>

The `arguments` field of a property is only used in cases where the user is
given a fixed number of values to choose from. This can occur for special types
of properties such as `TextSelection` or `IntegerSelection` properties. The
format of this field will be described in greater detail below.

The `name` field of a property is a special case. In most cases, this field
indicates the command-line option used to identify the property on the command
line. In these cases, the property is assumed to be positional and no
command-line option is used if the name is blank. For properties that specify a
limited set of selection values, however, this is not the case. Instead, the
property arguments specify both the command-line flag and the property value to
use for each option that is selected.

The `id` field is optional and a new identifier will be generated if none is
provided.

The `type` field must be present and must contain the name of one of the
property types defined in the database. You can get the list of defined and
undeprecated property types using the `/get-workflow-elements/property-types`
endpoint.

The `order` field is optional. If this field is not specified then the arguments
will appear on the command-line in the order in which they appear in the import
JSON. If you're not specifying the order, please be sure that the argument order
is unimportant for the tool being integrated.

The `validators` field contains a list of rules that can be used to verify that
property values entered by a user are valid. Note that in cases where the user
is given a list of possibilities to choose from, no validation rules are
required because the selection list itself can be used to validate the property
value.

The `omit_if_blank` field indicates whether the property should be omitted from
the command line completely if its value is null or blank. This is most useful
for optional arguments that use command-line flags in conjunction with a value.
In this case, it is an error to include the command-line flag without a
corresponding value. This flag indicates that the command-line flag should be
omitted if the value is blank. This can also be used for positional arguments,
but this flag tends to be useful only for trailing positional arguments.

The `data_object` field is currently not required, but it should be included any
time the property is used to identify an input or output file.

### Template JSON - Data Object

<table>
    <thead>
        <tr>
            <th>Accepted Names</th>
            <th>Description</th>
            <th>Produced Name</th>
            <th>Required</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>type, File, file_info_type</td>
            <td>described in detail below</td>
            <td>file_info_type</td>
            <td>no</td>
        </tr>
        <tr>
            <td>switch, option, param_option, cmdSwitch</td>
            <td>the flag to use on the command line</td>
            <td>cmdSwitch</td>
            <td>no</td>
        </tr>
        <tr>
            <td>format</td>
            <td>the format of the file</td>
            <td>format</td>
            <td>no</td>
        </tr>
        <tr>
            <td>retain</td>
            <td>
                indicates whether or not the data object should be copied back
                to the job output directory in iRODS
            </td>
            <td>retain</td>
            <td>no</td>
        </tr>
        <tr>
            <td>is_implicit</td>
            <td>
                indicates that the name of an output file is not specified on
                the command line
            </td>
            <td>is_implicit</td>
            <td>no</td>
    </tbody>
</table>

The `type` field contains the name of the information type that should be
associated with the file, which indicates what type of information is contained
within the file. You can use the `/get-workflow-elements/info-types` endpoint to
obtain the list of valid info types.

The `switch` field also currently only applies to input properties, and it
specifies the option flag used on the command line. If this field is blank or
null then, assuming the argument appears on the command line (more on this
later), the argument is assumed to be positional.

The `format` field indicates the format of the data in the file. You can use the
`/get-workflow-elements/formats` endpoint to get a list of valid formats.

The `retain` field indicates whether or not the data object should be copied
back into the job results folder in iRODS after the job completes.

The `is_implicit` field indicates whether or not the name of an output file is
implicitly determined by the app itself, and thus not included on the command
line. If the output file name is implicit then the output file name either must
always be the same or it must follow a naming convention that can easily be
matched with a glob pattern.

### Template JSON - Validators

<table>
    <thead>
        <tr>
            <th>Accepted Names</th>
            <th>Description</th>
            <th>Produced Name</th>
            <th>Required</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>type</td>
            <td>the type of the validation rule</td>
            <td>type</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>params</td>
            <td>the parameters to use when applying the validation rule</td>
            <td>params</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

The `type` of a validation rule describes how a property value should be
validated. For example, if the type is `IntAbove` then the property value
entered by the user must be an integer above a specific value, which is
specified in the parameter list. You can use the
`/get-workflow-elements/rule-types` endpoint to get a list of validation rule
types.

The `params` field contains the list of parameters to use when validating a
property value. For example, to ensure that a property contains a value that is
an integer greater than zero, you would use a validation rule of type `IntAbove`
along with a parameter list of `[0]`.

## Template JSON Example

Please see the [examples file](examples.md#updated-template-json).

# Updated Administrative Services

## Obtaining App Information for Job Submission

*Secured Endpoint:* GET /secured/app/{app-id}

The job submission utility in the DE uses this service to obtain a description
of the app in a format that is suitable for job submission. This JSON format is
identical to the template JSON format above, and multi-step apps are condensed
so that they appear to contain just one step. Please see
[Template JSON](#template-json) above for more information about the format of
the response body.

```
$ curl -s "http://by-tor:8888/secured/app/A750DD7B-7EBC-4809-B9EC-6F717220A1D1?user=nobody&email=nobody@iplantcollaborative.org" | python -mjson.tool
{
    "groups": [
        {
            "id": "BF640C7B-E5EA-4232-AC88-7F7D2768E3C1",
            "label": "Grp1",
            "name": "",
            "properties": [
                {
                    "arguments": [],
                    "defaultValue": null,
                    "description": "File input tool tip",
                    "id": "App Endpoint Test_2A72C63F-569A-4C6E-9572-E075F705DD3D",
                    "isVisible": true,
                    "label": "Input File",
                    "name": "-f",
                    "required": true,
                    "type": "FileInput",
                    "validators": []
                },
                {
                    "arguments": [],
                    "defaultValue": "",
                    "description": "TextBox tool tip",
                    "id": "App Endpoint Test_DA067538-535A-44FF-B927-0558DBD5E1D5",
                    "isVisible": true,
                    "label": "TextBox",
                    "name": "-b",
                    "required": false,
                    "type": "Text",
                    "validators": []
                },
                {
                    "arguments": [],
                    "defaultValue": "",
                    "description": "checkbox tool tip",
                    "id": "App Endpoint Test_01DBA927-0A02-48D2-9B85-CE77A66B2D63",
                    "isVisible": true,
                    "label": "Checkbox",
                    "name": "-c",
                    "required": false,
                    "type": "Flag",
                    "validators": []
                }
            ],
            "type": ""
        }
    ],
    "id": "A750DD7B-7EBC-4809-B9EC-6F717220A1D1",
    "label": "App Endpoint Test",
    "name": "App Endpoint Test",
    "disabled": false,
    "type": ""
}
```
