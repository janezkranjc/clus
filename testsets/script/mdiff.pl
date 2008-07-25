#!/usr/bin/perl

$f1 = $ARGV[0];
$f2 = $ARGV[1];

$diff = `diff $f1 $f2`;

if (!($diff =~ /^[\n\r\t\s]*$/)) {
    print "Diff: $f1 <-> $f2\n";
    print "$diff";
    open(LOG, ">>log.txt") || die "Can't create 'log.txt'";
    print LOG "Diff: $f1 <-> $f2\n";
    print LOG "$diff\n";
    close(LOG);
}
