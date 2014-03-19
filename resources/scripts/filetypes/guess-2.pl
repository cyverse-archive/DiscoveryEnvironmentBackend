#!/usr/bin/perl -w
# guess_bioseq_content.pl is a script that utilizes parts of Bioperl 1.6
# Nirav Merchant nirav@email.arizona.edu 05/03/2013
# Ver 0.2b
# This simple utility based on Bioperl GuessSeqFormat
# http://doc.bioperl.org/releases/bioperl-1.6.1/
# It takes three arguments -file a file to read content from note this file is limited to max first 1000 bytes
# unless you want to read more by passing -limit in bytes
# -size in bytes of the original file from which content was obtained and based on the guessed file format if it
# should be indexed by iPlant content indexing system
# it returns json of the format
# { "ipc-media-type": "fasta" , "ipc-media-sub-type": "" , "ipc-search-index": "", "ipc-search-index-method": "" }
# usage guess_bioseq_content.pl -file /tmp/XXX.tfa -size 1
#

use Getopt::Long qw(:config pass_through);

# use Data::Dumper;
# GuessSeqFormat is from Bioperl package I have commented specifics and just included the
# GuessSeqFormat.pm file
# use GuessSeqFormat;

my $file;
my $content;
my $size;
my $limit = 1000;

GetOptions( 'file=s' => \$file, 'limit=i' => \$limit, 'size=i' => \$size );

# my $guesser = new Bio::Tools::GuessSeqFormat( -text =>$content);
if ( defined $file ) {
    open( FH, $file ) or die "Cannot read $file for detecting content type";
    my $read_bytes = read FH, $content, $limit;
}
else { die "Cannot operate without -file option"; }

my ( $format, $sub_format ) = guess( $content, $size );

# print "Found $format\n";
# if we did not get format back we will not mess with subformat and print empty stuff
if ( !defined($format) ) {
    $format     = "";
    $sub_format = "";
}

# If that format does not have a sub format we will go with empty
if ( defined($format) && !defined($sub_format) ) {
    $sub_format = "";
}

print '{ "ipc-media-type": "'
    . $format
    . '" , "ipc-media-sub-type": "'
    . $sub_format
    . '" , "ipc-search-index": "", "ipc-search-index-method": "" }';

=head2 guess_bioseq_content.pl

 Title      : guess_bioseq_content.pl
 Version	: 0.1b
 Usage      : guess_bioseq_content.pl-file /tmp/XXX.tfa [-size -limit]
 Function   : Guesses the format of the data accociated with the
              file
 Returns    : { "ipc-media-type": "fasta" , "ipc-media-sub-type": "" , "ipc-search-index": "", "ipc-search-index-method": "" }
 Arguments  : -file [-size , -limit]

    This utility guesses popular bio formats associated with sequence analysis
    It can be called with optional arguments of -size which is the orginal file
    size content and it will return if it should be indexed for searching. -limit
    is for number of bytes to read, deafult is 10000
    utility is based on Bioperl GuessSeqFormat http://doc.bioperl.org/releases/bioperl-1.6.1/

=cut

sub guess {
    my $content = shift;
    my $size    = shift;

    my %formats = (
        tcsh      => { test => \&_possibly_tcsh },
        csh       => { test => \&_possibly_csh },
        sh        => { test => \&_possibly_sh },
        bash      => { test => \&_possibly_bash },
        perl      => { test => \&_possibly_perl },
        python    => { test => \&_possibly_python },
        ace       => { test => \&_possibly_ace },
        blast     => { test => \&_possibly_blast },
        bowtie    => { test => \&_possibly_bowtie },
        clustalw  => { test => \&_possibly_clustalw },
        codata    => { test => \&_possibly_codata },
        csv       => { test => \&_possibly_csv },
        embl      => { test => \&_possibly_embl },
        fasta     => { test => \&_possibly_fasta },
        fastq     => { test => \&_possibly_fastq },
        fastxy    => { test => \&_possibly_fastxy },
        game      => { test => \&_possibly_game },
        gcg       => { test => \&_possibly_gcg },
        gcgblast  => { test => \&_possibly_gcgblast },
        gcgfasta  => { test => \&_possibly_gcgfasta },
        gde       => { test => \&_possibly_gde },
        genbank   => { test => \&_possibly_genbank },
        genscan   => { test => \&_possibly_genscan },
        gff       => { test => \&_possibly_gff },
        hmmer     => { test => \&_possibly_hmmer },
        nexus     => { test => \&_possibly_nexus },
        mase      => { test => \&_possibly_mase },
        mega      => { test => \&_possibly_mega },
        msf       => { test => \&_possibly_msf },
        phrap     => { test => \&_possibly_phrap },
        pir       => { test => \&_possibly_pir },
        pfam      => { test => \&_possibly_pfam },
        phylip    => { test => \&_possibly_phylip },
        prodom    => { test => \&_possibly_prodom },
        raw       => { test => \&_possibly_raw },
        rsf       => { test => \&_possibly_rsf },
        selex     => { test => \&_possibly_selex },
        stockholm => { test => \&_possibly_stockholm },
        swiss     => { test => \&_possibly_swiss },
        tab       => { test => \&_possibly_tab },
        vcf       => { test => \&_possibly_vcf }
    );
    foreach my $fmt_key ( keys %formats ) {
        $formats{$fmt_key}{fmt_string}     = $fmt_key;
        $formats{$fmt_key}{sub_fmt_string} = $fmt_key;
        $formats{$fmt_key}{found}          = undef;
    }

    my ( $fh, %found );
    my $start_pos;
    my @lines;

    # Break the text into separate lines.
    @lines = split /\n/, $content;

    # print $content;

    my $done   = 0;
    my $lineno = 0;
    my $fmt_string;
    while ( !$done ) {
        my $line;    # The next line of the file.
        my $match = 0;    # Number of possible formats of this line.

        if ( defined $content ) {
            last if ( scalar @lines == 0 );
            $line = shift @lines;
        }
        next if ( $line =~ /^\s*$/ );    # Skip white and empty lines.

        chomp($line);
        $line =~ s/\r$//;                # Fix for DOS files on Unix.
        ++$lineno;

        # my $val,$ver;
        # my $match;

        while ( ( $fmt_key, $fmt ) = each(%formats) ) {

            ( $fmt_string, $sub_fmt_string ) = $fmt->{test}( $line, $lineno );

            if ($fmt_string) {
                ++$match;

                #            print "Match:$match\n";
                $found{$fmt_key} = $sub_fmt_string;

                #           print "Val = $fmt_string\tVer=$sub_fmt_string\n";
                #                $formats{$fmt_key}{fmt_string} = $fmt_string;
                $formats{$fmt_key}{fmt_string}     = $fmt;
                $formats{$fmt_key}{sub_fmt_string} = $sub_fmt_string;
                $formats{$fmt_key}{found}          = 1;

            }
        }

        # We're done if there was only one match.
        $done = ( $match == 1 );

        # print "second->Val = $fmt_string\tVer=$sub_fmt_string\n";
        # print Dumper(%formats);
        # print "== =====\n";
        # print Dumper(%found);

        if ( $match == 1 ) {

            # we only return stuff is there is a single match
            foreach my $val ( keys %found ) {
                return ( $val, $found{$val} );
            }
        }
    }

    # return ($done ? $fmt_string : undef,);
}

=head1 HELPER SUBROUTINES

All helper subroutines will, given a line of text and the line
number of the same line, return 1 if the line possibly is from a
file of the type that they perform a test of.

A zero return value does not mean that the line is not part
of a certain type of file, just that the test did not find any
characteristics of that type of file in the line.

=head2 _possibly_csh

csh shell script

=cut

sub _possibly_csh {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^#.+\/csh/ );
}

=head2 _possibly_tcsh

tcsh shell script

=cut

sub _possibly_tcsh {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^#.+\/tcsh/ );
}

=head2 _possibly_sh

sh shell script

=cut

sub _possibly_sh {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^#.+\/sh/ );
}

=head2 _possibly_bash

bash shell script

=cut

sub _possibly_bash {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^#.+\/bash/ );
}

=head2 _possibly_perl

perl script

=cut

sub _possibly_perl {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^#.+perl/ );
}

=head2 _possibly_python

python script

=cut

sub _possibly_python {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^#.+python/ );
}

=head2 _possibly_ace

From bioperl test data, and from
"http://www.isrec.isb-sib.ch/DEA/module8/B_Stevenson/Practicals/transcriptome_recon/transcriptome_recon.html".

=cut

sub _possibly_ace {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^(?:Sequence|Peptide|DNA|Protein) [":]/ );
}

=head2 _possibly_blast

 From various blast results.

=cut

sub _possibly_blast {
    my ( $line, $lineno ) = ( shift, shift );
    return (   $lineno == 1
            && $line =~ /^[[:upper:]]*BLAST[[:upper:]]*.*\[.*\]$/ );
}

=head2 _possibly_bowtie

Contributed by kortsch.

=cut

sub _possibly_bowtie {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line
            =~ /^[[:graph:]]+\t[-+]\t[[:graph:]]+\t\d+\t([[:alpha:]]+)\t([[:graph:]]+)\t\d+\t[[:graph:]]?/
    ) && length($1) == length($2);
}

=head2 _possibly_clustalw

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_clustalw {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /CLUSTAL/ );
}

=head2 _possibly_codata

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_codata {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /^ENTRY/ )
            || ( $lineno == 2 && $line =~ /^SEQUENCE/ )
            || $line =~ m{^(?:ENTRY|SEQUENCE|///)} );
}

=head2 _possibly_csv

Contributed by Nirav.

=cut

sub _possibly_csv {
    my ( $line, $lineno ) = ( shift, shift );

    # my $version = () = $line =~ /,/g ;
    # print "Comma Count: $count\n";
    return ( $lineno == 1 && $line =~ /^[^,]+,[^,]+/ );
}

=head2 _possibly_embl

From
"http://www.ebi.ac.uk/embl/Documentation/User_manual/usrman.html#3.3".

=cut

sub _possibly_embl {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /^ID   / && $line =~ /BP\.$/ );
}

=head2 _possibly_fasta

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_fasta {
    my ( $line, $lineno ) = ( shift, shift );
    return ( ( $lineno != 1 && $line =~ /^[A-IK-NP-Z]+$/i )
            || $line =~ /^>\s*\w/ );
}

=head2 _possibly_fastq

From bioperl test data.

=cut

sub _possibly_fastq {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /^@/ )
            || ( $lineno == 3 && $line =~ /^\+/ ) );
}

=head2 _possibly_fastxy

From bioperl test data.

=cut

sub _possibly_fastxy {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /^ FAST(?:XY|A)/ )
            || ( $lineno == 2 && $line =~ /^ version \d/ ) );
}

=head2 _possibly_game

From bioperl testdata.

=cut

sub _possibly_game {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^<!DOCTYPE game/ );
}

=head2 _possibly_gcg

From bioperl, Bio::SeqIO::gcg.

=cut

sub _possibly_gcg {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /Length: .*Type: .*Check: .*\.\.$/ );
}

=head2 _possibly_gcgblast

From bioperl testdata.

=cut

sub _possibly_gcgblast {
    my ( $line, $lineno ) = ( shift, shift );
    return (
        ( $lineno == 1 && $line =~ /^!!SEQUENCE_LIST/ ) || ( $lineno == 2
            && $line =~ /^[[:upper:]]*BLAST[[:upper:]]*.*\[.*\]$/ )
    );
}

=head2 _possibly_gcgfasta

From bioperl testdata.

=cut

sub _possibly_gcgfasta {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /^!!SEQUENCE_LIST/ )
            || ( $lineno == 2 && $line =~ /FASTA/ ) );
}

=head2 _possibly_gde

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_gde {
    my ( $line, $lineno ) = ( shift, shift );
    return (
               $line =~ /^[{}]$/
            || $line =~ /^(?:name|longname|sequence-ID|
                          creation-date|direction|strandedness|
                          type|offset|group-ID|creator|descrip|
                          comment|sequence)/x
    );
}

=head2 _possibly_genbank

From "http://www.ebi.ac.uk/help/formats.html".
Format of [apparantly optional] file header from
"http://www.umdnj.edu/rcompweb/PA/Notes/GenbankFF.htm". (TODO: dead link)

=cut

sub _possibly_genbank {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /GENETIC SEQUENCE DATA BANK/ )
            || ( $lineno == 1 && $line =~ /^LOCUS / )
            || ( $lineno == 2 && $line =~ /^DEFINITION / )
            || ( $lineno == 3 && $line =~ /^ACCESSION / ) );
}

=head2 _possibly_genscan

From bioperl test data.

=cut

sub _possibly_genscan {
    my ( $line, $lineno ) = ( shift, shift );
    return (
        ( $lineno == 1 && $line =~ /^GENSCAN.*Date.*Time/ )
            || (
            $line =~ /^(?:Sequence\s+\w+|Parameter matrix|Predicted genes)/ )
    );
}

=head2 _possibly_gff

From bioperl test data.

=cut

sub _possibly_gff {
    my ( $line, $lineno ) = ( shift, shift );
    my $version;
    if ( $lineno == 1 && $line =~ /^##gff-version/ ) {
        my @val = split( /\s+/, $line );
        $version = $val[1];
    }
    if (   ( $lineno == 1 && $line =~ /^##gff-version/ )
        || ( $lineno == 2 && $line =~ /^##date/ ) )
    {

        #  return("gff",$version);
    }

    return (
        ( $lineno == 1 && $line =~ /^##gff-version/ )
            || ( $lineno == 2 && $line =~ /^##date/ ),
        $version
    );
}

=head2 _possibly_hmmer

From bioperl test data.

=cut

sub _possibly_hmmer {
    my ( $line, $lineno ) = ( shift, shift );
    return (
        ( $lineno == 2 && $line =~ /^HMMER/ ) || ( $lineno == 3
            && $line =~ /Washington University School of Medicine/ )
    );
}

=head2 _possibly_nexus

From "http://paup.csit.fsu.edu/nfiles.html".

=cut

sub _possibly_nexus {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /^#NEXUS/ );
}

=head2 _possibly_mase

From bioperl test data.
More detail from "http://www.umdnj.edu/rcompweb/PA/Notes/GenbankFF.htm" (TODO: dead link)

=cut

sub _possibly_mase {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /^;;/ )
            || ( $lineno > 1 && $line =~ /^;[^;]?/ ) );
}

=head2 _possibly_mega

From the ensembl broswer (AlignView data export).

=cut

sub _possibly_mega {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /^#mega$/ );
}

=head2 _possibly_msf

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_msf {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ m{^//} || $line =~ /MSF:.*Type:.*Check:|Name:.*Len:/ );
}

=head2 _possibly_phrap

From "http://biodata.ccgb.umn.edu/docs/contigimage.html". (TODO: dead link)
From "http://genetics.gene.cwru.edu/gene508/Lec6.htm".    (TODO: dead link)
From bioperl test data ("*.ace.1" files).

=cut

sub _possibly_phrap {
    my ( $line, $lineno ) = ( shift, shift );
    return (
        $line =~ /^(?:AS\ |CO\ Contig|BQ|AF\ |BS\ |RD\ |
                          QA\ |DS\ |RT\{)/x
    );
}

=head2 _possibly_pir

From "http://www.ebi.ac.uk/help/formats.html".
The ".,()" spotted in bioperl test data.

=cut

sub _possibly_pir    # "NBRF/PIR" (?)
{
    my ( $line, $lineno ) = ( shift, shift );
    return ( ( $lineno != 1 && $line =~ /^[\sA-IK-NP-Z.,()]+\*?$/i )
            || $line =~ /^>(?:P1|F1|DL|DC|RL|RC|N3|N1);/ );
}

=head2 _possibly_pfam

From bioperl test data.

=cut

sub _possibly_pfam {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ m{^\w+/\d+-\d+\s+[A-IK-NP-Z.]+}i );
}

=head2 _possibly_phylip

From "http://www.ebi.ac.uk/help/formats.html".  Initial space
allowed on first line (spotted in ensembl AlignView exported
data).

=cut

sub _possibly_phylip {
    my ( $line, $lineno ) = ( shift, shift );
    return (
               ( $lineno == 1 && $line =~ /^\s*\d+\s\d+/ )
            || ( $lineno == 2 && $line =~ /^\w\s+[A-IK-NP-Z\s]+/ )
            || ( $lineno == 3
            && $line =~ /(?:^\w\s+[A-IK-NP-Z\s]+|\s+[A-IK-NP-Z\s]+)/ )
    );
}

=head2 _possibly_prodom

From "http://prodom.prabi.fr/prodom/current/documentation/data.php".

=cut

sub _possibly_prodom {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /^ID   / && $line =~ /\d+ seq\.$/ );
}

=head2 _possibly_raw

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_raw {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $line =~ /^[A-Za-z\s]+$/ );
}

=head2 _possibly_rsf

From "http://www.ebi.ac.uk/help/formats.html".

=cut

sub _possibly_rsf {
    my ( $line, $lineno ) = ( shift, shift );
    return (
               ( $lineno == 1 && $line =~ /^!!RICH_SEQUENCE/ )
            || $line =~ /^[{}]$/
            || $line =~ /^(?:name|type|longname|
                          checksum|creation-date|strand|sequence)/x
    );
}

=head2 _possibly_selex

From "http://www.ebc.ee/WWW/hmmer2-html/node27.html".

Assuming presence of Selex file header.  Data exported by
Bioperl on Pfam and Selex formats are identical, but Pfam file
only holds one alignment.

=cut

sub _possibly_selex {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /^#=ID / )
            || ( $lineno == 2 && $line =~ /^#=AC / )
            || ( $line =~ /^#=SQ / ) );
}

=head2 _possibly_stockholm

From bioperl test data.

=cut

sub _possibly_stockholm {
    my ( $line, $lineno ) = ( shift, shift );
    return ( ( $lineno == 1 && $line =~ /^# STOCKHOLM/ )
            || $line =~ /^#=(?:GF|GS) / );
}

=head2 _possibly_swiss

From "http://ca.expasy.org/sprot/userman.html#entrystruc".

=cut

sub _possibly_swiss {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /^ID   / && $line =~ /AA\.$/ );
}

=head2 _possibly_tab

Contributed by Heikki.

=cut

sub _possibly_tab {
    my ( $line, $lineno ) = ( shift, shift );
    return ( $lineno == 1 && $line =~ /^[^\t]+\t[^\t]+/ );
}

=head2 _possibly_vcf

From "http://www.1000genomes.org/wiki/analysis/vcf4.0".

Assumptions made about sanity - format and date lines are line 1 and 2
respectively. This is not specified in the format document.

=cut

sub _possibly_vcf {
    my ( $line, $lineno ) = ( shift, shift );
    return (   ( $lineno == 1 && $line =~ /##fileformat=VCFv/ )
            || ( $lineno == 2 && $line =~ /##fileDate=/ ) );
}
