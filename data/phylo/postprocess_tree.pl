# usage: perl postprocess_tree.pl appl (with appl the name of the settings file without ".s")

$appl=$ARGV[0];

# compact tree

open(OUT,"$appl.out");
open(POST,">$appl.clus-phi_tree");
open(TMP,">$appl.tmp");

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
			$prefix =~ s/yes:/ /g;
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

open(IN,"$appl.clus-phi_tree");
open(OUT,">$appl.clus-phi_newick");
$line=<IN>;
&produce_newick_string();
print OUT ";";

close(IN);
close(OUT);


# full tree

open(IN,"$appl.tmp");
@lines = <IN>;
close(IN);
open(OUT,">>$appl.clus-phi_tree");
print OUT "\n\nPhylogenetic tree with all splits listed:\n\n";
print OUT @lines;
close(OUT);
system("rm -f $appl.tmp");


# subroutines


sub produce_newick_string {
	my $line=<IN>;
	if (($line =~ /CLUSTER/) or ($line =~ /^\n$/))
	{
		print OUT "(";
		&produce_newick_string();
		print OUT ",";
		&produce_newick_string();
		print OUT ")";
	}
	else
	{
		$line =~ /\+--\s+(\S+.+)$/;
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
