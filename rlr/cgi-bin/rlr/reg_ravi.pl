#!/usr/bin/perl
open(FP,'> touche');
close(FP);
# By Brian Exelbierd (bex@ncsu.edu) 6-9-95
#
# guestbook.pl
#	Process entries to the guestbook.  Used in my tutorial.
#
# This program uses Steve Brenner's cgi-lib.pl library to handle the input
# 
# Modified by Lynn Ryan on 8/22/97 for the RL people database.
#   Uses an included read-parse routine rather than the cgi-lib ReadParse
#
# Modified by Lynn Ryan on 11/27/97 to also write to whatsnew.html
# initialization section

$true = 1;
$false = 0;

$exclusive_lock = 2;
$unlock = 8;

$document_root = "/anw/www-anw/httpd/htdocs/rlr";
$guest_file = "/list.html";
$new_file = "/whatsnew.html";
$full_path = $document_root . $guest_file;
$new_path = $document_root . $new_file;

$url_root = "http://www-anw.cs.umass.edu/rlr";
$full_url = $url_root . $guest_file;

$webmaster = "www-anw\@www-anw.cs.umass.edu";


# Read and parse the input pairs into the associative array 
#   $FORM{varname}

#if ($ENV{'REQUEST_METHOD'} eq 'POST') {
     #read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
     $buffer="firstname=Mohammad&lastname=Ghavamzadeh&organization=UMass&ar=yes";
     @pairs = split(/&/, $buffer);
     foreach $pair (@pairs) {
         ($name, $value) = split(/=/, $pair);
         $value =~ tr/+/ /;
         $value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
             $FORM{$name} = $value;
     }  # end foreach $pair

     # open guestbook to read the number of lines it contains	
     open(GUESTBOOK,"<" . $full_path);	
     @lines = <GUESTBOOK>;
     close(GUESTBOOK);	
     $size=@lines;	

     # open guestbook for writing
     if (open(GUESTBOOK,">" . $full_path) ) {
         flock (GUESTBOOK, $exclusive_lock);

         # Move to the correct alphabetical location in the file

         $found_name = $false;
         for ($i=0; $i<=$size; $i++) {
	     $currline = $lines[$i];
	  #   print "line number $i contains $currline\n";

         if ($currline =~ /<!nextperson>/ ) {

	     #first isolate the last name on the current line
	     ($comment, $rest) = split(/<B>/, $currline, 2);
	     ($nextname, $rest) = split(/,/, $rest, 2);
	        #  print "found a name on a line, $nextname\n";

             if ($nextname gt $FORM{'lastname'} && $found_name != $true) {
	
	   	   # we need to back up one line 
		   $i = $i - 1;
		   $found_name = $true;

		   &print_new_entry;	

             }    # end if nextname > newname

	  else {   # print the current name
             print GUESTBOOK $currline;
	  }

        }    # end if line contains newname comment
      else {	# line did not contain newname comment, so print it out
          print GUESTBOOK $currline;
        }
    }   # end for

    # now handle the case when the new name is alphabetically after all
    # those already on the list	
       #  print "Value of found_name boolean is $found_name";
    if ($found_name == $false) {
	&print_new_entry;
    }

    flock(GUESTBOOK, $unlock);
    close(GUESTBOOK);
   
    # now write to the What's New page
     # open whatsnew page to read the number of lines it contains	
     open(WHATSNEW,"<" . $new_path);	
     @lines = <WHATSNEW>;
     close(WHATSNEW);	
     $size=@lines;	
     open(WHATSNEW,">" . $new_path);	
     flock (WHATSNEW, $exclusive_lock);

     for ($i=0; $i<=$size; $i++) {
       $currline = $lines[$i];
       if ($currline =~ /<!add_here>/ ) {
          print WHATSNEW $currline;
	  &print_whatsnew_entry;	
       }    # end if add_here comment found
       else {   # print the current name
          print WHATSNEW $currline;
       }
     }   # end for

    flock(WHATSNEW, $unlock);
    close(WHATSNEW);

    # now output the thanks page, and give them an opportunity to link to 
    # the researcher-list page

  &thank_you;

   }  # end if successful guestbook open
#}  # end if method = post

#else { 	
		# Format an error message for the user
#
#    print "Content-type: text/html\n\n";
#    print "<HTML>\n";
#    print "<HEAD>\n";
#    print "<TITLE>Comment Form Error</TITLE>\n";
#    print "</HEAD>\n";
#    print "<BODY>\n";
#    print "<H1>Comment Form Error</H1>\n";
#    print "<HR>\n";
#    print "<P>\n";
#    print "Form input was not processed.  Please mail your ";
#    print "remarks to $webmaster\n";
#    print "</BODY>\n";
#    print "</HTML>\n";
#}

sub print_new_entry {

    print GUESTBOOK "<!nextperson><B>$FORM{'lastname'}, $FORM{'firstname'}</B>"; 
    print GUESTBOOK " with: $FORM{'organization'}\n";
    print GUESTBOOK "<BLOCKQUOTE><B>Research interests: </B>\n";
    if ($FORM{'fa'} eq "yes")  {
        print GUESTBOOK " Function approximation | \n";
    }
    if ($FORM{'hm'} eq "yes") {
        print GUESTBOOK " Hierarchical methods | \n";
    }
    if ($FORM{'ar'} eq "yes") {
        print GUESTBOOK " Applications to robotics | \n";
    }
    if ($FORM{'in'} eq "yes") {
        print GUESTBOOK " Industrial applications | \n";
    }
    if ($FORM{'po'} eq "yes") {
        print GUESTBOOK " Partially-observable problems | \n";
    }
    if ($FORM{'td'} eq "yes") {
        print GUESTBOOK " TD-learning | \n";
    }
    if ($FORM{'un'} eq "yes") {
        print GUESTBOOK " Average-reward/undiscounted methods | \n";
    }
    if ($FORM{'sh'} eq "yes") {
        print GUESTBOOK " Shaping | \n";
    }
    if ($FORM{'pl'} eq "yes") {
        print GUESTBOOK " Planning | \n";
    }
    if ($FORM{'ps'} eq "yes") {
        print GUESTBOOK " Policy-space Search Methods | \n";
    }
    if ($FORM{'dp'} eq "yes") {
        print GUESTBOOK " DP/MDP | \n";
    }
    if ($FORM{'nb'} eq "yes") {
        print GUESTBOOK " Neuro-biological RL | \n";
    }
    if ($FORM{'th'} eq "yes") {
        print GUESTBOOK " Theoretical analysis | \n";
    }
    if ($FORM{'di'} eq "yes") {
        print GUESTBOOK " Distributed and Multi-Agent RL | \n";
    }
    print GUESTBOOK "<BR>\n";

    if ($FORM{'email'} ne "" ) {	
	print GUESTBOOK "<B>E-mail:</B> <A
	   HREF=mailto:$FORM{email}>$FORM{email}</A><BR>\n";
    }
    if (($FORM{'homepage'} ne "http://") && ($FORM{'homepage'} ne "")) {	
	print GUESTBOOK "<B>Home page:</B> <A
	   HREF=$FORM{homepage}>$FORM{homepage}</A><BR>\n";
    }
    print GUESTBOOK "</BLOCKQUOTE><HR>\n";
}

sub print_whatsnew_entry {

    # first get the current date and write it to the file
    ($sec,$min,$hours,$day,$mon,$year,$wday,$yday,$isdst)=localtime(time);
    $adjust_month = $mon + 1;
    $adjust_year = $year+1900;
    print WHATSNEW "$adjust_month/$day/$adjust_year - New Researcher Added:<br>\n";

    # now write everything else

    print WHATSNEW "<!nextentry><BLOCKQUOTE>\n";
    print WHATSNEW "<B>$FORM{'lastname'}, $FORM{'firstname'}</B>\n"; 
    print WHATSNEW " with: $FORM{'organization'}<BR>\n";
    print WHATSNEW "<B>Research interests: </B>\n";
    if ($FORM{'fa'} eq "yes")  {
        print WHATSNEW " Function approximation | \n";
    }
    if ($FORM{'hm'} eq "yes") {
        print WHATSNEW " Hierarchical methods | \n";
    }
    if ($FORM{'ar'} eq "yes") {
        print WHATSNEW " Applications to robotics | \n";
    }
    if ($FORM{'in'} eq "yes") {
        print WHATSNEW " Industrial applications | \n";
    }
    if ($FORM{'po'} eq "yes") {
        print WHATSNEW " Partially-observable problems | \n";
    }
    if ($FORM{'td'} eq "yes") {
        print WHATSNEW " TD-learning | \n";
    }
    if ($FORM{'un'} eq "yes") {
        print WHATSNEW " Average-reward/undiscounted methods | \n";
    }
    if ($FORM{'sh'} eq "yes") {
        print WHATSNEW " Shaping | \n";
    }
    if ($FORM{'pl'} eq "yes") {
        print WHATSNEW " Planning | \n";
    }
    if ($FORM{'ps'} eq "yes") {
        print GUESTBOOK " Policy-space Search Methods | \n";
    }
    if ($FORM{'dp'} eq "yes") {
        print WHATSNEW " DP/MDP | \n";
    }
    if ($FORM{'nb'} eq "yes") {
        print WHATSNEW " Neuro-biological RL | \n";
    }
    if ($FORM{'th'} eq "yes") {
        print WHATSNEW " Theoretical analysis | \n";
    }
    if ($FORM{'di'} eq "yes") {
        print WHATSNEW " Distributed and Multi-Agent RL | \n";
    }
    print WHATSNEW "<BR>\n";

    if ($FORM{'email'} ne "" ) {	
	print WHATSNEW "<B>E-mail:</B> <A
	   HREF=mailto:$FORM{email}>$FORM{email}</A><BR>\n";
    }
    if (($FORM{'homepage'} ne "http://") && ($FORM{'homepage'} ne "")) {	
	print WHATSNEW "<B>Home page:</B> <A
	   HREF=$FORM{homepage}>$FORM{homepage}</A><BR>\n";
    }
    print WHATSNEW "</BLOCKQUOTE><HR>\n";
}


sub thank_you {

    print "Content-type: text/html\n\n";
    print "<HTML>\n";
    print "<HEAD><TITLE>Thank you for registering as an RL researcher.</TITLE></HEAD>\n";
    print "<BODY BGCOLOR=\"FFFFBB\" TEXT=\"000000\" LINK=\"0000FF\">\n";
    print "<H2>Thank you</H2>\n";
    print "<HR>\n";
    print "Thank you, $FORM{firstname} $FORM{lastname}, for submitting
	your name as a RL researcher.\n\n";
    print "If you would like to see the updated researchers page, click
	<A HREF=$full_url>here</A>.\n";
    print "</BODY></HTML>\n";
}

