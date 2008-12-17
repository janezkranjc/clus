###########################################################################################
#
# This script runs Clus-SC, starting from the HMC data and settings file. 
# (The settings file can be changed to optimise the ftest for SC.)
#
# RESULT:
# file.out for each class
#
# USAGE: 
# Put the HMC datafiles, the HMC settingsfile, and this file in the same directory and 
# run "perl run_sc.pl name" where "name" is the name of the settingsfile, without ".s".
#
###########################################################################################

# Overwrite this variable to point to the Clus directory
$clusdir = "$ENV{HOME}/Clus";

$clusmem = "1800000000";

$sfile = @ARGV[0];

# Construct new .arff files for each file listed in the settings file
# ------------------------------------------------------------------- 

read_settings("$sfile.s");

# find out which datafiles are listed in the settings file
@splitfiles=("File");
if (defined($SETTINGREAD{"Data"}{"TestSet"})) {
	push(@splitfiles,"TestSet");
}
if (defined($SETTINGREAD{"Data"}{"PruneSet"})) {
	push(@splitfiles,"PruneSet");
}

# for each datafile, construct new file to be used in SC learning
foreach $split (@splitfiles) {
	add_settval("Hierarchical", "Type", $SETTINGREAD{"Hierarchical"}{"Type"});
	add_settval("Hierarchical", "HSeparator", $SETTINGREAD{"Hierarchical"}{"HSeparator"});
	add_settval("Data", "File", $SETTINGREAD{"Data"}{$split});
	write_s_file("$sfile-make-SC-arff");
	run_clus("addon.hmc.HMCConvertToSC.HMCConvertToSC $sfile-make-SC-arff SC");
	$filename = $SETTINGREAD{"Data"}{$split};
	$filename =~ /(\S+)\.arff/;
	$newfilename = "$1" . "_SINGLE.arff";
	system("mv SC.arff $newfilename");
	system("rm -f $sfile-make-SC-arff.s");
}


# Find beginning and end of the target attributes in the new .arff files
# ----------------------------------------------------------------------
open(IN,$newfilename);
$line=<IN>;
while ($line !~ /ATTRIBUTE/) {
	$line=<IN>;
}
# $line =~ /ATTRIBUTE/
$attcount=1;
while ($line !~ /ATTRIBUTE.+hierarchical.+p,n/) {
	$line=<IN>;
	$attcount++;
}
# $line =~ /ATTRIBUTE.+hierarchical.+p,n/
$targetstart = $attcount;
@classnames = ();
while ($line =~ /ATTRIBUTE\s+(\S+)\s+hierarchical.+p,n/) {
	$classname = $1;
	$classname =~ s/\//-/g;  # the "/" symbol should not occur in the filenames
	push(@classnames,$classname);
	$line=<IN>;
	$attcount++;
}
# $line !~ /ATTRIBUTE.+hierarchical.+p,n/
$targetend = $attcount - 1;
close(IN);


# Construct a new settings file for each target
# ---------------------------------------------
# construct template settingsfile
open(TP,">settingsfile_template.s");
open(IN,"$sfile.s");
$targetwritten = 0;
while ($line=<IN>) {
	$line =~ s/\.arff/_SINGLE\.arff/g;
	$line =~ s/\.arff\.zip/\.arff/g;
	print TP $line;
	if ($line =~ /\[Attributes\]/) {		
		print TP "Target = ?\n";
		print TP "Disable = $targetstart-$targetend\n";
		$targetwritten = 1;
	}
}
if ($targetwritten==0) {
	print TP "[Attributes]\n";
	print TP "Target = ?\n";
	print TP "Disable = $targetstart-$targetend\n";
}
close(IN);
close(TP);

# for each target, copy the template to a new settingsfile, indicating the right target attribute index
$targetindex = $targetstart;
open(TP,"settingsfile_template.s");
@tplines = <TP>;
close(TP);
foreach $cln (@classnames) {
	open(SETT,">${sfile}_${cln}.s");
	foreach $tpl (@tplines)	{
		if ($tpl =~ /Target/) {
			print SETT "Target = $targetindex\n";
		}
		else {
			print SETT $tpl;
		}
	}
	$targetindex++;
}
system("rm -f settingsfile_template.s");


# Run clus for each target separately
# -----------------------------------
foreach $cln (@classnames) {
	run_clus("clus.Clus ${sfile}_${cln}.s");
}

# Subroutines
# -----------
sub read_settings {
    my ($fname) = @_;
	open(FILE, "$fname") || die "can't open $fname";
    while ($line = <FILE>) {
        $line =~ s/[\n\r]//g;
        if ($line =~ /^\[(.*)\]\s*$/) {
            $section = $1;
            $SECSREAD{$section} = 1; 
        } elsif ($line =~ /^(\S+)\s*=\s*(\S+)\s*$/) {
            $name = $1;
            $value = $2;
            $VALSREAD{"$section-$name"} = 1;
            $SETTINGREAD{$section}{$name} = $value;
        } elsif (!($line =~ /^\s*$/)) {
            die "Error '$line' in $fname";
        }
    }
    close(FILE);
}

sub write_s_file {
    my ($fname) = @_;
    open(OUT, ">${fname}.s") || die "can't create ${fname}.s";
    foreach $sec (keys %SECS) {
        print OUT "[$sec]\n";
        foreach $set (keys %VALS) {
            ($ssec, $sname) = split(/\-/, $set);
            if ($ssec eq $sec) {
                if (defined($LOCALSETTING{$sec}{$sname})) {
                    $value = $LOCALSETTING{$sec}{$sname};
                    print "$sname = $value\n";          
                } else {
                    print "not defined $sec $name \n";
                }
                print OUT "$sname = $value\n";
            }
        }
        print OUT "\n";
    }
    close(OUT);
}

sub add_sett {
    my($sec, $sett) = @_;
    $SECS{$sec} = 1;
    $VALS{"$sec-$sett"} = 1;
}

sub add_settval {
    my($sec, $sett, $val) = @_;
    add_sett($sec, $sett);
    $LOCALSETTING{$sec}{$sett} = $val;
}

# Run clus
sub run_clus {
    my($args) = @_;
    if (-f "$clusdir/bin/clus/Clus.class") {
	# Using the CVS version or a self-compiled Clus
        $clus_cp = "$clusdir/bin:$clusdir/jars/commons-math-1.0.jar:$clusdir/jars/jgap.jar";
    } else {
	# If Clus was downloaded, then use included pre-compiled .jar file
        $clus_cp = "$clusdir/Clus.jar";
    }
    $cmd = "java -Xmx$clusmem -cp $clus_cp $args";
    print "Running: $cmd\n";
    system($cmd);
}
