#!/usr/local/bin/perl
# By Brian Exelbierd (bex@ncsu.edu) 6-9-95
#
# guestbook.pl
#	Process entries to the guestbook.  Used in my tutorial.
#
# This program uses Steve Brenner's cgi-lib.pl library to handle the input
push(@INC,"/home/suntan1/student/gamo/public-html/cgi-bin");
require("cgi-lib.pl");

&ReadParse;

$quit = 0;
while ($quit != 1) { 
  if (-e ".guestlock") {
    # The file existed
    sleep(1);
    }
  else {

    # Create the lock file, thereby locking the guestbook
    open(LOCK,">.guestlock");
    close LOCK;

    # open and read in the old guestbook
    
 open(GB,"<guestbook.html");
    @lines = <GB>;
    close GB;

    # Expand the comments for html
    $in{'comments'} =~ s/\n/<BR>\n/go;

    # Empty the old guest book, and print it out again, adding the new entry
    
open(GB,">guestbook.html");
    foreach $line (@lines) {
      $line =~ s/<!--INSERT HERE-->/<!--INSERT HERE-->\n<P><B>Name:<\/B>$in{'name'}<BR>\n<B>Comments:<\/B>\n$in{'comments'}<\/P><HR>\n/o;
      print GB $line;
      }
    close GB;

    # unlock the file
    unlink(".guestlock");

    # Return the new guestbook, and set $quit = 1
    print "Location: http://www.csee.usf.edu/~gamo/cgi-bin/guestbook.html\n\n";
    $quit = 1;
    }
  }
