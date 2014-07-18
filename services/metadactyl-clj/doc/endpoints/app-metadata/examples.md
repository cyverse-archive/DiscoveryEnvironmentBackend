# Table of Contents

* [JSON Examples](#json-examples)
    * [Template JSON](#template-json)
    * [App JSON](#app-json)
    * [App JSON for UI](#app-json-for-ui)
    * [Updated Template JSON](#updated-template-json)

# JSON Examples

## Template JSON

```json
{
    "component": "cat",
    "component_id": "c72c314d1eace461290b9b568d9feb86a",
    "description": "An awesomer cat!",
    "edited_date": "1362102664862",
    "groups": {
        "description": "",
        "groups": [
            {
                "description": "",
                "id": "03CE3E5C-2495-0D26-FBF8-BE3BEC34E200",
                "isVisible": true,
                "label": "Input",
                "name": "",
                "properties": [
                    {
                        "data_object": {
                            "cmdSwitch": "",
                            "data_source": "file",
                            "description": "The files to concatenate together.",
                            "file_info_type": "File",
                            "file_info_type_id": "0900E992-3BBD-4F4B-8D2D-ED289CA4E4F1",
                            "format": "Unspecified",
                            "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                            "id": "6BE6E5C4-9E25-F8A3-BFE4-80AD69CFBF49",
                            "is_implicit": false,
                            "multiplicity": "Many",
                            "name": "Input Files",
                            "order": 1,
                            "required": true,
                            "retain": false
                        },
                        "description": "The files to concatenate together.",
                        "id": "6BE6E5C4-9E25-F8A3-BFE4-80AD69CFBF49",
                        "isVisible": true,
                        "label": "Input Files",
                        "name": "",
                        "omit_if_blank": false,
                        "order": 1,
                        "type": "Input",
                        "validator": {
                            "id": "vffb06deb654749578ef4918bdbec9215",
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
                "id": "9B3DB651-96E7-EA67-87A2-388AA9DC6752",
                "isVisible": true,
                "label": "Options",
                "name": "",
                "properties": [
                    {
                        "description": "Number all output lines.",
                        "id": "D37E9C66-92EA-A132-4DC4-5020BF1D3DBC",
                        "isVisible": true,
                        "label": "Number Output Lines",
                        "name": "-n",
                        "omit_if_blank": false,
                        "order": 2,
                        "type": "Flag",
                        "value": "true"
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
    "id": "F1B92BA7-7081-42B7-9461-197792E4E0F3",
    "implementation": {
        "implementor": "nobody",
        "implementor_email": "nobody@iplantcollaborative.org"
    },
    "label": "Lynx",
    "name": "Lynx",
    "published_date": "1362102664863",
    "references": [
        ""
    ],
    "tito": "F1B92BA7-7081-42B7-9461-197792E4E0F3",
    "type": ""
}
```

## App JSON

```json
{
    "analyses": [
        {
            "analysis_id": "0234CD33-46C2-4DEC-BBC7-FCCA91534DAF",
            "analysis_name": "Muscle-Fasttree",
            "deleted": false,
            "description": "Align and generate tree",
            "implementation": {
                "implementor": "nobody",
                "implementor_email": "nobody@iplantcollaborative.org"
            },
            "mappings": [
                {
                    "map": {
                        "01311435-EFC6-F1DB-9456-106BD956A224": "69D7E36B-AF0C-4A98-A791-DF709B28CF23"
                    },
                    "source_step": "step_1_9B41C9E4-5031-4A49-B1CB-C471335DF16E",
                    "target_step": "step_2_2A6B165E-416C-2EDD-1DF4-036EB7D0684F"
                }
            ],
            "ratings": [],
            "references": [],
            "steps": [
                {
                    "config": {},
                    "description": "Muscle-3.8.31",
                    "id": "F052C71C-FD5A-4DB0-8BC8-A82CF0E5B775",
                    "name": "step_1_9B41C9E4-5031-4A49-B1CB-C471335DF16E",
                    "app_type": "DE",
                    "template_ref": "Muscle-3.8.31"
                },
                {
                    "config": {},
                    "description": "Ninja",
                    "id": "E3259BB6-E403-4253-9F7D-8485C3CCCC09",
                    "name": "step_2_2A6B165E-416C-2EDD-1DF4-036EB7D0684F",
                    "app_type": "DE",
                    "template_ref": "Ninja"
                }
            ],
            "suggested_groups": [],
            "type": "",
            "wiki_url": ""
        }
    ],
    "components": [
        {
            "attribution": "nobody",
            "description": "Muscle",
            "id": "c4859e9b8599248c0af6217332cd7738b",
            "implementation": {
                "implementor": "nobody",
                "implementor_email": "nobody@email.arizona.edu",
                "test": {
                    "input_files": [],
                    "output_files": []
                }
            },
            "location": "/usr/local2",
            "name": "muscle-3.8.31.pl",
            "type": "executable",
            "version": "3.8.31"
        },
        {
            "attribution": "Travis Wheeler",
            "description": "NINJA",
            "id": "3A848CAF-C362-487C-8B34-51B518173FDA",
            "implementation": {
                "implementor": "nobody",
                "implementor_email": "nobody@iplantcollaborative.org",
                "test": {
                    "input_files": [],
                    "output_files": []
                }
            },
            "location": "/usr/local2/bin",
            "name": "ninja",
            "type": "executable",
            "version": ""
        }
    ],
    "templates": [
        {
            "component_ref": "ninja",
            "description": "",
            "edited_date": "",
            "groups": {
                "description": "",
                "groups": [
                    {
                        "description": "",
                        "id": "DAF94841-68B1-4908-BE27-8B4802C0058A",
                        "isVisible": true,
                        "label": "Select input data",
                        "name": "Select data:",
                        "properties": [
                            {
                                "data_object": {
                                    "cmdSwitch": "",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "File",
                                    "file_info_type_id": "0900E992-3BBD-4F4B-8D2D-ED289CA4E4F1",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "69D7E36B-AF0C-4A98-A791-DF709B28CF23",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "name": "Input file",
                                    "order": 3,
                                    "required": true,
                                    "retain": false
                                },
                                "description": "",
                                "id": "69D7E36B-AF0C-4A98-A791-DF709B28CF23",
                                "isVisible": true,
                                "label": "Input file",
                                "name": "",
                                "omit_if_blank": true,
                                "order": 3,
                                "type": "Input",
                                "value": ""
                            }
                        ],
                        "type": "step"
                    },
                    {
                        "description": "",
                        "id": "1AF0BA7E-1384-4516-8E61-7FEB8696F8F7",
                        "isVisible": true,
                        "label": "Options",
                        "name": "",
                        "properties": [
                            {
                                "description": "",
                                "id": "A39BDD75-3D74-4F48-8B50-F6266C1C217F",
                                "isVisible": false,
                                "label": "script",
                                "name": "",
                                "omit_if_blank": true,
                                "order": -86,
                                "type": "Text",
                                "value": "/usr/local2/bin/ninja"
                            },
                            {
                                "description": "",
                                "id": "E31FB0C2-213E-4996-954E-5594B5F3CB46",
                                "isVisible": true,
                                "label": "Input type",
                                "name": "--in,--in_type d",
                                "omit_if_blank": true,
                                "order": 2,
                                "type": "Selection",
                                "validator": {
                                    "id": "D633C634-245F-4536-97A2-7968EDCFA88A",
                                    "name": "",
                                    "required": true,
                                    "rules": [
                                        {
                                            "MustContain": [
                                                {
                                                    "display": "Multiple sequence alignment (fasta)",
                                                    "isDefault": false,
                                                    "name": "--in",
                                                    "value": ""
                                                },
                                                {
                                                    "display": "Pair-wise distance matrix (phylip)",
                                                    "isDefault": false,
                                                    "name": "--in_type",
                                                    "value": "d"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                "value": "0"
                            },
                            {
                                "description": "",
                                "id": "A83E93C5-D6D0-4B1B-A17F-7162F9D6B635",
                                "isVisible": true,
                                "label": "Output file",
                                "name": "--out ",
                                "omit_if_blank": true,
                                "order": 4,
                                "type": "Text",
                                "value": "tree.newick"
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
            "id": "2A6B165E-416C-2EDD-1DF4-036EB7D0684F",
            "label": "Ninja",
            "name": "Ninja",
            "published_date": "",
            "tito": "2A6B165E-416C-2EDD-1DF4-036EB7D0684F",
            "type": ""
        },
        {
            "component_ref": "muscle-3.8.31.pl",
            "description": "",
            "edited_date": "",
            "groups": {
                "description": "",
                "groups": [
                    {
                        "description": "",
                        "id": "804AD475-2946-43A6-9461-D5BA60A055AC",
                        "isVisible": true,
                        "label": "Select input data",
                        "name": "Select data:",
                        "properties": [
                            {
                                "data_object": {
                                    "cmdSwitch": "",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "NucleotideOrPeptideSequence",
                                    "file_info_type_id": "1C59C759-9CD3-4036-B7B4-82E8DA40D0C2",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "513E1726-79E0-4D86-90D2-08FEB47C4139",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "name": "Select Multiple Sequence File (FASTA format)",
                                    "order": 1,
                                    "required": true,
                                    "retain": false
                                },
                                "description": "",
                                "id": "513E1726-79E0-4D86-90D2-08FEB47C4139",
                                "isVisible": true,
                                "label": "Select Multiple Sequence File (FASTA format)",
                                "name": "",
                                "omit_if_blank": false,
                                "order": 1,
                                "type": "Input",
                                "value": ""
                            }
                        ],
                        "type": "step"
                    },
                    {
                        "description": "",
                        "id": "A9867696-B993-4DC0-8B02-75216B89C454",
                        "isVisible": true,
                        "label": "Sequence Type",
                        "name": "",
                        "properties": [
                            {
                                "description": "",
                                "id": "A52CB953-3C2D-C9F5-6FD3-D8D418E8246E",
                                "isVisible": true,
                                "label": "Sequence Type",
                                "name": "",
                                "omit_if_blank": false,
                                "order": 2,
                                "type": "Selection",
                                "validator": {
                                    "id": "23A2884C-84A2-4099-BE75-4C260AD89568",
                                    "name": "",
                                    "required": true,
                                    "rules": [
                                        {
                                            "MustContain": [
                                                {
                                                    "display": "Auto",
                                                    "isDefault": false,
                                                    "name": "Auto",
                                                    "value": ""
                                                },
                                                {
                                                    "display": "Protein",
                                                    "isDefault": false,
                                                    "name": "Protein",
                                                    "value": ""
                                                },
                                                {
                                                    "display": "DNA",
                                                    "isDefault": false,
                                                    "name": "DNA",
                                                    "value": ""
                                                }
                                            ]
                                        }
                                    ]
                                },
                                "value": "0"
                            }
                        ],
                        "type": ""
                    },
                    {
                        "description": "",
                        "id": "FE1B3812-C358-738F-7EF5-EEFD15D80239",
                        "isVisible": true,
                        "label": "Outputs",
                        "name": "",
                        "properties": [
                            {
                                "data_object": {
                                    "cmdSwitch": "-phyiout",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "MultipleSequenceAlignment",
                                    "file_info_type_id": "F65A8F23-3E46-4DF4-80F9-387641C013A6",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "A3F83803-DFE8-6672-171A-6F33F74749EB",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "order": 5,
                                    "output_filename": "phylip_interleaved.aln",
                                    "required": true,
                                    "retain": true
                                },
                                "description": "",
                                "id": "A3F83803-DFE8-6672-171A-6F33F74749EB",
                                "isVisible": false,
                                "label": "phylip_interleaved.aln",
                                "name": "-phyiout",
                                "omit_if_blank": true,
                                "order": 5,
                                "type": "Output",
                                "value": ""
                            },
                            {
                                "data_object": {
                                    "cmdSwitch": "-clwout",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "MultipleSequenceAlignment",
                                    "file_info_type_id": "F65A8F23-3E46-4DF4-80F9-387641C013A6",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "1E6AF9D2-56D0-B7E8-C558-0762168C9F32",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "order": 3,
                                    "output_filename": "clustalw.aln",
                                    "required": true,
                                    "retain": true
                                },
                                "description": "",
                                "id": "1E6AF9D2-56D0-B7E8-C558-0762168C9F32",
                                "isVisible": false,
                                "label": "clustalw.aln",
                                "name": "-clwout",
                                "omit_if_blank": true,
                                "order": 3,
                                "type": "Output",
                                "value": ""
                            },
                            {
                                "data_object": {
                                    "cmdSwitch": "-fastaout",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "MultipleSequenceAlignment",
                                    "file_info_type_id": "F65A8F23-3E46-4DF4-80F9-387641C013A6",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "01311435-EFC6-F1DB-9456-106BD956A224",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "order": 4,
                                    "output_filename": "fasta.aln",
                                    "required": true,
                                    "retain": true
                                },
                                "description": "",
                                "id": "01311435-EFC6-F1DB-9456-106BD956A224",
                                "isVisible": false,
                                "label": "fasta.aln",
                                "name": "-fastaout",
                                "omit_if_blank": true,
                                "order": 4,
                                "type": "Output",
                                "value": ""
                            },
                            {
                                "data_object": {
                                    "cmdSwitch": "-physout",
                                    "data_source": "file",
                                    "description": "",
                                    "file_info_type": "MultipleSequenceAlignment",
                                    "file_info_type_id": "F65A8F23-3E46-4DF4-80F9-387641C013A6",
                                    "format": "Unspecified",
                                    "format_id": "E806880B-383D-4AD6-A4AB-8CDD88810A33",
                                    "id": "3E6910DA-B6D8-C61E-5E6D-D5B7E785C778",
                                    "is_implicit": false,
                                    "multiplicity": "One",
                                    "order": 6,
                                    "output_filename": "phylip_sequential.aln",
                                    "required": true,
                                    "retain": true
                                },
                                "description": "",
                                "id": "3E6910DA-B6D8-C61E-5E6D-D5B7E785C778",
                                "isVisible": false,
                                "label": "phylip_sequential.aln",
                                "name": "-physout",
                                "omit_if_blank": true,
                                "order": 6,
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
            "id": "9B41C9E4-5031-4A49-B1CB-C471335DF16E",
            "label": "Muscle-3.8.31",
            "name": "Muscle-3.8.31",
            "published_date": "",
            "tito": "9B41C9E4-5031-4A49-B1CB-C471335DF16E",
            "type": ""
        }
    ]
}
```

## App JSON for UI

```json
{
    "analyses": [
        {
            "groups": [
                {
                    "id": "804AD475-2946-43A6-9461-D5BA60A055AC",
                    "label": "Muscle-3.8.31 - Select input data",
                    "name": "Muscle-3.8.31 - Select data:",
                    "properties": [
                        {
                            "description": "",
                            "id": "step_1_9B41C9E4-5031-4A49-B1CB-C471335DF16E_513E1726-79E0-4D86-90D2-08FEB47C4139",
                            "isVisible": true,
                            "label": "Select Multiple Sequence File (FASTA format)",
                            "name": "",
                            "type": "FileInput",
                            "validator": {
                                "label": "",
                                "name": "",
                                "required": true
                            }
                        }
                    ],
                    "type": "step"
                },
                {
                    "id": "A9867696-B993-4DC0-8B02-75216B89C454",
                    "label": "Muscle-3.8.31 - Sequence Type",
                    "name": "Muscle-3.8.31 - ",
                    "properties": [
                        {
                            "description": "",
                            "id": "step_1_9B41C9E4-5031-4A49-B1CB-C471335DF16E_A52CB953-3C2D-C9F5-6FD3-D8D418E8246E",
                            "isVisible": true,
                            "label": "Sequence Type",
                            "name": "",
                            "type": "Selection",
                            "validator": {
                                "id": "23A2884C-84A2-4099-BE75-4C260AD89568",
                                "label": "",
                                "name": "",
                                "required": true,
                                "rules": [
                                    {
                                        "MustContain": [
                                            {
                                                "display": "Auto",
                                                "isDefault": false,
                                                "name": "Auto",
                                                "value": ""
                                            },
                                            {
                                                "display": "Protein",
                                                "isDefault": false,
                                                "name": "Protein",
                                                "value": ""
                                            },
                                            {
                                                "display": "DNA",
                                                "isDefault": false,
                                                "name": "DNA",
                                                "value": ""
                                            }
                                        ]
                                    }
                                ]
                            },
                            "value": "0"
                        }
                    ],
                    "type": ""
                },
                {
                    "id": "1AF0BA7E-1384-4516-8E61-7FEB8696F8F7",
                    "label": "Ninja - Options",
                    "name": "Ninja - ",
                    "properties": [
                        {
                            "description": "",
                            "id": "step_2_2A6B165E-416C-2EDD-1DF4-036EB7D0684F_E31FB0C2-213E-4996-954E-5594B5F3CB46",
                            "isVisible": true,
                            "label": "Input type",
                            "name": "",
                            "type": "Selection",
                            "validator": {
                                "id": "D633C634-245F-4536-97A2-7968EDCFA88A",
                                "label": "",
                                "name": "",
                                "required": true,
                                "rules": [
                                    {
                                        "MustContain": [
                                            {
                                                "display": "Multiple sequence alignment (fasta)",
                                                "isDefault": false,
                                                "name": "--in",
                                                "value": ""
                                            },
                                            {
                                                "display": "Pair-wise distance matrix (phylip)",
                                                "isDefault": false,
                                                "name": "--in_type",
                                                "value": "d"
                                            }
                                        ]
                                    }
                                ]
                            },
                            "value": "0"
                        },
                        {
                            "description": "",
                            "id": "step_2_2A6B165E-416C-2EDD-1DF4-036EB7D0684F_A83E93C5-D6D0-4B1B-A17F-7162F9D6B635",
                            "isVisible": true,
                            "label": "Output file",
                            "name": "--out ",
                            "type": "Text",
                            "value": "tree.newick"
                        }
                    ],
                    "type": ""
                }
            ],
            "id": "0234CD33-46C2-4DEC-BBC7-FCCA91534DAF",
            "label": "Muscle-Fasttree",
            "name": "Muscle-Fasttree",
            "type": ""
        }
    ]
}
```

## Updated Template JSON

```json
{
    "objects": [
        {
            "component": "tar",
            "component_id": "ca6eaa4e7a8cb40ad9d0fbfe8a37bca44",
            "description": "",
            "edited_date": "1323731379419",
            "groups": [
                {
                    "description": "",
                    "id": "3CB157DD-E8EB-457C-95DE-07FB0065769F",
                    "isVisible": true,
                    "label": "Filename",
                    "name": "",
                    "properties": [
                        {
                            "arguments": [],
                            "data_object": {
                                "data_source": "file",
                                "file_info_type": "File",
                                "format": "Unspecified",
                                "is_implicit": false,
                                "retain": false
                            },
                            "defaultValue": "",
                            "description": "",
                            "id": "233F78BC-8E8B-4F8D-AF6B-4EE8651D3803",
                            "isVisible": true,
                            "label": "file",
                            "name": "xzf ",
                            "omit_if_blank": false,
                            "order": 1,
                            "required": true,
                            "type": "FileInput",
                            "validators": []
                        }
                    ],
                    "type": ""
                }
            ],
            "id": "F63C8AAB-5A06-48AD-A8F5-8E904FAA079A",
            "label": "zip asplode",
            "name": "zip asplode",
            "published_date": "",
            "references": [],
            "tito": "F63C8AAB-5A06-48AD-A8F5-8E904FAA079A",
            "type": ""
        }
    ]
}
```
