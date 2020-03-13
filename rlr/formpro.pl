#!/usr/local/bin/perl

$webmaster = "gamo\@csee.usf.edu";

if ($ENV{'REQUEST_METHOD'} eq 'POST') {
    read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
    @pairs = split(/&/, $buffer);
    foreach $pair (@pairs) {
        ($name, $value) = split(/=/, $pair);
        $value =~ tr/+/ /;
        $value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
        $FORM{$name} = $value;
    }
    open (MESSAGE,"| /usr/lib/sendmail -t");
    print MESSAGE "To: $webmaster\n";

    if ($FORM{email} ne "") {
        print MESSAGE "Reply-To: $FORM{email}\n";
    }
    print MESSAGE "Subject: Comments from $FORM{name}\n\n";
    print MESSAGE "Name: $FORM{name}\n";
    print MESSAGE "Email: $FORM{email}\n\n";
    print MESSAGE "Thanks for give me some information about you:\n";
    print MESSAGE "Subject: Comments from $FORM{name}\n\n";

    print MESSAGE "Hello: $FORM{name}\n";
    print MESSAGE "You typed that your e-mail is: $FORM{email}\n";


    print MESSAGE "Suggestion about the period to travel\n";

    if ( ($FORM{days} < 10) ) {
        print MESSAGE "You won't have enough time to get a good taste of Spain\n";
    }
    if ( ($FORM{days} > 10 ) ) {
        print MESSAGE "Hopefully you'll have enough time to travel around \n";
    }
    if (($FORM{people} = 1) ){
       print MESSAGE "Probably you can meet somebody...so you won't be lonely\n";
    }
    if (($FORM{people} = 2) ){
       print MESSAGE "Spain is a very romantic country.\n";
    }
    if (($FORM{people} > 2) ){
    print MESSAGE "More than 2 people,lots of money travelling\n";
    }

    if ( ($FORM{preference} eq "Sclass") ) {
        print MESSAGE "Second class transportation have more than 35% savings\n";
    }

    if ( ($FORM{preference} eq "Fclass") ) {
        print MESSAGE "First class transportation is a bit expensive in Europe\n";
    }

    if ( $FORM{preference} eq "N" )  {
        print MESSAGE "No travel preference given. Enjoy the adventure!\n";
    }

    print MESSAGE "Recommendation? Please be careful with the sun...\n\n";


    if ( $FORM{visit1} ) {
        print MESSAGE "You want to travel to Sevilla,visit la Giralda\n";
    }

    if ( $FORM{visit2}) {
        print MESSAGE "If you visit Madrid, you should visit the Museo del Prado\n";
    }
  print MESSAGE "\nComments:\n";
    print MESSAGE "$FORM{comments}\n";



    &thank_you;
} 
sub thank_you {

    print "Content-type: text/html\n\n";
    print "<HTML>\n";
    print "<HEAD><TITLE>Thanks for filling the Form</TITLE></HEAD>\n";
    print "<BODY BGCOLOR=\"FFFFFF\" TEXT=\"000000\" LINK=\"0000FF\">\n";
    print "<H2 ALIGN=CENTER>Have a good day!.</H2>\n";
    print "<HR>\n";
    print "Thanks $FORM{name} for your input regarding your travel preferences.\n";
    print "<P>Back to top <A HREF=\"http://www.csee.usf.edu/~gamo/index.html\" TARGET=\"MAIN\">to my webpage</A>.\n";

    print "</BODY></HTML>\n";
}
