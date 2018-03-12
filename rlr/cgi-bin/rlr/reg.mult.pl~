#!/exp/rcf/share/bin/perl
# By Brian Exelbierd (bex@ncsu.edu) 6-9-95
#
# guestbook.pl
#	Process entries to the guestbook.  Used in my tutorial.
#
# This program uses Steve Brenner's cgi-lib.pl library to handle the input
# 
# Modified by Lynn Ryan on 8/22/97 for the RL people database.
#   Uses an included read-parse routine rather than the cgi-lib ReadParse

# initialization section

$true = 1;
$false = 0;

$exclusive_lock = 2;
$unlock = 8;

$document_root = "/user/web/htdocs/groups/rlr/";
$guest_file = "/list.html";
$full_path = $document_root . $guest_file;

$url_root = "http://web.cps.msu.edu/rlr/";
$full_url = $url_root . $guest_file;

$webmaster = "ryanlyn1\@cps.msu.edu";

# Read and parse the input pairs into the associative array 
#   $FORM{varname}

if ($ENV{'REQUEST_METHOD'} eq 'POST') {
     read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
     @pairs = split(/&/, $buffer);
     foreach $pair (@pairs) {
         ($name, $value) = split(/=/, $pair);
         $value =~ tr/+/ /;
         $value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
             $FORM{$name} = $value;
     }  # end foreach $pair

     &get_file_names;

     # open guestbooks for writing
     if (open(GUESTBOOK,"<" . $full_path1) ) {
         @lines = <GUESTBOOK>;
         close(GUESTBOOK);
         $size=@lines;
         if (open(GUESTBOOK,">" . $full_path1) ) {
             flock (GUESTBOOK, $exclusive_lock);
             &write_res;
             flock(GUESTBOOK, $unlock);
	     close(GUESTBOOK);
         }
     }
     if ($second_name_found == $true) {
         # print "found second file name\n";
         open(GUESTBOOK,"<" . $full_path2);
         @lines = <GUESTBOOK>;
         close(GUESTBOOK);
         $size=@lines;
        # print "size of second file is $size2\n";
         if (open(GUESTBOOK,">" . $full_path2) ) {
            # print "able to open second file name\n";
            flock (GUESTBOOK, $exclusive_lock);
            &write_res;
            flock(GUESTBOOK, $unlock);
	    close(GUESTBOOOK);
        }
     }

    # now output the thanks page, and give them an opportunity to link to 
    # the researcher-list page

  &thank_you;

}  # end if method = post

else { 	
		# Format an error message for the user

    print "Content-type: text/html\n\n";
    print "<HTML>\n";
    print "<HEAD>\n";
    print "<TITLE>Comment Form Error</TITLE>\n";
    print "</HEAD>\n";
    print "<BODY>\n";
    print "<H1>Comment Form Error</H1>\n";
    print "<HR>\n";
    print "<P>\n";
    print "Form input was not processed.  Please mail your ";
    print "remarks to $webmaster\n";
    print "</BODY>\n";
    print "</HTML>\n";
}

sub print_new_entry {

    print GUESTBOOK "<!nextperson><B>$FORM{'lastname'}, $FORM{'firstname'}</B> with: $FORM{'organization'}\n";
    print GUESTBOOK "<BLOCKQUOTE><B>Research interests: </B>\n";
    if ($FORM{'fa'} eq "yes")  {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/fa-top.html>\n";
        print GUESTBOOK " Function approximation</a> | \n";
    }
    if ($FORM{'hm'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/hm-top.html>\n";
        print GUESTBOOK " Hierarchical methods</a> | \n";
    }
    if ($FORM{'ar'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/ar-top.html>\n";
        print GUESTBOOK " Applications to robotics</a> | \n";
    }
    if ($FORM{'in'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/in-top.html>\n";
        print GUESTBOOK " Industrial applications</a> | \n";
    }
    if ($FORM{'po'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/po-top.html>\n";
        print GUESTBOOK " Partially-observable problems</a> | \n";
    }
    if ($FORM{'td'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/td-top.html>\n";
        print GUESTBOOK " TD-learning</a> | \n";
    }
    if ($FORM{'un'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/un-top.html>\n";
        print GUESTBOOK " Average-reward/undiscounted methods</a> | \n";
    }
    if ($FORM{'sh'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/sh-top.html>\n";
        print GUESTBOOK " Shaping</a> | \n";
    }
    if ($FORM{'pl'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/pl-top.html>\n";
        print GUESTBOOK " Planning</a> | \n";
    }
    if ($FORM{'dp'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/dp-top.html>\n";
        print GUESTBOOK " DP/MDP</a> | \n";
    }
    if ($FORM{'nb'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/nb-top.html>\n";
        print GUESTBOOK " Neuro-biological RL</a> | \n";
    }
    if ($FORM{'th'} eq "yes") {
        print GUESTBOOK " <a href=http://www.cps.msu.edu/rlr/th-top.html>\n";
        print GUESTBOOK " Theoretical analysis</a> | \n";
    }
    print GUESTBOOK "<BR>\n";

    if ($FORM{'email'} ne "" ) {	
	print GUESTBOOK "<B>E-mail:</B> <A
	   HREF=mailto:$FORM{email}>$FORM{email}</A><BR>\n";
    }
    if ($FORM{'homepage'} ne "" ) {	
	print GUESTBOOK "<B>Home page:</B> <A
	   HREF=$FORM{homepage}>$FORM{homepage}</A><BR>\n";
    }
    print GUESTBOOK "</BLOCKQUOTE><HR>\n";
}

sub get_file_names  {

    $first_name_found = $false;	
    $second_name_found = $false;	

    if ($FORM{'fa'} eq "yes")  {
	if ($first_name_found == $false) {
            $guest_file1 = 'fa';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'fa';
	    $second_name_found = $true;
        }
    }

    if ($FORM{'hm'} eq "yes")  {
	# print "WENT INTO HM\n";

	if ($first_name_found == $false) {
	# print "WENT INTO IF\n";
            $guest_file1 = 'hm';
	    $first_name_found = $true;
        }
        else  {
	# print "WENT INTO ELSE\n";
	    $guest_file2 = 'hm';
	    $second_name_found = $true;
        }
    }

    if ($FORM{'ar'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'ar';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'ar';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'in'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'in';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'in';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'po'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'po';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'po';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'td'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'td';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'td';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'un'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'un';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'un';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'sh'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'sh';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'sh';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'pl'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'pl';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'pl';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'dp'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'dp';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'dp';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'nb'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'nb';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'nb';
	    $second_name_found = $true;
        }
    }
    if ($FORM{'th'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'th';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'th';
	    $second_name_found = $true;
        }
    }

     $guest_file1 = $guest_file1 . "-res.html";
     $guest_file2 = $guest_file2 . "-res.html";
#	print "Before concatenation, filename1 is $guest_file1\n";
#	print "Before concatenation, filename2 is $guest_file2\n";

     $full_path1 = $document_root . $guest_file1;
     $full_path2 = $document_root . $guest_file2;
     $full_url1 = $url_root . $guest_file1;	
     $full_url2 = $url_root . $guest_file2;	
    #  print "Guest file name 1 is $full_url1\n";
    #  print "Guest file name 2 is $full_url2\n";
}


sub write_res  {
    # Move to the correct alphabetical location in the file

    $found_name = $false;
    for ($i=0; $i<=$size; $i++) {
	$currline = $lines[$i];
     #  print "line number $i contains $currline\n";

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
	<A HREF=$full_url1>here</A>.\n";
    print "</BODY></HTML>\n";
}

