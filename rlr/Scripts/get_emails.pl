#!/opt/bin/perl
# This script takes two arguments:
# 1. The filename of a file containing the html to get emails
# 2. A target file in which to put the goals-per-number-of-steps.emails

if( $#ARGV < 1 ){

    $myprog = $0; # the absolute program name is held in $0 
    # strip out the path and grab just the filename
    $myprog =~ s!^.*/!!; 

    print "\n***Usage: $myprog <htmlfile> <newfile>\n\n";
      
} else {  

    $in = $ARGV[0];
    $out = $ARGV[1];

        open(IN, $in) || die "Can't open $in: $!\n";
        open(OUT, ">$out") || die "Can't open $out: $!\n";
        while ($line = <IN>) {
	    chomp($line);
	    #if( $line =~ m'mailto:(\w+\@\[\.\w]+)' ){ # try to find name
	    if( $line =~ m'mailto:((\S+)\@\S+)\>\S+\@' ){ # try to find name
		#print "$1\n";
		print OUT "$1,\n ";
	    }
	}

}
