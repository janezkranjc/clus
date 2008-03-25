package sit.searchAlgorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jgap.*;
import org.jgap.impl.*;

import sit.TargetSet;
import sit.mtLearner.MTLearner;
import clus.data.type.ClusAttrType;

public class GeneticSearch extends SearchAlgorithmImpl{
	protected MTLearner learner;
	final protected int MAX_ALLOWED_EVOLUTIONS = 150; 

	public TargetSet search(ClusAttrType mainTarget, TargetSet candidates) {
		//create the configuration, nothing fancy for now
		Configuration.reset();
		Configuration conf = new DefaultConfiguration();
		FitnessFunction SITFitness = new SITFitnessFunction(mainTarget, learner, candidates);
		Genotype population = null;
		try {
			//create the sampleChromosone
			//the chromosone consists of boolean genes
			//one for each candidate in the targetset
			IChromosome sampleChromosome = new Chromosome(conf,
					new BooleanGene(conf),candidates.size());
			conf.setSampleChromosome(sampleChromosome);
			conf.setPopulationSize(50);
			conf.setFitnessFunction(SITFitness);
			conf.setPreservFittestIndividual(true);
			conf.setKeepPopulationSizeConstant(false);
			List l = conf.getGeneticOperators();
			Iterator i = l.iterator();
			while(i.hasNext()){
				GeneticOperator o = (GeneticOperator) i.next();
				if(o instanceof MutationOperator){
					((MutationOperator)o).setMutationRate(1);
				}
			}
			//lets create a population
			population = Genotype.randomInitialGenotype( conf );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		//let the evolution commence!
		Chromosome bestSolutionSoFar = null;
		for( int i = 0; i < MAX_ALLOWED_EVOLUTIONS; i++ )
		{
			population.evolve();
			bestSolutionSoFar = (Chromosome) population.getFittestChromosome();
			System.out.print("Best fitness so far:"+bestSolutionSoFar.getFitnessValue());
			System.out.println(" Best support set:"+getTargetSet(candidates,bestSolutionSoFar));
			
		}
		return getTargetSet(candidates, bestSolutionSoFar);
	}

	

	final static protected TargetSet getTargetSet(TargetSet t,Chromosome c){
		
		Object[] targets =  t.toArray();
		TargetSet result = new TargetSet();
		Gene[] genes =  c.getGenes();
		for(int i=0;i<t.size();i++){
			if(((BooleanGene)genes[i]).booleanValue()){
				result.add(targets[i]);
			}
		}
		
		return result;

	}
	public String getName() {
		return "GeneticSearch";
	}

}
