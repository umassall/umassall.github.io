#!/exp/rcf/share/bin/perl
# By Brian Exelbierd (bex@ncsu.edu) 6-9-95
#
# guestbook.pl
#	Process entries to the guestbook.  Used in my tutorial.
#
# This program uses Steve Brenner's cgi-lib.pl library to handle the input
# 
# Modified by Lynn Ryan on 9/2/97 for the RL publications pages.
#   Uses an included read-parse routine rather than the cgi-lib ReadParse
# Modified by Lynn Ryan on 11/28/97 to also write to the what's new page

# initialization section

$true = 1;
$false = 0;

$exclusive_lock = 2;
$unlock = 8;

$document_root = "/anw/www-anw/httpd/htdocs/rlr/";
$url_root = "http://www-anw.cs.umass.edu/rlr/";
$webmaster = "www-anw\@www-anw.cs.umass.edu";
$new_file = "whatsnew.html";
$new_path = $document_root . $new_file;

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


     # open the proper guestbook(s) to read the number of lines it
     # contains
     	
     # get the names of the one or two files that will be appended	
     &get_file_names;	

     # but first write to the new file (the one that contains the
     # publication information separately)
     &write_new_file;	

     open(GUESTBOOK1,"<" . $full_path1);
     # print "Path1 is $full_path1\n";
     
     @lines1 = <GUESTBOOK1>;
     close(GUESTBOOK1);	
     $size1=@lines1;	
     # print "Size of file 1 is $size1\n";

     # open guestbooks for writing
     open(GUESTBOOK1,">" . $full_path1);
     flock (GUESTBOOK1, $exclusive_lock);
     &write_guestbook1;

     if ($second_name_found == $true) {
	 # print "found second file name\n"; 
         open(GUESTBOOK2,"<" . $full_path2);
         @lines2 = <GUESTBOOK2>;
         close(GUESTBOOK2);	
         $size2=@lines2;	
	# print "size of second file is $size2\n"; 
         if (open(GUESTBOOK2,">" . $full_path2) ) {
	# print "able to open second file name\n"; 
             flock (GUESTBOOK2, $exclusive_lock);
                &write_gb2;
         }
     }

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
           &write_whatsnew_entry;
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


# --------------------------  BEGIN SUBROUTINES -------------------------
sub write_new_file {

     # open a new HTML file that will store the full text of the
     # abstract
     $new_filename_found = $false;
     $i = 1;

     # remove any spaces in the name
     $FORM{'lastname1'} =~ s/ +//g; 

     # print "lastname is now $FORM{'lastname1'}END\n";

     while ($new_filename_found == $false) {
        $new_filename = $document_root . "pub/";
        $new_filename = $new_filename . $FORM{'lastname1'};
        $new_filename = $new_filename . "$i";
        $new_filename = $new_filename . ".html";
# print "new filename is $new_filename\n";
        if (-e $new_filename) {
             $i = $i + 1;
        }
        else {
             $new_filename_found = $true;
             open(NEWFILE,">" . $new_filename);
             $full_url_new = $url_root . "pub/";	
             $full_url_new = $full_url_new . $FORM{'lastname1'};	
             $full_url_new = $full_url_new . "$i";	
             $full_url_new = $full_url_new . ".html";	
	#	print "new file url is (just created) $full_url_new\n";
        }
     }  

    print NEWFILE "<HTML><HEAD>\n";
    print NEWFILE "<TITLE>$FORM{'title'}</TITLE></HEAD>\n";
    print NEWFILE "<BODY bgcolor\=\"FFFFBB\">\n";	
    print NEWFILE "<H2>$FORM{'title'}</H2>\n";
    print NEWFILE "<!nextperson><B>$FORM{'lastname1'}, $FORM{'firstname1'}</B>";
    if ($FORM{'othernames'} ne "") {
	print NEWFILE " , $FORM{'othernames'}";
    }
    print NEWFILE "<blockquote>";
    if ($FORM{'email'} ne "" ) {	
    }

    if ($FORM{'puburl'} ne "" ) {	
	print NEWFILE "<A HREF= $FORM{'puburl'}>$FORM{'title'}</A><BR>\n\n";
    }
    else   {
	print NEWFILE "<b>$FORM{'title'}</b><BR>\n\n";
    }

    print NEWFILE "<I> $FORM{'journal'} </I>\n\n";

    if ($FORM{'format'} eq "ps") {
	print NEWFILE "(Postscript - $FORM{'pubsize'} )\n";
    }
    if ($FORM{'format'} eq "gps") {
	print NEWFILE "( gzipped Postscript - $FORM{'pubsize'} )\n";
    }
    if ($FORM{'format'} eq "cps") {
	print NEWFILE "(compressed Postscript - $FORM{'pubsize'} )\n";
    }
    if ($FORM{'format'} eq "wd") {
	print NEWFILE "(Microsoft Word - $FORM{'pubsize'} )\n";
    }
    if ($FORM{'format'} eq "ot") {
	print NEWFILE "($FORM{'otherformat'} - $FORM{'pubsize'})\n";
    }
    print NEWFILE "\n";
    print NEWFILE "<BR><BR><B>Abstract</B>: $FORM{'abstract'}\n";
	
    print NEWFILE "</BLOCKQUOTE>\n";

    close(NEWFILE);
    chmod 0755, $new_filename;

}
# -------------------------------------------------------------------

sub write_guestbook1 {

     $entered_wr1 = $true;
	
     # Move to the correct alphabetical location in the file

     $found_name1 = $false;
     for ($i=0; $i<=$size1; $i++) {
	 $currline1 = $lines1[$i];
         # print "line number $i contains $currline\n";

         if ($currline1 =~ /<!nextperson>/ ) {

	    #first isolate the last name on the current line
	    ($comment, $rest) = split(/<B>/, $currline1, 2);
	    ($nextname1, $rest) = split(/,/, $rest, 2);
	    # print "found a name on a line, $nextname\n";

            if ($nextname1 gt $FORM{'lastname1'} && $found_name1 != $true){
	
	   	   # we need to back up one line 
		   $i = $i - 1;
		   $found_name1 = $true;

		   &write_new_entry;	

             }    # end if nextname > newname

	  else {   # print the current name
             print GUESTBOOK1 $currline1;
	  }

        }    # end if line contains newname comment
      else {	# line did not contain newname comment, so print it out
          print GUESTBOOK1 $currline1;
        }
    }   # end for

    # now handle the case when the new name is alphabetically after all
    # those already on the list	
       #  print "Value of found_name boolean is $found_name";
    if ($found_name1 == $false) {
	&write_new_entry;
    }

    flock(GUESTBOOK1, $unlock);
    close(GUESTBOOK1);

}

# -------------------------------------------------------------------

sub write_gb2 {
	
     # Move to the correct alphabetical location in the file
     # print "enter write 2\n";
     # print "entered write_gb2\n";
     $entered_wr2 = $true;

     $found_name2 = $false;
     for ($i=0; $i<=$size2; $i++) {
	 $currline2 = $lines2[$i];
         # print "line number $i contains $currline\n";

         if ($currline2 =~ /<!nextperson>/ ) {

	    #first isolate the last name on the current line
	    ($comment, $rest) = split(/<B>/, $currline2, 2);
	    ($nextname2, $rest) = split(/,/, $rest, 2);
	    # print "found a name on a line, $nextname\n";

            if ($nextname2 gt $FORM{'lastname1'} && $found_name2 != $true) {
	
	   	   # we need to back up one line 
		   $i = $i - 1;
		   $found_name2 = $true;

		   &write_new_entry2;	

             }    # end if nextname > newname

	  else {   # print the current name
             print GUESTBOOK2 $currline2;
	  }

        }    # end if line contains newname comment
      else {	# line did not contain newname comment, so print it out
          print GUESTBOOK2 $currline2;
        }
    }   # end for

    # now handle the case when the new name is alphabetically after all
    # those already on the list	
       #  print "Value of found_name boolean is $found_name";
    if ($found_name2 == $false) {
	&write_new_entry2;
    }

    flock(GUESTBOOK2, $unlock);
    close(GUESTBOOK2);

}

# ------------------------------------------------------------------
sub write_new_entry {

    print GUESTBOOK1 "<!nextperson><B>$FORM{'lastname1'}, \n";
    print GUESTBOOK1 "$FORM{'firstname1'}</B>\n";
    if ($FORM{'othernames'} ne "") {
	print GUESTBOOK1 " , $FORM{'othernames'}";
    }
    if ($FORM{'email'} ne "" ) {	
	print GUESTBOOK1 "( <A
	   HREF=mailto:$FORM{email}>$FORM{email}</A>)<BR>\n";
    }
    print GUESTBOOK1 "<blockquote>";

    if ($FORM{'puburl'} ne "" ) {	
	print GUESTBOOK1 "<A HREF=$FORM{puburl}>$FORM{title}</A><BR>\n";
    }
    print GUESTBOOK1 "<i>$FORM{'journal'}</i>\n\n";

    if ($FORM{'format'} eq "ps") {
	print GUESTBOOK1 "(Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "gps") {
	print GUESTBOOK1 "( gzipped Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "cps") {
	print GUESTBOOK1 "(compressed Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "wd") {
	print GUESTBOOK1 "(Microsoft Word - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "ot") {
	print GUESTBOOK1 "($FORM{'otherformat'} - $FORM{'pubsize'})\n";
    }
    print GUESTBOOK1 "<A HREF=$full_url_new>Abstract</A>: \n";
 #	print "full url for new file is $full_url_new\n";
    $short_abstract = substr($FORM{'abstract'},0,100);
    print GUESTBOOK1 "<BR>$short_abstract...\n";
	
    print GUESTBOOK1 "</BLOCKQUOTE><HR>\n";

}
# ------------------------------------------------------------------

sub write_whatsnew_entry {

  # first get the current date and write it to the file
    ($sec,$min,$hours,$day,$mon,$year,$wday,$yday,$isdst)=localtime(time);
    $adjust_month = $mon + 1;
    $adjust_year = $year+1900;
    print WHATSNEW "$adjust_month/$day/$adjust_year - New Publication Added:<br>\n";

    print WHATSNEW "<!nextperson><B>$FORM{'lastname1'}, \n";
    print WHATSNEW "$FORM{'firstname1'}</B>\n";
    if ($FORM{'othernames'} ne "") {
	print WHATSNEW " , $FORM{'othernames'}";
    }
    if ($FORM{'email'} ne "" ) {	
	print WHATSNEW "( <A
	   HREF=mailto:$FORM{email}>$FORM{email}</A>)<BR>\n";
    }
    print WHATSNEW "<blockquote>";

    if ($FORM{'puburl'} ne "" ) {	
	print WHATSNEW "<A HREF=$FORM{puburl}>$FORM{title}</A><BR>\n";
    }
    print WHATSNEW "<i>$FORM{'journal'}</i>\n\n";

    if ($FORM{'format'} eq "ps") {
	print WHATSNEW "(Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "gps") {
	print WHATSNEW "( gzipped Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "cps") {
	print WHATSNEW "(compressed Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "wd") {
	print WHATSNEW "(Microsoft Word - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "ot") {
	print WHATSNEW "($FORM{'otherformat'} - $FORM{'pubsize'})\n";
    }
    print WHATSNEW "<A HREF=$full_url_new>Abstract</A>: \n";
 #	print "full url for new file is $full_url_new\n";
    $short_abstract = substr($FORM{'abstract'},0,100);
    print WHATSNEW "<BR>$short_abstract...\n";
	
    print WHATSNEW "</BLOCKQUOTE><HR>\n";

}

# ------------------------------------------------------------------

sub write_new_entry2 {

    print GUESTBOOK2 "<!nextperson><B>$FORM{'lastname1'}, \n";
    print GUESTBOOK2 "$FORM{'firstname1'}</B>\n";
    if ($FORM{'othernames'} ne "") {
	print GUESTBOOK2 " , $FORM{'othernames'}";
    }
    print GUESTBOOK2 "<blockquote>";
    if ($FORM{'email'} ne "" ) {	
	print GUESTBOOK2 "<B>E-mail:</B> <A
	   HREF=mailto:$FORM{email}>$FORM{email}</A><BR>\n";
    }

    if ($FORM{'puburl'} ne "" ) {	
	print GUESTBOOK2 "<A HREF=$FORM{puburl}>$FORM{title}</A><BR>\n";
    }
    print GUESTBOOK2 "<i>$FORM{'journal'}</i>\n\n";

    if ($FORM{'format'} eq "ps") {
	print GUESTBOOK2 "(Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "gps") {
	print GUESTBOOK2 "( gzipped Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "cps") {
	print GUESTBOOK2 "(compressed Postscript - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "wd") {
	print GUESTBOOK2 "(Microsoft Word - $FORM{'pubsize'})\n";
    }
    if ($FORM{'format'} eq "ot") {
	print GUESTBOOK2 "($FORM{'otherformat'} - $FORM{'pubsize'})\n";
    }
    print GUESTBOOK2 "<A HREF=$full_url_new>Abstract</A>: \n";
    $short_abstract = substr($FORM{'abstract'},0,100);
    print GUESTBOOK2 "<BR>$short_abstract...\n";
	
    print GUESTBOOK2 "</BLOCKQUOTE><HR>\n";

}

# ------------------------------------------------------------------

sub thank_you {

    print "Content-type: text/html\n\n";

    print "<HTML>\n";
    print "<HEAD><TITLE>Thank you for registering as an RL researcher.</TITLE></HEAD>\n";
    print "<BODY BGCOLOR=\"FFFFBB\" TEXT=\"000000\" LINK=\"0000FF\">\n";
    print "<H2>Thank you</H2>\n";
    print "<HR>\n";
    print "Thank you, $FORM{firstname1} $FORM{lastname1}, for submitting
	your publication to the RL Repository.\n\n";
    print "If you would like to see the updated publications page, click
	<A HREF=$full_url1>here</A>.\n";
    print "</BODY></HTML>\n";
}

# ------------------------------------------------------------------
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
    if ($FORM{'ps'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'ps';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'ps';
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
    if ($FORM{'di'} eq "yes") {
	if ($first_name_found == $false) {
            $guest_file1 = 'di';
	    $first_name_found = $true;
        }
        else  {
	    $guest_file2 = 'di';
	    $second_name_found = $true;
        }
    }

     $guest_file1 = $guest_file1 . ".html";
     $guest_file2 = $guest_file2 . ".html";
#	print "Before concatenation, filename1 is $guest_file1\n";
#	print "Before concatenation, filename2 is $guest_file2\n";

     $full_path1 = $document_root . $guest_file1;
     $full_path2 = $document_root . $guest_file2;
     $full_url1 = $url_root . $guest_file1;	
     $full_url2 = $url_root . $guest_file2;	
      # print "Guest file name 1 is $full_url1\n";
      # print "Guest file name 2 is $full_url2\n";
}
