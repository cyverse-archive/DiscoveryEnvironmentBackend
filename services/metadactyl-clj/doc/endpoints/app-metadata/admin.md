# Table of Contents

* [Overview](#overview)
    * [Deployed Components](#deployed-components)
    * [Templates](#templates)
        * [Property Groups](#property-groups)
        * [Properties](#properties)
        * [Property Types](#property-types)
        * [Value Types](#value-types)
        * [Validators](#validators)
        * [Rules](#rules)
        * [Rule Arguments](#rule-arguments)
        * [Data Objects](#data-objects)
        * [Data Object Multiplicity](#data-object-multiplicity)
        * [Data Sources](#data-sources)
        * [Info Types](#info-types)
        * [Data Formats](#data-formats)
    * [Apps](#apps)
        * [Transformation Activities](#transformation-activities)
        * [Transformation Activity References](#transformation-activity-references)
        * [Ratings](#ratings)
        * [Suggested Groups](#suggested-groups)
        * [Transformation Steps](#transformation-steps)
        * [Transformations](#transformations)
        * [Input/Output Mappings](#inputoutput-mappings)
        * [Data Object Mappings](#data-object-mappings)
        * [Transformation Values](#transformation-values)
        * [Template Groups](#template-groups)
* [App Metadata JSON](#app-metadata-json)
    * [Template JSON](#template-json)
        * [Template JSON - Template](#template-json---template)
        * [Template JSON - Implementation](#template-json---implementation)
        * [Template JSON - Property Group](#template-json---property-group)
        * [Template JSON - Property](#template-json---property)
        * [Template JSON - Data Object](#template-json---data-object)
        * [Template JSON - Validator](#template-json---validator)
        * [Template JSON - Rule](#template-json---rule)
    * [Template JSON Example](#template-json-example)
    * [App JSON](#app-json)
        * [App JSON - Deployed Components](#app-json---deployed-components)
            * [App JSON - Deployed Components - Implementation](#app-json---deployed-components---implementation)
            * [App JSON - Deployed Components - Test Data](#app-json---deployed-components---test-data)
        * [App JSON - Templates](#app-json---templates)
            * [App JSON - Templates - Property Group](#app-json---templates---property-group)
            * [App JSON - Templates - Property](#app-json---templates---property)
            * [App JSON - Templates - Data Object](#app-json---templates---data-object)
            * [App JSON - Templates - Validator](#app-json---templates---validator)
            * [App JSON - Templates - Rule](#app-json---templates---rule)
        * [App JSON - Analyses](#app-json---analyses)
            * [App JSON - Analyses - Steps](#app-json---analyses---steps)
            * [App JSON - Analyses - Mappings](#app-json---analyses---mappings)
            * [App JSON - Analyses - Implementation](#app-json---analyses---implementation)
    * [App JSON Example](#app-json-example)
    * [App JSON for UI](#app-json-for-ui)
        * [App JSON for UI - Property Group](#app-json-for-ui---property-group)
        * [App JSON for UI - Property](#app-json-for-ui---property)
        * [App JSON for UI - Validator](#app-json-for-ui---validator)
        * [App JSON for UI - Rule](#app-json-for-ui---rule)
    * [App JSON for UI Example](#app-json-for-ui-example)
* [App Metadata Administration Services](#app-metadata-administration-services)
    * [Exporting an Analysis](#exporting-an-analysis)

# Overview

The app metadata model used by the DE has three major types of components:
_deployed components_, _templates_ and _apps_.

Deployed components represent tools that have been deployed within the Discovery
Environment. Currently, these refer to command-line tools that can be executed
in the Discovery Environment, either from within the Discovery Environment's
Condor cluster or on the HPC resources at TACC.

Templates represent a single use of a deployed component, including command-line
arguments and options that the deployed component supports. One important thing
to keep in mind is that a template does not have to describe every possible use
of a deployed component and it is common for multiple templates to be used to
describe distinct usages of a single deployed component. For example, it would
be perfectly reasonable to have two templates for the Unix utility, `tar`: one
for extracting files from a tarball and another for building a tarball. The
structure of a template is fairly deep and complex, and will be described in
more detail later.

Apps, represent groups of one or more templates that can be run by a user from
within the Discovery Environment. A template cannot be used directly by a user
without being included in an app. And a single app may contain multple templates
strung together into a pipeline. Note that the `/import-template` service in
metadactyl automatically generates a single-step app containing that template.
This is done as a convenience because single-step apps are common.

A fourth type of component, _notification sets_, was supported at one time, but
it is no longer supported as of DE version 1.8. Some vestiges of notification
sets still exist, but they are no longer used by the DE. All remaining support
for them will be removed at some point in the future.

It should be noted that a lot of tables in the database have elements that
aren't being used in the Discovery Environments. They may be populated using the
app metadata import services, but they're ignored. It would be nice to get rid
of these fields, but they're being retained for the time being because the
Hibernate object mappings that we're using currently requires them to be there.
The unused fields will be removed if and when we get around to revamping the
database schema.

## Deployed Components

Each deployed component contains information about a command-line tool that is
deployed in the Discovery Environment. This includes the path to the directory
containing the executable file, the name of the executable file and several
pieces of information to determine how the tool is executed:

<table border="1">
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the executable file.</td>
        </tr>
        <tr>
            <td>Location</td>
            <td>The path to the directory containing the executable file.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the tool.</td>
        </tr>
        <tr>
            <td>Version</td>
            <td>The tool version.</td>
        </tr>
        <tr>
            <td>Attribution</td>
            <td>
                Information about the people or entities that created the tool.
            </td>
        </tr>
        <tr>
            <td>Integration Data</td>
            <td>Information related to the tool installation request.</td>
        </tr>
        <tr>
            <td>Tool Type</td>
            <td>The type of the tool.</td>
        </tr>
    </tbody>
</table>

The integration data and tool type both deserve special attention. The
integration data includes the name and email address of the person who requested
that the tool be installed along with example input files and expected output
files for a test run of the tool. The tool type indicates where the utility
runs. There are currently two available tool types: `executable`, which
indicates that the tool runs on the Discovery Environment's Condor cluster, and
`fAPI`, which indicates that the job is submitted to the Foundation API.

## Templates

As mentioned above, each template describes one possible use of a deployed
component. This includes descriptions of all of the options and command-line
arguments required for that use of the deployed component. The template
structure is nested fairly deeply, so we'll start with a brief description of
each level in the structure:

<table>
    <thead>
        <tr>
            <th>Structure Level</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Template</td>
            <td>The top level of the template structure</td>
        </tr>
        <tr>
            <td>Property Group</td>
            <td>
                Represents a group of related options or command-line arguments.
            </td>
        </tr>
        <tr>
            <td>Property</td>
            <td>Represents a single option or command-line argument.</td>
        </tr>
        <tr>
            <td>Property Type</td>
            <td>Indicates the type of information accepted by the property.</td>
        </tr>
        <tr>
            <td>Value Type</td>
            <td>
                Indicates the type of value associated with a property type.
            </td>
        </tr>
        <tr>
            <td>Validator</td>
            <td>Indicates how property values should be validated.</td>
        </tr>
        <tr>
            <td>Rule</td>
            <td>Represents one rule for validating a property value.</td>
        </tr>
        <tr>
            <td>Rule Type</td>
            <td>Indicates how rule arguments should be interpreted.</td>
        </tr>
        <tr>
            <td>Rule Argument</td>
            <td>Provies an argument to a rule.</td>
        </tr>
        <tr>
            <td>Data Object</td>
            <td>Represents one or more input or output files.</td>
        </tr>
        <tr>
            <td>Multiplicity</td>
            <td>
                Indicates the number of input or output files accepted or
                produced by a tool for a specific data object.
            </td>
        </tr>
        <tr>
            <td>Data Source</td>
            <td>
                Indicates where the data for an output data object originates.
            </td>
        <tr>
            <td>Info Type</td>
            <td>
                Represents the type of information in an input or output file.
            </td>
        </tr>
        <tr>
            <td>Data Format</td>
            <td>Represents the format of an input or output file.</td>
        </tr>
    </tbody>
</table>

There are several fields associated with the top level of the template
structure, most of which are largely ignored. Aside from the identifier, the
only field that is commonly used is the deployed component identifier. The name
field is used by the import service in some cases, which will be described
later, but it is not used otherwise.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        <tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the template.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the template.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>A display label for the template.</td>
        </tr>
        <tr>
            <td>Type</td>
            <td>The type of the template.</td>
        </tr>
        <tr>
            <td>Deployed Component Identifier</td>
            <td>A reference to the deployed component.</td>
        </tr>
    </tbody>
</table>

### Property Groups

A property group is nothing more than a way to group related options and
command-line arguments. This allows the Discovery Environment to group related
things together in the user interface.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the property group.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the property group.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>The display label for the property group.</td>
        </tr>
        <tr>
            <td>Property Group Type</td>
            <td>The type of the property group.</td>
        </tr>
        <tr>
            <td>Visibility Flag</td>
            <td>Indicates whether or not the group is displayed in the DE.</td>
        </tr>
    </tbody>
</table>

The property group label is used as the label of an accordion panel in the UI
that is generated for the template. The visibility flag indicates whether or not
the property group should be displayed in the UI. Hidden property groups can be
useful for grouping properties that are not configurable by the end user.

### Properties

A property represents a single option or command-line argument for a tool. This
is where things start to get a little more interesting. Properties come in
several different types, which indicate the type of information accepted by the
property.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The command-line flag to use for the property.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the property.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>
                The display label for the property. This field determines how
                the widget associated with the property will be labeled in the
                UI.
            </td>
        </tr>
        <tr>
            <td>Default Value</td>
            <td>
                The value used for the property if none is provided. If this
                field is blank then the default value is also blank.
            </td>
        </tr>
        <tr>
            <td>Visibility Flag</td>
            <td>
                Indicates if the property is visible in the UI. Hidden
                properties are useful for settings that have to be passed to
                the tool but are not configruable by end users.
            </td>
        </tr>
        <tr>
            <td>Order Index</td>
            <td>
                Indicates the relative command-line order of the property. For
                example, if a property with an order index of 1 will be included
                first on the command line and a property with an order index of
                2 will be second.
            </td>
        </tr>
        <tr>
            <td>Property Type</td>
            <td>
                The type of information accepted by the property. In the
                database, this field is a foreign key into a table that lists
                all of the property types that are supported by the DE.
            </td>
        </tr>
        <tr>
            <td>Validator</td>
            <td>
                Information about how to validate property values, if
                applicable.
            </td>
        </tr>
        <tr>
            <td>Data Object</td>
            <td>
                Information about an output or input file associated with the
                property, if applicable.
            </td>
        </tr>
        <tr>
            <td>Omit if Blank Flag</td>
            <td>
                Indicates whether or not the property should be omitted from the
                command line if its value is blank.
            </td>
        </tr>
    </tbody>
</table>

### Property Types

A property type indicates both how an option or command-line argument is
presented to the user and what type of information is accepted by it. There must
be one property type per user interface widget available in the Discovery
Environment. Because code has to be written for each property type, users may
not enter new property types. Instead, property types are only added by database
initialization and conversion scripts.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name display name of the property type.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the property type.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>Not used.</td>
        </tr>
        <tr>
            <td>Deprecated</td>
            <td>
                Indicates whether or not the property type may be used in new
                apps.
            </td>
        </tr>
        <tr>
            <td>Display Order</td>
            <td>
                Determines the order in which property types are listed in the
                DE.
            </td>
        </tr>
        <tr>
            <td>Value Type</td>
            <td>
                Indicates the type of value associated with the property type,
                which determines what types of rules can be applied to
                proerties of this type.
            </td>
        </tr>
    </tbody>
</table>

### Value Types

A value type indicates what type of value is associated with a specific type of
property, which determines which types of rules may be used to validate
properties of a given type. As with property types, value types may not be added
to the Discovery Environment by end users.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the value type.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the value type.</td>
        </tr>
    </tbody>
</table>

### Validators

A validator determines how user input for a property is validated. Not all
properties have to be validated, so some of them will not have validators
associated with them.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the validator.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the validator.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>The validator label.</td>
        </tr>
        <tr>
            <td>Required</td>
            <td>
                Indicates whether or not the property associated with the
                validator requires a non-blank value.
            </td>
        </tr>
        <tr>
            <td>Rules</td>
            <td>
                The list of validation rules associated with this validator.
            </td>
    </tbody>
</table>

### Rules

A rule provides a way to validate a property value. Multiple rules may be
associated with a single validator so that property values may be validated in
multiple steps or in multiple ways.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the rule.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the rule.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>The rule label.</td>
        </tr>
        <tr>
            <td>Rule Type</td>
            <td>Indicates how the validation is performed.</td>
        </tr>
        <tr>
            <td>Value Types</td>
            <td>The types of values that the rule may be applied to.</td>
        </tr>
        <tr>
            <td>Rule Arguments</td>
            <td>Arguments that are passed to the rule.</td>
        </tr>
    </tbody>
</table>

### Rule Arguments

A rule argument provides a single argument to a rule. For example, a property
whose value should be an integer greater than 5 could be validated using the
rule type `IntAbove` with a single argument of `5`.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Argument Value</td>
            <td>The value used for this argument.</td>
        </tr>
        <tr>
            <td>Hibernate ID</td>
            <td>
                This field is misnamed; it determines the order in which the
                arguments are passed to the rule.
            </td>
        </tr>
    </tbody>
</table>

### Data Objects

A data object describes either an input file that is accepted by a deployed
component or an output file that is produced by one. Data objects are closely
related to properties, but were listed separately from properties at one point
in time. Because of that, there are a lot of redundant fields in properties and
data objects.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>
                This field is overloaded. It'll be described in more detail
                later.
            </td>
        </tr>
        <tr>
            <td>Label</td>
            <td>The data object label.</td>
        </tr>
        <tr>
            <td>Order</td>
            <td>
                The relative order in which the data object appears on the
                command line.
            </td>
        </tr>
        <tr>
            <td>Switch</td>
            <td>The command line flag used for the data object.</td>
        </tr>
        <tr>
            <td>Info Type</td>
            <td>
                Indicates the type of information represented by the data
                object.
            </td>
        </tr>
        <tr>
            <td>Data Format</td>
            <td>
                Indicates the format of the information contained in the data
                object.
            </td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the data object.</td>
        </tr>
        <tr>
            <td>Required</td>
            <td>
                Indicates whether an input data object is required or a name for
                an output data object must be provided for an app to run.
            </td>
        </tr>
        <tr>
            <td>Multiplicity</td>
            <td>
                Indicates the number of files accepted or produced by the app
                for a data object.
            </td>
        </tr>
        <tr>
            <td>Retain</td>
            <td>
                Indicates whether or not an input data object should be retained
                by the Discovery Environment.
            </td>
        </tr>
        <tr>
            <td>Implicit</td>
            <td>
                Indicates that the data object is not specified on the command
                line.
            </td>
        </tr>
        <tr>
            <td>Data Source</td>
            <td>
                Indicates where the data for an output data object originates.
            </td>
        </tr>
    </tbody>
</table>

### Data Object Multiplicity

The multiplicity indicates how many files are accepted or produced by an app for
a data object. The currently available multiplicities are `Many`, `Single` and
`Folder`. `Many` indicates that multiple files are accepted or produced by the
app. For input files, it must be possible to pass the names of the files to the
app on the command line. For output files, the names of the files must match a
glob expression so that the DE can retrieve them after the job completes.
`Single` indicates that only one file is either accepted or produced by the app.
`Folder` indicates that the contents of a folder on the file system are either
accepted or produced by the app.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the multiplicity.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>The dislpay label for the multiplicity</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the multiplicity.</td>
        </tr>
        <tr>
            <td>Type Name</td>
            <td>The name of the control type used for the multiplicity.</td>
        </tr>
    </tbody>
</table>

### Data Sources

The data source associated with an output data source indicates where the data
in the data object originates. The currently available data sources are `File`,
`Standard Output` and `Standard Error Output`. A data source of `File` indicates
that the app produces the file directly. A data source of `Standard Output`
indicates that the app sends the data to the standard output stream. Finally, a
data source of `Standard Error Output` indicates that the app sends the data to
the standard error output stream.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <body>
        <tr>
            <td>name</td>
            <td>The name of the data source.</td>
        </tr>
        <tr>
            <td>label</td>
            <td>The display name for the data source.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the data source.</td>
        </tr>
    </tbody>
</table>

### Info Types

Info types represent the type of information contained in a data object. Some
examples of this are phylogenetic tree files, and plain text files.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the information type.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>The display label for the information type.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the information type.</td>
        </tr>
        <tr>
            <td>Deprecated</td>
            <td>
                Indicates whether the information type may be used in new apps.
            </td>
        </tr>
        <tr>
            <td>Display Order</td>
            <td>The order in which info types appear in drop-down lists.</td>
        </tr>
    </tbody>
</table>

### Data Formats

Data formats indicate the format of data in a data object. Some examples of this
are FASTA and Genbank.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the data format.</td>
        </tr>
        <tr>
            <td>Label</td>
            <td>the display label for the data format.</td>
        </tr>
        <tr>
            <td>Display Order</td>
            <td>The order in which data formats appear in drop-down lists.</td>
        </tr>
    </tbody>
</table>

## Apps

In the discovery environment, an app is a runnable collection of one or more
templates. If an app contains more than one template, typically the output files
from all but the last step are fed into subsequent steps as input files so that
the data being processed may be transformed in multiple steps.  Haps contain the
following components:

<table>
    <thead>
        <tr>
            <th>Component Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Transformation Activity</td>
            <td>The component that describes the app itself.</td>
        </tr>
        <tr>
            <td>Transformation Activity References</td>
            <td>Links to references related to the app.</td>
        </tr>
        <tr>
            <td>Ratings</td>
            <td>Information about ratings that users have given to apps.</td>
        </tr>
        <tr>
            <td>Suggested Groups</td>
            <td>
                References to app categories that the user suggests for an app.
            </td>
        </tr>
        <tr>
            <td>Transformation Steps</td>
            <td>A single step in an app.</td>
        </tr>
        <tr>
            <td>Transformation</td>
            <td>A data transformation performed in a step.</td>
        </tr>
        <tr>
            <td>Input/Output Mapping</td>
            <td>
                Maps outputs of one step to inputs of a subsequent step.
            </td>
        </tr>
        <tr>
            <td>Data Object Mapping</td>
            <td>
                Maps a single output of one step to a single input of a
                subsequent step.
            </td>
        </tr>
        <tr>
            <td>Transformation Values</td>
            <td>
                Values that should be applied to properties without prompting
                the user.
            </td>
        </tr>
        <tr>
            <td>Template Groups</td>
            <td>A categorization for one or more apps.</td>
        </tr>
    </tbody>
</table>

### Transformation Activities

A transformation activity is the top-level element of an app. It contains the
app name and identifier along with references to the steps that comprise the
app.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the app.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the app.</td>
        </tr>
        <tr>
            <td>Workspace</td>
            <td>The user's workspace that the app is associated with.</td>
        </tr>
        <tr>
            <td>Type</td>
            <td>A short description of what the app does.</td>
        </tr>
        <tr>
            <td>Deleted</td>
            <td>Indicates whether the app should be displayed in the DE.</td>
        </tr>
        <tr>
            <td>Integration Data</td>
            <td>Information about the user who integrated the app.</td>
        </tr>
        <tr>
            <td>Wiki URL</td>
            <td>A link to the app's documentation.</td>
        </tr>
        <tr>
            <td>Integration Date</td>
            <td>The date that the app was integrated into the DE.</td>
        </tr>
        <tr>
            <td>Disabled</td>
            <td>Indicates whether the app can be executed.</td>
        </tr>
        <tr>
            <td>Edited Date</td>
            <td>The date that the app was most recently edited.</td>
        </tr>
        <tr>
            <td>Ratings</td>
            <td>The ratings that users have given to the app.</td>
        </tr>
        <tr>
            <td>Suggested Groups</td>
            <td>A list of groups that the intergator suggests for the app.</td>
        </tr>
        <tr>
            <td>Transformation Activity Mappings</td>
            <td>A list of all of the input/output mappings for the app.</td>
        </tr>
        <tr>
            <td>Transformation Activity References</td>
            <td>A list of links to references related to the app.</td>
        </tr>
        <tr>
            <td>Transformation Steps</td>
            <td>The list of steps in the app.</td>
        </tr>
    </tbody>
</table>

A few fields here deserve some special attention. The _Deleted_ field indicates
whether or not the app should be considered deleted in the DE, which typically
does not completely remove apps from the database for a couple of different
reasons. First, the app may have been used in user's experiments in the past, so
it may be necessary to retrieve information about the app so that information
about the app is still accessible. Sedond, it may be necessary to restore an app
at some point in the future.

The _Disabled_ field indicates whether or not the app should be considered
disabled. A disabled app will appear in the DE, but cannot be executed. This
allows DE administrators to temporarilty prevent an app from being executed,
which can be useful for apps that are currently not functioning, for example.

The _Suggested Groups_ field contains the list of groups that an app integrator
thinks is appropriate for the app when an app is first made available for public
use. When the app is initially made public, it is placed in the _Beta_ category.
After some period of time, a DE administrator might decide to move the app out
of the _Beta_ category and into another, more permanent, category. This field
serves as a list of suggestions for where the DE administrator might put the
app.

The _Transformation Activity Mappings_ field contains a list of all of the
input/output mappings associated with an app. This relationship serves merely as
a convenience for the code that submits jobs for execution; the input/output
mappings can also be accessed via the list of transformation steps.

### Transformation Activity References

Transformation Activity References contain links to reference information
related to an app. For example, apps that use tools in the FASTX Toolkit tend to
reference its home page.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Reference Text</td>
            <td>The link to the reference material.</td>
        </tr>
    </tbody>
</table>

The _Reference Text_ field is a text field, so it would be possible to store
actual reference text in this field. In practice, however, this is not commonly
done. Instead, this field normally just contains a link to the actual reference
information.

### Ratings

DE Users have the ability to rate apps based on perceived usefulness. This
entity contains information about ratings that users have given to apps.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>User</td>
            <td>The user who rated the app.</td>
        </tr>
        <tr>
            <td>App</td>
            <td>The app being rated.</td>
        </tr>
        <tr>
            <td>Rating</td>
            <td>The numeric rating given to the app.</td>
        </tr>
        <tr>
            <td>Comment ID</td>
            <td>The identifier of the comment in Confluence.</td>
        </tr>
    </tbody>
</table>

### Suggested Groups

As mentioned above, suggested groups are recommendations of app categories that
the app integrator thinks might be appropriate for the app when it's moved from
the _Beta_ category. When a DE administrator moves an app from the _Beta_
category to a more permanent category, one of the categories in this list of
suggestions is typically chosen.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Transformation Activity</td>
            <td>The app that is being made public.</td>
        </tr>
        <tr>
            <td>Template Group</td>
            <td>The suggested app category</td>
        </tr>
    </tbody>
</table>

### Transformation Steps

A transformation step represents a single step in an app. Each step eventually
maps to a single template. For multi-step apps, each step is referenced by a set
of input/output mappings, which are used to map the outputs from one step to the
inputs of a subsequent step.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the step.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the step.</td>
        </tr>
        <tr>
            <td>Transformation</td>
            <td>The transformation, which refers to the template.</td>
        </tr>
    </tbody>
</table>

### Transformations

A transformation represents a way to customize a template by specifying constant
values for certain properties. The original purpose of a transformation was to
foster template reuse, but transformations tend not to be used for this purpose
in practice. Instead, transformations just serve as an empty link from a
transformation step to a template.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The name of the transformation.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the transformation.</td>
        </tr>
        <tr>
            <td>Template ID</td>
            <td>
                The external identifier of the template used by the
                transformation. This is often a point of confusion because the
                template ID is not listed as a foreign key in the database.
            </td>
        </tr>
        <tr>
            <td>Transformation Values</td>
            <td>The property values specified by the transformation.</td>
        </tr>
    </tbody>
</table>

### Input/Output Mappings

Input/output mappings only apply to multi-step apps; they indicate which outputs
of one step should be applied to which inputs of a subsequent step.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Source</td>
            <td>The source transformation step</td>
        </tr>
        <tr>
            <td>Target</td>
            <td>The target transformation step</td>
        </tr>
        <tr>
            <td>Data Object Mappings</td>
            <td>The actual data object mappings.</td>
        </tr>
    </tbody>
</table>

### Data Object Mappings

Data object mappings only apply to multi-step apps; they map an output of one
step to the input of a subsequent step.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Mapping</td>
            <td>The associated input/output mapping.</td>
        </tr>
        <tr>
            <td>Input</td>
            <td>The external data object ID for the input.</td>
        </tr>
        <tr>
            <td>Output</td>
            <td>The external data object ID for the output.</td>
        </tr>
    </tbody>
</table>

### Transformation Values

A transformation value specifies a constant value for a property inside a
template referenced by a transformation.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Transformation</td>
            <td>The transformation to which this value applies.</td>
        </tr>
        <tr>
            <td>Value</td>
            <td>The constant property value.</td>
        </tr>
        <tr>
            <td>Property</td>
            <td>
                The identifier of the property to assign the constant value to.
            </td>
        </tr>
    </tbody>
</table>

### Template Groups

Template groups are somewhat misnamed. A template group doesn't contain a set of
templates; it contains a set of apps. Template groups can also contain other
template groups so that apps can be grouped hierarchically. Template groups are
currently restricted to containing either apps or subgroups, but not both.

<table>
    <thead>
        <tr>
            <th>Field Name</th>
            <th>Field Value</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>The display name of the template group.</td>
        </tr>
        <tr>
            <td>Description</td>
            <td>A brief description of the template group.</td>
        </tr>
        <tr>
            <td>Workspace</td>
            <td>The workspace that the template group belongs to.</td>
        </tr>
        <tr>
            <td>Subgroups</td>
            <td>The list of subgroups in the current group.</td>
        </tr>
        <tr>
            <td>Templates</td>
            <td>The list of templates in the current group.</td>
        </tr>
    </tbody>
</table>

# App Metadata JSON

The import and export services use JSON representations of the template and app
structures described above. The JSON format will be documented in detail below
using a separate table for each level in the structure.

The column headers deserve a little special attention. The _Accepted Names_
column contains a list of JSON field names that are accepted by the import
services. The _Description_ column contains a brief description of each field.
The _Produced Name_ column contains the field name produced by the export
service. The _Required_ column contains information about whether or not a field
is required.

## Template JSON

This section describes the JSON format accepted by the `/import-template` and
`/update-template` endpoints and produced by the `/export-template` endpoint.
These endpoints are provided for importing and exporting single-step apps. They
are intended to be convenience endpoints for dealing with single-step apps in
order to avoid dealing with the more complicated JSON format associated with the
`/*-workflow` endpoints.

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
        <tr>
            <td>implementation</td>
            <td>information about the person who integrated the template</td>
            <td>N/A</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

The `name` and `id` fields deserve some special attention. The
`/import-template` and `/update-template` endpoints both treat these fields the
same way. If an identifier is provided then the endpoints will attempt to match
based on the identifier. If a template with a matching identifier is found then
the `/import-template` endpoint will refuse to do anything and the
`/update-template` endpoint will update the existing template. If no identifier
is provided then the services attempt to find another template with the same
name. If exactly one template with the same name is found then the
`/import-template` interface will refuse to proceed and the `/update-template`
endpoint will update the existing template. If multiple templates with the same
name are found then neither endpoint will do anything. If no identifier is
specified and no template with the same name is found then both endpoints will
generate a new ID and import the template into the database as a new template.
If an identifier of `auto-gen` is specified then neither endpoint will attempt
to match by name or ID. Instead, a new identifier will be generated and a new
template will be imported into the database.

The `implementation` field is only used for the template import services, and is
not generated by the template export services. Note that the means that a
template that has been exported via the `/export-template` endpoint cannot be
imported using either the `/import-template` or `/update-template` endpoints.
Please use the `/export-workflow` and `/force-update-workflow` endpoints for
copying apps from one DE deployment to another.

The `label` field contains the display name to use for the template. If the
template label isn't provided then the template name will be used as its display
name. Strictly speaking neither the template name nor label is displayed in the
DE. Instead, the app name or label is displayed. Both import services use the
template name and label as the name and label of the automatically generated
single-step app corresponding to the template, however.

The `groups` field is required, but it's permitted to be an empty JSON array (or
an object containing an empty JSON array).

### Template JSON - Implementation

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
            <td>implementor_email</td>
            <td>the email address of the implementor</td>
            <td>N/A</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>implementor</td>
            <td>the name of the implementor</td>
            <td>N/A</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

Both the implementor name and email address are required, and attempts to import
or update templates will fail if either is not provided. The import services
attempt to avoid adding duplicate integration data objects by looking for a
matching entry in the database. If a match is found then another link to the
matching entry is created. Otherwise, a new entry is added.

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
            <td>value, default_value, defaultvalue</td>
            <td>the default value of the property</td>
            <td>value</td>
            <td>no</td>
        </tr>
        <tr>
            <td>validator</td>
            <td>the validator describing how to validate property values</td>
            <td>validator</td>
            <td>no</td>
        </tr>
        <tr>
            <td>visible, isVisible</td>
            <td>indicates whether the property is displayed in the UI</td>
            <td>isVisible</td>
            <td>no</td>
        </tr>
        <tr>
            <td>omit_if_blank, omitIfBlank</td>
            <td>
                indicates whether the command-line option should be omitted if
                the property value is blank
            </td>
            <td>omitIfBlank</td>
            <td>no</td>
        </tr>
        <tr>
            <td>data_object</td>
            <td>the data object associated with an input or output property</td>
            <td>data_object</td>
            <td>no</td>
        </tr>
    </tbody>
</table>

The `name` field of a property is a special case. In most cases, this field
indicates the command-line option used to identify the property on the command
line. In these cases, the property is assumed to be positional and no
command-line option is used if the name is blank. For properties that specify a
limited set of selection values, however, this is not the case. Instead, the
validation rules specify both the command-line flag and the property value to
use for each option that is selected. This will be described in greater detail
later.

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

The `value`, `default_value` or `defaultvalue` field is optional, as is the
`validator` field, in both cases the fields are left unspecified if nothing is
provided in the import JSON. The `visible` or `isVisible` flag is optional and
defaults to `true`.

The `omit_if_blank` or `omitIfBlank` field indicates whether the property should
be omitted from the command line completely if its value is null or blank. This
is most useful for optional arguments that use command-line flags in conjunction
with a value. In this case, it is an error to include the command-line flag
without a corresponding value. This flag indicates that the command-line flag
should be omitted if the value is blank. This can also be used for positional
arguments, but this flag tends to be useful only for trailing positional
arguments.

The `data_object` field is currently not required, but it should be included any
time the property type is `Input` or `Output`. Without the data object an input
or output property is useless. The import services should eventually be modified
to verify that this field is present whenever not having this field would cause
the imported template to be invalid. In the meantime, please be careful when
dealing with this field.

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
            <td>id</td>
            <td>the data object identifier</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>name, label, output_filename</td>
            <td>described in detail below</td>
            <td>
                name (for input properties),
                output_filename (for output properties)
            </td>
            <td>no</td>
        </tr>
        <tr>
            <td>type, File, file_info_type</td>
            <td>described in detail below</td>
            <td>file_info_type</td>
            <td>no</td>
        </tr>
        <tr>
            <td>multiplicity</td>
            <td>indicates the number of files accepted or produced</td>
            <td>multiplicity</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>order</td>
            <td>specifies the relative order in the arugment list</td>
            <td>order</td>
            </td>no</td>
        </tr>
        <tr>
            <td>switch, option, param_option, cmdSwitch</td>
            <td>the flag to use on the command line</td>
            <td>cmdSwitch</td>
            <td>no</td>
        </tr>
        <tr>
            <td>required</td>
            <td>
                indicates whether or not an output name or an input must be
                specified
            </td>
            <td>required</td>
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

A lot of the fields in data objects are vestigial remains from the time when
data objects were referenced directly from within the template instead of being
associated with a property, which is no longer the case. The import and export
JSON could be simplified quite a bit, but we haven't had the time to devote to
the simplification yet.

As usual, the `id` field is optional and an identifier will be generated if none
is provided.

The `name` field is overloaded in data objects just like it is in other template
components, which can be a cause for confusion at times. For input properties,
the name is used to specify the prompt to use in the UI, meaning that it serves
essentially the same purpose as the `label` field for a property, which is why
`label` is used as a synonym for `name` in this field. For output properties,
the name was once used as the default output file name. Fortunately, this is no
longer the case. Instead, the name field is ignored for output data objects and
the default value field from the associated property is used instead.

The `type` field contains the name of the information type that should be
associated with the file, which indicates what type of information is contained
within the file. You can use the `/get-workflow-elements/info-types` endpoint to
obtain the list of valid info types.

The `multiplicity` field indicates how many files are either consumed or
produced by an app for a specific property. The available multiplicity names are
currently `single`, `many` and `folder`, representing single files, multiple
files and a folder containing one or more files, respectively.

The `order` field currently only applies to input properties, which still use
the data object to format the job request that is sent down to the JEX. All
other properties, including output properties, use the `order` field in the
property to determine the relative command-line order.

The `switch` field also currently only applies to input properties, and it
specifies the option flag used on the command line. If this field is blank or
null then, assuming the argument appears on the command line (more on this
later), the argument is assumed to be positional.

For input properties, the `required` parameter indicates whether or not an input
file is required for an app to run. For output properties, this parameter
indicates whether or not the name of an output file must be specified in order
for an app to run.

The `retain` field indicates whether or not the data object should be copied
back into the job results folder in iRODS after the job completes.

The `is_implicit` field indicates whether or not the name of an output file is
implicitly determined by the app itself, and thus not included on the command
line. If the output file name is implicit then the output file name either must
always be the same or it must follow a naming convention that can easily be
matched with a glob pattern.

### Template JSON - Validator

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
            <td>the validator identifier</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the validator name</td>
            <td>name</td>
            <td>no</td>
        </tr>
        <tr>
            <td>required</td>
            <td>true if the field is required to have a value</td>
            <td>required</td>
            <td>no</td>
        </tr>
        <tr>
            <td>rules</td>
            <td>the list of validation rules associated with the validator</td>
            <td>rules</td>
            <td>no</td>
        </tr>
    </tbody>
</table>

The `name` field is optional and largely irrelevant.

The `required` field indicates whether or not a non-blank value is required for
the property in order for the app to function property. If this field is set to
true and the user does not enter a value then the UI should prevent the job from
being submitted.

### Template JSON - Rule

Rules are described in the JSON a little bit differently from other metadata
components. Instead of having a dedicated JSON format, each rule is described as
a JSON object containing exactly one member. The name of the member is the name
of the type of rule being used for validation. You can get the list of accepted
rule types using the `/get-workflow-elements/rule-types` endpoint. The value of
the member is a list of arguments to pass to that rule. An example of a valid
rule specification would be:

```json
{
    "IntRange": [ 0, 42 ]
}
```

## Template JSON Example

Please see the [examples file](examples.md#template-json).

## App JSON

The app JSON is a more generalized version of the Template JSON that can
correctly handle multi-step apps among other things. The top level of this
format is a JSON object containing up to four fields:

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
            <td>components</td>
            <td>zero or more deployed component definitions</td>
            <td>components</td>
            <th>no</td>
        </tr>
        <tr>
            <td>templates</td>
            <td>zero or more template definitions</td>
            <td>templates</td>
            <td>no</td>
        </tr>
        <tr>
            <td>analyses</td>
            <td>zero or more app (a.k.a. analysis) definitions</td>
            <td>analyses</td>
            <td>no</td>
        </tr>
    </tbody>
</table>

A fourth field, `notification_sets`, is accepted by the import service, but will
not be documented here. Notification sets are no longer supported in the DE, but
support for them has not been removed from the import service yet.

While it is possible to import templates using the services that accept this
JSON format, there are two important differences to note. First apps are not
automatically generated for templates that are imported using these services.
Instead, apps must be specified explicitly using the `analyses` field. Second,
these services allow elements to refer to other elements defined in the same
service call by name. For example, a template might refer to a deployed
component defined in the same JSON document by its name rather than by its
identifier. This allows the automatic generation of identifiers without
requiring multiple separate service calls.

### App JSON - Deployed Components

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
            <td>the deployed component identifier</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the name of the file that is used to run the tool</td>
            <td>name</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>location</td>
            <td>the path to the directory containing the executable</td>
            <td>location</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the deployed component type name</td>
            <td>type</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>description</td>
            <td>a brief description of the deployed component</td>
            <td>description</td>
            <td>no</td>
        </tr>
        <tr>
            <td>version</td>
            <td>the deployed component version</td>
            <td>version</td>
            <td>no</td>
        </tr>
        <tr>
            <td>attribution</td>
            <td>the people or entities who create or maintain the tool</td>
            <td>attribution</td>
            <td>no</td>
        </tr>
        <tr>
            <td>implementation</td>
            <td>
                information about the user who integrated the deployed component
                into the DE
            </td>
            <td>implementation</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

The `type` field has to contain the name of ne of the known tool types. You can
get the list of known tool types using the `/get-workflow-elements/tool-types`
endpoint.

#### App JSON - Deployed Components - Implementation

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
            <td>implementor_email</td>
            <td>the email address of the implementor</td>
            <td>N/A</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>implementor</td>
            <td>the name of the implementor</td>
            <td>N/A</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>test</td>
            <td>test data for the deployed component</td>
            <td>test</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

All three fields are required, and attempts to import or update deployed
components will fail if any field is not provided. The import services attempt
to avoid adding duplicate integration data objects by looking for a matching
entry in the database. If a match is found then another link to the matching
entry is created. Otherwise, a new entry is added.

The test data files are stored in a separate table, so integration data table
entries can be shared between multiple deployed components and even between apps
and deployed components.

#### App JSON - Deployed Components - Test Data

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
            <td>input_files</td>
            <td>the list of paths to test input files in iRODS</td>
            <td>input_files</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>output_files</td>
            <td>the list of paths to expected output files in iRODS</td>
            <td>output_files</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

Both the `input_files` and `output_files` fields are required, and an error will
occur if either is not specified. The list of paths may be empty in both cases,
however.

### App JSON - Templates

The JSON format described here is identical to the JSON format described in the
[Template JSON](#template-json) section except that the top-level JSON object
accepts new field called, `component_ref`, and does not accept the
`implementation` field.

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
            <td>component_ref</td>
            <td>
                the name of a deployed component defined in the same JSON
                document
            </td>
            <td>component_ref</td>
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

The `name` and `id` fields deserve some special attention. The
`/import-template` and `/update-template` endpoints both treat these fields the
same way. If an identifier is provided then the endpoints will attempt to match
based on the identifier. If a template with a matching identifier is found then
the `/import-template` endpoint will refuse to do anything and the
`/update-template` endpoint will update the existing template. If no identifier
is provided then the services attempt to find another template with the same
name. If exactly one template with the same name is found then the
`/import-template` interface will refuse to proceed and the `/update-template`
endpoint will update the existing template. If multiple templates with the same
name are found then neither endpoint will do anything. If no identifier is
specified and no template with the same name is found then both endpoints will
generate a new ID and import the template into the database as a new template.
If an identifier of `auto-gen` is specified then neither endpoint will attempt
to match by name or ID. Instead, a new identifier will be generated and a new
template will be imported into the database.

The `component_ref` and `component` fields are different ways of specifying the
deployed component. If the `component` field is used then the deployed component
must have been imported into the database before the service was called. If the
`component_ref` field is used then the field value should refer to the deployed
component by name and the deployed component must be defined within the same
JSON document. If both the `component_ref` field and the `component` field are
specified then the `component` field takes precedence.

The `label` field contains the display name to use for the template. If the
template label isn't provided then the template name will be used as its display
name. Strictly speaking neither the template name nor label is displayed in the
DE. Instead, the app name or label is displayed. Both import services use the
template name and label as the name and label of the automatically generated
single-step app corresponding to the template, however.

The `groups` field is required, but it's permitted to be an empty JSON array (or
an object containing an empty JSON array).

#### App JSON - Templates - Property Group

Please see [Template JSON - Property Group](#template-json---property-group).

#### App JSON - Templates - Property

Please see [Template JSON - Property](#template-json---property).

#### App JSON - Templates - Data Object

Please see [Template JSON - Data Object](#template-json---data-object).

#### App JSON - Templates - Validator

Please see [Template JSON - Validator](#template-json---validator).

#### App JSON - Templates - Rule

Please see [Template JSON - Rule](#template-json---rule).

### App JSON - Analyses

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
            <td>analysis_id, id</td>
            <td>the app identifier</td>
            <td>analysis_id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>analysis_name</td>
            <td>the app display name</td>
            <td>analysis_name</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the app type</td>
            <td>type</td>
            <td>no</td>
        </tr>
        <tr>
            <td>description</td>
            <td>a textual description of the app</td>
            <td>description</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>steps</td>
            <td>the sequence of steps used to run the app</td>
            <td>steps</td>
            <td>mappings</td>
        </tr>
        <tr>
            <td>implementation</td>
            <td>the implementation details for the analysis</td>
            <td>implementation</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

The app import and update services match existing apps in the same way that the
corresponding template service match templates. If an app ID is specified then
the service will attempt to match based on the app ID. If the app ID is not
specified then the service will attempt to match based on the app name. If no
match is found (whether an ID match or a name match was done) then the service
will create a new app, generating an identifier if necessary. As a special case,
if the app identifier is `auto-gen` then the service will always create a new
app and generate a new identifier for it even if another app with the same name
already exists in the database.

The `type` field is provided as another means of documentation, but it's not
used for anything in the DE.

The `description` field is not required for the import service to be used, but a
description must be provided before an app can be made public.

#### App JSON - Analyses - Steps

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
            <td>id</td>
            <td>the step identifier</td>
            <td>id</td>
            <td>no</td>
        </tr>
        <tr>
            <td>description</td>
            <td>a brief description of the step</td>
            <td>description</td>
            <td>no</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the transformation step name</td>
            <td>name</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>app_type</td>
            <td>The type of app; may be `DE` or `External`</td>
            <td>app_type</td>
            <td>no; defaults to `DE` if unspecified</td>
        </tr>
        <tr>
            <td>template_id</td>
            <td>the identifier of the template used to perform the step</td>
            <td>template_id</td>
            <td>
                required for external apps; otherwise, either template_id or template_ref must be
                specified
            </td>
        </tr>
        <tr>
            <td>template_ref</td>
            <td>the name of the template used to perform the step</td>
            <td>template_ref</td>
            <td>
                can't be used for external apps; either template_id or template_ref must be
                specified
            </td>
        </tr>
        <tr>
            <td>config</td>
            <td>a list of default property value settings</td>
            <td>config</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

As usual, the `id` field is optional and a new identifier will be generated if
one is not supplied.

The `name` field is required because it's used to distinguish properties in case
two properties in different steps have the same identifier. This name is not
displayed, so it would be perfectly reasonable to put an ID in this field or use
general names such as `step1`.

For DE apps, the `template_id` and `template_ref` fields represent two different
means of indicating which template should be used for the step. If the
`template_id` field is used then the template ID must be known in advance,
either by specifying the template ID when it is being imported or by importing
the template first then looking up the identifier. If the `template_ref` field
is used then the field value refers to the template by name and the template
must be defined within the same JSON document as the app.  For external apps,
the `template_id` field must be used.

The `config` field contains a set of automatically assigned property values to
use in the step. These are the values that will be used every time the app is
executed, and users will not be prompted for or shown the values. The
configuration itself is JSON object that maps property IDs to property values.
here's an example configuration:

```json
{
    "91A738F2-EF76-48DF-A386-7FC6BCB4A2BF": "foo",
    "11CBBCCB-2D56-4636-AB3C-807C132373B9": "bar"
}
```

#### App JSON - Analyses - Mappings

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
            <td>source_step</td>
            <td>the name of the step that produces the output files</td>
            <td>source_step</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>target_step</td>
            <td>the name of the step that consumes the input files</td>
            <td>target_step</td>
            <td>yes</td>
        </tr>
        <tr>
            <td>map</td>
            <td>maps output file IDs to input file IDS</td>
            <td>map</td>
            <td>yes</td>
        </tr>
    </tbody>
</table>

The `source_step` field refers to the name of the step that produces the output
files that are later consumed by the step referenced by the `target_step` field.
Both steps must be defined in the same app, and the source step must come before
the target step in the app definition.

The `map` field contains a JSON object that maps output identifiers from the
source step to input identifiers from the target step. Here's an example of a
complete mapping:

```json
{
    "source_step": "step1",
    "target_step": "step2",
    "map": {
        "00B9894F-70BC-47CF-8C0C-D45A30290F88": "C3196109-73A9-45B0-AEB6-BFC6759E768C",
        "01C5A5E4-8AF8-454E-A8B8-D68009F6C0FB": "4FAB6049-AEDF-4664-B081-E25048613605"
    }
}
```

#### App JSON - Analyses - Implementation

Please see [Template JSON - Implementation](template-json---implementation).

## App JSON Example

Please see the [examples file](examples.md#app-json).

## App JSON for UI

The app JSON required by the UI closely resembles the template JSON required by
the template import services. The property groups in each app are collapsed into
what appears to be a single app and the property identifiers are amended so that
the job submission services can efficiently relate them to the correct
properties.

<table>
    <thead>
        <tr>
            <th>Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>id</td>
            <td>the app identifier</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the app name</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the app name</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the app type</td>
        </tr>
        <tr>
            <td>groups</td>
            <td>the list of property groups</td>
        </tr>
    </tbody>
</table>

### App JSON for UI - Property Group

<table>
    <thead>
        <tr>
            <th>Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>id</td>
            <td>the property group ID</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the property group name</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the property group label</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the property group type</td>
        </tr>
        <tr>
            <td>properties</td>
            <td>the list of properties in the property group</td>
        </tr>
    </tbody>
</table>

If the app is a multi-step app then the `name` and `label` fields are prefixed
by the template name and a hyphen in order to disambiguate similarly named or
labeled property groups in different steps. For single-step apps these fields
are left unmodified.

### App JSON for UI - Property

<table>
    <thead>
        <tr>
            <th>Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>id</td>
            <td>the property ID</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the property name</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the property label</td>
        </tr>
        <tr>
            <td>isVisible</td>
            <td>visibility flag</td>
        </tr>
        <tr>
            <td>value</td>
            <td>the default value for the property</td>
        </tr>
        <tr>
            <td>type</td>
            <td>the property type name</td>
        </tr>
        <tr>
            <td>description</td>
            <td>a brief description of the property.</td>
        </tr>
        <tr>
            <td>validator</td>
            <td>property value validation instructions</td>
        </tr>
    </tbody>
</table>

The way in which properties are formatted varies depending on whether the
property is an output property, an input property, or any other type of
property.

In all cases, the `id` field is prefixed by the step name and an underscore in
order to ensure that property values can be mapped to properties using only the
identifier given to the UI and the property value itself.

Also note that properties are only formatted in the JSON that is sent to the UI
if their values cannot be determined without prompting the user. This varies for
different types of properties. For any property that is neither an output nor an
input property, the property is only formatted if it is marked as visible and
its value isn't specified in the transformation (the `config` section from the
import JSON). An input or output property is formatted only if it is marked as
visible, its value is not specified in the transformation, it does not appear in
an input/output mapping, and its name is not implicitly defined by the deployed
component itself.

Also note that the validators for input and output properties are handled a
little bit differently than those for other types of properties. For input and
output properties that consume or generate files, the formatter looks at the
data object associated with the property. If the data object indicates that the
input or output is required then the formatter generates a validator with no
rules and the `required` flag set to true. For reference genome type properties,
the formatter generates a validator with the `required` flag set to true and a
single `MustContain` rule defined to allow users to select the appropriate
reference genome from a drop-down list.

### App JSON for UI - Validator

<table>
    <thead>
        <tr>
            <th>Name</td>
            <th>Description</td>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>id</td>
            <td>the validator identifier</td>
        </tr>
        <tr>
            <td>label</td>
            <td>the validator label</td>
        </tr>
        <tr>
            <td>name</td>
            <td>the validator name</td>
        </tr>
        <tr>
            <td>required</td>
            <td>a flag indicating if user input is required</td>
        </tr>
        <tr>
            <td>rules</td>
            <td>the list of validation rules</td>
        </tr>
    </tbody>
</table>

### App JSON for UI - Rule

The rule JSON is formatted in the same way that the export service formats it.
For more information, please see [Template JSON - Rule](#template-json---rule)
above.

## App JSON for UI Example

Please see the [examples file](examples.md#app-json-for-ui).

# App Metadata Administration Services

## Exporting an Analysis

*Unsecured Endpoint:* GET /export-workflow/{analysis-id}

This service exports an analysis in the format used to import multi-step
analyses into the DE. Note that this format will work for both single- and
multi-step analyses. This service is used by the export script to export
analyses from the DE. The response body for this service is fairly large, so it
will not be documented here. Please see [Template JSON](#template-json) above
for more information.

Here's an example:

```
$ curl -s http://by-tor:8888/export-workflow/2976DE6C-03E3-4109-AECD-3D9CAEDD3122 | python -mjson.tool
{
    "analyses": [
        {
            "analysis_id": "2976DE6C-03E3-4109-AECD-3D9CAEDD3122",
            "analysis_name": "Find Unique Values",
            "deleted": false,
            "description": "GNU uniq: Discard all but one of successive identical lines from a sorted INPUT file, writing to an OUTPUT file",
            "implementation": {
                "implementor": "Nobody",
                "implementor_email": "nobody@iplantcollaborative.org"
            },
            "ratings": [
                {
                    "rating": 4,
                    "username": "nobody@iplantcollaborative.org"
                }
            ],
            "references": [],
            "steps": [
                {
                    "config": {},
                    "description": "GNU uniq: Discard all but one of successive identical lines from INPUT, writing to OUTPUT",
                    "id": "s8384791fa91a45be89aef0b5faf3f7ac",
                    "name": "Find or count unique values",
                    "template_ref": "Find or count unique values"
                }
            ],
            "suggested_groups": [],
            "type": "",
            "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Find+Unique+Values"
        }
    ],
    "components": [
        {
            "attribution": "nobody",
            "description": "Scan for unique values",
            "id": "c4e6f548cc0ee431da7f2ddfdf3ace761",
            "implementation": {
                "implementor": "No name",
                "implementor_email": "noreply@iplantcollaborative.org",
                "test": {
                    "input_files": [],
                    "output_files": []
                }
            },
            "location": "/usr/bin/",
            "name": "uniq",
            "type": "executable",
            "version": "1.00"
        }
    ],
    "templates": [
        {
            "component_ref": "uniq",
            "description": "",
            "edited_date": "",
            "groups": {
                "description": "",
                "groups": [
                    {
                        "description": "",
                        "id": "8724F26F-256B-B627-8DD9-08163C4B2B85",
                        "isVisible": true,
                        "label": "Inputs",
                        "name": "",
                        "properties": [
                            {
                                "data_object": {
                                    "cmdSwitch": "",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "PlainText",
                                    "file_info_type_id": "6270AB49-D6B6-4D8C-B15A-89657B4227A4",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "6086ADB0-513A-1FCA-463A-45BDC92893FD",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "name": "Select an input file",
                                    "order": 7,
                                    "required": false,
                                    "retain": false
                                },
                                "description": "",
                                "id": "6086ADB0-513A-1FCA-463A-45BDC92893FD",
                                "isVisible": true,
                                "label": "Select an input file",
                                "name": "",
                                "omit_if_blank": true,
                                "order": 7,
                                "type": "Input",
                                "validator": {
                                    "id": "v830a719b4b0545b3944d44336e26104f",
                                    "name": "",
                                    "required": true,
                                    "rules": []
                                },
                                "value": ""
                            }
                        ],
                        "type": ""
                    },
                    {
                        "description": "",
                        "id": "CC56EB41-FD18-26E6-E5A0-BE44A426303E",
                        "isVisible": true,
                        "label": "Options",
                        "name": "",
                        "properties": [
                            {
                                "description": "",
                                "id": "BFBA9726-6676-D16A-6D8D-EDE11C49DC3A",
                                "isVisible": true,
                                "label": "Ignore case while doing comparisons",
                                "name": "--ignore-case",
                                "omit_if_blank": false,
                                "order": 2,
                                "type": "Flag",
                                "value": "false"
                            },
                            {
                                "description": "",
                                "id": "A4549AA8-0F0E-5B62-4097-A38B1542FBD0",
                                "isVisible": true,
                                "label": "Prefix unique lines by the number of occurrences",
                                "name": "--count",
                                "omit_if_blank": false,
                                "order": 1,
                                "type": "Flag",
                                "value": "false"
                            },
                            {
                                "description": "",
                                "id": "2D50BC00-D747-C0A8-64CA-189EF0BA5AB4",
                                "isVisible": true,
                                "label": "Only print duplicate lines",
                                "name": "--repeated",
                                "omit_if_blank": false,
                                "order": 3,
                                "type": "Flag",
                                "value": "false"
                            },
                            {
                                "description": "",
                                "id": "3CAF0548-5E41-90A9-F161-723CE5740C9C",
                                "isVisible": true,
                                "label": "Only print unique lines",
                                "name": "--unique",
                                "omit_if_blank": false,
                                "order": 6,
                                "type": "Flag",
                                "value": "false"
                            },
                            {
                                "description": "A field is a run of whitespace, then non-whitespace characters.",
                                "id": "4F650FA8-E03C-38DB-EC9D-A8239CE6EDED",
                                "isVisible": true,
                                "label": "Avoid comparing the first N fields",
                                "name": "--skip-fields",
                                "omit_if_blank": true,
                                "order": 5,
                                "type": "Double",
                                "value": ""
                            },
                            {
                                "description": "Fields are skipped before chars.",
                                "id": "E261EDE5-A2B2-B929-204A-60A8E2AA02AC",
                                "isVisible": true,
                                "label": "Avoid comparing the first N characters",
                                "name": "--skip-chars",
                                "omit_if_blank": true,
                                "order": 4,
                                "type": "Double",
                                "value": ""
                            },
                            {
                                "data_object": {
                                    "cmdSwitch": "",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "PlainText",
                                    "file_info_type_id": "6270AB49-D6B6-4D8C-B15A-89657B4227A4",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "6EDC0122-650B-3C89-AAF4-1F8CA424396D",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "order": 8,
                                    "output_filename": "uniq_output.txt",
                                    "required": true,
                                    "retain": true
                                },
                                "description": "",
                                "id": "6EDC0122-650B-3C89-AAF4-1F8CA424396D",
                                "isVisible": false,
                                "label": "uniq_output.txt",
                                "name": "",
                                "omit_if_blank": true,
                                "order": 8,
                                "type": "Output",
                                "value": ""
                            }
                        ],
                        "type": ""
                    }
                ],
                "id": "--root-PropertyGroupContainer--",
                "isVisible": true,
                "label": "",
                "name": ""
            },
            "id": "2976DE6C-03E3-4109-AECD-3D9CAEDD3122",
            "label": "Find or count unique values",
            "name": "Find or count unique values",
            "published_date": "",
            "tito": "2976DE6C-03E3-4109-AECD-3D9CAEDD3122",
            "type": ""
        }
    ]
}
```
