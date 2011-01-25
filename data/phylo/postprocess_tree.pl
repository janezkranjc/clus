###########################################################################################
#
# This script postprocesses the .out file. It extracts the tree from it and produces
# three files: .clus-phy_tree (that contains the tree in two formats), .clus-phy_newick
# (that contains the tree in newick format), and .clusters (that lists the leaf node
# clusters, one per line, only relevant if a stop criterion is used).
#
# USAGE:
# run "perl postprocess_tree.pl appl" 
# where "appl" is the name of the settings file without ".s".
#
###########################################################################################

$appl=$ARGV[0];

# compact tree

open(OUT,"$appl.out") or die "$appl.out file was not found!\n";
open(POST,">$appl.clus-phy_tree") or die "can not open $appl.clus-phy_tree!\n";
open(TMP,">$appl.tmp") or die "can not open $appl.tmp!\n";

print "Producing phylogenetic tree structure...\n";

print POST "Phylogenetic tree structure:\n\n";

$line=<OUT>;
while ($line !~ /Original Model/)
{
	$line=<OUT>;
}
$line=<OUT>;
$line=<OUT>;

while ($line=<OUT> and $line !~ /Pruned Model/)
{
	if ($line =~ /(.+)LEAF : (.+) sequence/)
	{
		$prefix = $1;   
		$nbseq = $2;
		
		if ($nbseq > 1)
		{
			print "Warning: more than one sequence found in a leaf!\n";
		}
		
		print TMP "$prefix";
		$prefix =~ s/yes:/ /g;
		$prefix =~ s/no://g;
		print POST "$prefix";
		
		for $i (1..$nbseq)
		{
			$line=<OUT>;
			if ($line =~ /"(.+)"/)
			{
				print POST " $1";
				print TMP " $1";
			}
			else
			{
				print "no leaf name found\n";
			}
		}
		print POST "\n";
		print TMP "\n";
		$line=<OUT>;
		$line=<OUT>;
		$line=<OUT>;
		$line=<OUT>;
		$line=<OUT>;
		$line=<OUT>;
		$line=<OUT>;
		}
	elsif (($line =~ /(.+:).+=/) or ($line =~ /(.+:).+in/))
	{
		$prefix = $1;
		$prefix =~ s/yes://g;
		$prefix =~ s/no://g;
		print POST "$prefix CLUSTER\n";
		print TMP "$line";
	}
	elsif (($line =~ /=/) or ($line =~ /in/))
	{
		print TMP "$line";
	}
	else
	{
		print POST $line;
		print TMP $line;
	}
}
close(OUT);
close(POST);
close(TMP);

# newick

print "Producing newick format...\n";

open(IN,"$appl.clus-phy_tree");
open(OUT,">$appl.clus-phy_newick") or die "can not open $appl.clus-phy_newick!\n";
open(CLUS,">$appl.clusters") or die "can not open $appl.clusters!\n";
$line=<IN>;
&produce_newick_string_and_clustering();
print OUT ";";
close(IN);
close(OUT);
close(CLUS);


# full tree

print "Producing phylogenetic tree with all splits listed...\n";

open(IN,"$appl.tmp");
@lines = <IN>;
close(IN);
open(OUT,">>$appl.clus-phy_tree");
print OUT "\n\nPhylogenetic tree with all splits listed:\n\n";
print OUT @lines;
close(OUT);
system("rm -f $appl.tmp");

# newick with labels

print "Producing newick format with all splits listed...\n";

open(IN,"$appl.clus-phy_tree");
open(OUT,">$appl.clus-phy_newick_labels") or die "can not open $appl.clus-phy_newick_labels!\n";
$line=<IN>;
while ($line !~ /Phylogenetic tree with all splits listed/)
{
	$line=<IN>;
}
$line=<IN>;
&produce_newick_string_with_labels();
print OUT ";";

close(IN);
close(OUT);

print "Done!\n";

# subroutines

sub produce_newick_string_with_labels {
	my $line=<IN>;
	if (($line =~ /\+--\S+:\s+(p\d+.*)$/) or ($line =~ /^(p\d+.*)$/))
	{
		my $name = "$1\n";
		chomp($name);
		print OUT "(";
		&produce_newick_string_with_labels();
		print OUT ",";
		&produce_newick_string_with_labels();
		print OUT ")\'$name\'";
	}
	else
	{
		$line =~ /\+--\S+:\s+(\S.+)$/ or die "wrong format sequence line: $line\n";		
		my $seqs = $1;
		@sequences = split(" ",$seqs);
		produce_sequences(@sequences);
	}
}

sub produce_newick_string_and_clustering {
	my $line=<IN>;
	if (($line =~ /CLUSTER/) or ($line =~ /^\n$/))
	{
		print OUT "(";
		&produce_newick_string_and_clustering();
		print OUT ",";
		&produce_newick_string_and_clustering();
		print OUT ")";
	}
	else
	{
		$line =~ /\+--\s+(\S.+)$/ or die "wrong format sequence line: $line\n";
		print CLUS "$1\n";
		@sequences = split(" ",$1);
		produce_sequences(@sequences);
	}
}

sub produce_sequences {
	@seqs = @_;
	$nbseq = @seqs;
	if ($nbseq == 1)
	{
		#$seqs[0] =~ s/_//g;
		print OUT $seqs[0];
	}
	else
	{
		#$seqs[0] =~ s/_//g;
		print OUT "($seqs[0],";
		shift(@seqs);
		&produce_sequences(@seqs);
		print OUT ")";
	}
}
