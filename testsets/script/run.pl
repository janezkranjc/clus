#!/usr/bin/perl

$file = $ARGV[0];
$file =~ s/^\.\///;

$stem = $file;
$stem =~ s/\.s$//;

if ($file eq $stem) {
   die "Not a settings file: '$file'";
}

open(LOG, ">>log.txt") || die "Can't create 'log.txt'";
print LOG "RUN: $file\n";
close(LOG);

$hasclean = 0;
open(IN, $file) || die "Can't open '$file'";
while (($hasclean == 0) && ($line = <IN>)) {
   chomp($line);
   if ($line =~ /^\%\s*\*RUN\*\s*$/) {
      $done = 0;
      $hasclean = 1;
      while (($done == 0) && ($line = <IN>)) {
         chomp($line);
         if (($line =~ /^\s*$/) || !($line =~ /^\s*\%/)) {
            $done = 1;
         } else {
            if (!($line =~ /^\s*\%\%/)) {
               $line =~ s/^\s*\%\s*//;
               $line =~ s/cdiff/perl script\/cdiff.pl/;
               $eval = "\$line =~ s/\\%f/$stem/g";
               eval($eval);
               print "$line\n";
               system($line);
            }
         }
      }
   }
}
close(IN);

if ($hasclean == 0) {
   die "No cleaning information in '$file'";
}
