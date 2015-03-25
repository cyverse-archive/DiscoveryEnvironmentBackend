#!/usr/bin/env perl

use 5.008000;

use warnings;
use strict;

use Carp;
use English qw(-no_match_vars);
use File::Basename;
use Getopt::Long;

# The path to curl.
use constant CURL => '/usr/bin/curl';

# Variables for command-line options.
my $filename;

# Get the command-line options.
Getopt::Long::Configure('bundling_override');
my $opts_ok = GetOptions( 'o|output=s' => \$filename );
if ( !$opts_ok || !defined $filename || scalar @ARGV != 1 ) {
    spew_usage();
    exit 1;
}

# Extract the positional parameters.
my ($url) = @ARGV;

# Fetch the file and obtain the HTTP status code.
my $code = fetch_file( $filename, $url );
if ( $code < 200 || $code > 299 ) {
    croak "URL upload failed with HTTP status $code\n";
}
if ( !-e $filename ) {
    croak "URL upload returned HTTP status $code, but no file was saved";
}

exit;

##########################################################################
# Usage      : $code = fetch_file( $filename, $url );
#
# Purpose    : Fetches a file from a remote server using curl and not
#              following redirects.
#
# Returns    : The HTTP status code.
#
# Parameters : $filename - the file name to use when saving the file.
#              $url      - the URL to retrieve the file from.
#
# Throws     : "unable to run curl: $reason" if curl can't be executed.
#              "curl exited with status code $code" on non-zero exit.
sub fetch_file {
    my ( $filename, $url ) = @_;

    # Build the command.
    my @cmd = (
        CURL, '-k', '-L', '-w', '%{http_code}', '-sSo', $filename, $url
    );

    # Run the command and retrieve the output.
    open my $in, '-|', @cmd
        or croak "uanble to run curl: $ERRNO";
    my $output = do { local $INPUT_RECORD_SEPARATOR; <$in> };
    close $in
        or croak "curl exited with status code: $CHILD_ERROR";

    return $output;
}

##########################################################################
# Usage      : spew_usage()
#
# Purpose    : Prints a usage message to STDERR.
#
# Returns    : Nothing.
#
# Parameters : None.
#
# Throws     : No exceptions.
sub spew_usage {
    my $prog = basename $0;
    print {*STDERR} <<"END_OF_USAGE";
Usage:
    $prog -o filename url
    $prog --output=filename url
END_OF_USAGE
}
