#!/opt/bin/perl

$recipient = "ryanlyn1\@cps.msu.edu";

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
    print MESSAGE "To: $recipient\n";

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
    print "Thanks $FORM{name} for your submission.\n";
    print "</BODY></HTML>\n";
}
