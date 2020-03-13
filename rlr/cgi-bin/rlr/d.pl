#!/opt/bin/perl

# first get the current date and write it to the file
    ($sec,$min,$hours,$day,$mon,$year,$wday,$yday,$isdst)=localtime(time);
    $adjust_month = $mon + 1;
    $adjust_year = $year+1900;
    print "Date: $adjust_month/$day/$adjust_year\n";

