#!/opt/bin/perl

# this script looks at files of type "**/a",
# where "**" is the name of the forum.

if( $#ARGV != 2 ){ # require 3 arguments

    $myprog = $0; # the absolute program name is held in $0 
    # strip out the path and grab just the filename
    $myprog =~ s!^.*/!!; 

    print "*** Usage: $myprog <First-Name> <Last-Name> <Email-Addr>\n";

}else{

    $newFN = $ARGV[0];
    $newLN  = $ARGV[1];
    @newEmail = split( /[\\]?\@/,$ARGV[2]); # capture userid and network
                                          # e.g. "abc" and "cse.msu.edu" from 
                                          # abc@cse.msu.edu or abc\@cse.msu.edu

    @afiles = split(/\s+\*?/,`ls */a`);
    foreach $file (@afiles){
      unless( $file =~ m!test! ){ # don't check "test" forums

	  #print "perl -i -p -e 's/forum_monitor = \"([\\w ]+)\"/forum_monitor = \"$newFN $newLN\"/' $file\n";
	  #print "perl -i -p -e 's/forum_monitor_email = \"([\\w\\.]+\\\\\@[\\w\\.]+)\"/forum_monitor_email = \"$newEmail[0]\\\\\\\@$newEmail[1]\"/' $file\n";
	  #print "\n\n";
	  
	  `perl -i -p -e 's/forum_monitor = \"([\\w ]+)\"/forum_monitor = \"$newFN $newLN\"/' $file`;
	  `perl -i -p -e 's/forum_monitor_email = \"([\\w\\.]+\\\\\@[\\w\\.]+)\"/forum_monitor_email = \"$newEmail[0]\\\\\\\@$newEmail[1]\"/' $file`;

    }
  }
}
