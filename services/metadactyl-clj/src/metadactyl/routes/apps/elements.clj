(ns metadactyl.routes.apps.elements
  (:use [common-swagger-api.schema]
        [metadactyl.metadata.element-listings :only [list-elements]]
        [metadactyl.routes.domain.app.element]
        [metadactyl.routes.domain.tool :only [ToolListing]]
        [metadactyl.routes.params]
        [ring.util.http-response :only [ok]])
  (:require [metadactyl.util.service :as service]
            [compojure.route :as route]))

(defroutes* app-elements
  (GET* "/" []
        :query [params SecuredIncludeHiddenParams]
        :summary "List All Available App Elements"
        :description "This endpoint may be used to obtain lists of all available elements that may be
        included in an App."
        (ok (list-elements "all" params)))

  (GET* "/data-sources" []
        :query [params SecuredQueryParams]
        :return DataSourceListing
        :summary "List App File Parameter Data Sources"
        :description "Data sources are the known possible sources for file parameters. In most cases, file
        parameters will come from a plain file. The only other options that are currently available
        are redirected standard output and redirected standard error output. Both of these options
        apply only to file parameters that are associated with an output."
        (ok (list-elements "data-sources" params)))

  (GET* "/file-formats" []
        :query [params SecuredQueryParams]
        :return FileFormatListing
        :summary "List App Parameter File Formats"
        :description "The known file formats can be used to describe supported input or output formats for
        a tool. For example, tools in the FASTX toolkit may support FASTA files, several different
        varieties of FASTQ files and Barcode files, among others."
        (ok (list-elements "file-formats" params)))

  (GET* "/info-types" []
        :query [params SecuredQueryParams]
        :return InfoTypeListing
        :summary "List Tool Info Types"
        :description "The known information types can be used to describe the type of information consumed
        or produced by a tool. This is distinct from the data format because some data formats may
        contain multiple types of information and some types of information can be described using
        multiple data formats. For example, the Nexus format can contain multiple types of
        information, including phylogenetic trees. And phylogenetic trees can also be represented in
        PhyloXML format, and a large number of other formats. The file format and information type
        together identify the type of input consumed by a tool or the type of output produced by a
        tool."
        (ok (list-elements "info-types" params)))

  (GET* "/parameter-types" []
        :query [params AppParameterTypeParams]
        :return ParameterTypeListing
        :summary "List App Parameter Types"
        :description "Parameter types represent the types of information that can be passed to a tool. For
        command-line tools, a parameter generally represents a command-line option and the parameter
        type represents the type of data required by the command-line option. For example a
        `Boolean` parameter generally corresponds to a single command-line flag that takes no
        arguments. A `Text` parameter, on the other hand, generally represents some sort of textual
        information. Some parameter types are not supported by all tool types, so it is helpful in
        some cases to filter parameter types either by the tool type or optionally by the tool
        (which is used to determine the tool type). If you filter by both tool type and tool ID then
        the tool type will take precedence. Including either an undefined tool type or an undefined
        tool type name will result in an error"
        (ok (list-elements "parameter-types" params)))

  (GET* "/rule-types" []
        :query [params SecuredQueryParams]
        :return RuleTypeListing
        :summary "List App Parameter Rule Types"
        :description "Rule types represent types of validation rules that may be defined to validate user
        input. For example, if a parameter value must be an integer between 1 and 10 then the
        `IntRange` rule type may be used. Similarly, if a parameter value must contain data in a
        specific format, such as a phone number, then the `Regex` rule type may be used."
        (ok (list-elements "rule-types" params)))

  (GET* "/tools" []
        :query [params SecuredIncludeHiddenParams]
        :return ToolListing
        :summary "List App Tools"
        :description "This endpoint is used by the Discovery Environment to obtain a list of registered
        tools (usually, command-line tools) that can be executed from within the DE."
        (ok (list-elements "tools" params)))

  (GET* "/tool-types" []
        :query [params SecuredQueryParams]
        :return ToolTypeListing
        :summary "List App Tool Types"
        :description "Tool types are known types of tools in the Discovery Environment. Generally, there's
        a different tool type for each execution environment that is supported by the DE."
        (ok (list-elements "tool-types" params)))

  (GET* "/value-types" []
        :query [params SecuredQueryParams]
        :return ValueTypeListing
        :summary "List App Parameter and Rule Value Types"
        :description "If you look closely at the response schema for parameter types and rule types
        listings then you'll notice that each parameter type has a single value type assocaited with
        it and each rule type has one or more value types associated with it. The purpose of value
        types is specifically to link parameter types and rule types. The App Editor uses the value
        type to determine which types of rules can be applied to a parameter that is being defined
        by the user."
        (ok (list-elements "value-types" params)))

  (route/not-found (service/unrecognized-path-response)))
