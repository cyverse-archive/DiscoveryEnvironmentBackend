File Preview
------------

__URL Path__: /secured/filesystem/file/preview

__HTTP Method__: GET

__Error Codes__: ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_FILE, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* path - A path to a file in iRODS.

__Response Body__:

    {
        "preview" : "Copyright (c) 2011, The Arizona Board of Regents on behalf of \nThe University of Arizona\n\nAll rights reserved.\n\nDeveloped by: iPlant Collaborative as a collaboration between\nparticipants at BIO5 at The University of Arizona (the primary hosting\ninstitution), Cold Spring Harbor Laboratory, The University of Texas at\nAustin, and individual contributors. Find out more at \nhttp:\/\/www.iplantcollaborative.org\/.\n\nRedistribution and use in source and binary forms, with or without \nmodification, are permitted provided that the following conditions are\nmet:\n\n * Redistributions of source code must retain the above copyright \n   notice, this list of conditions and the following disclaimer.\n * Redistributions in binary form must reproduce the above copyright \n   notice, this list of conditions and the following disclaimer in the \n   documentation and\/or other materials provided with the distribution.\n * Neither the name of the iPlant Collaborative, BIO5, The University \n   of Arizona, Cold Spring Harbor Laboratory, The University of Texas at \n   Austin, nor the names of other contributors may be used to endorse or \n   promote products derived from this software without specific prior \n   written permission.\n\nTHIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS\nIS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED \nTO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A \nPARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT \nHOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,\nSPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED\nTO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR\nPROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF\nLIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING\nNEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS \nSOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
    }

__Curl Command__:

    curl http://127.0.0.1:3000/file/preview?user=johnw&path=/iplant/home/johnw/LICENSE.txt




