package sit.searchAlgorithm;

import java.util.Iterator;

import org.jgap.*;
import org.jgap.impl.*;

import sit.TargetSet;
import sit.mtLearner.MTLearner;
import clus.data.type.ClusAttrType;

public class GeneticSearch implements SearchAlgorithm{
	protected MTLearner learner;
	final protected int MAX_ALLOWED_EVOLUTIONS = 500; 
	
	public TargetSet search(ClusAttrType mainTarget, TargetSet candidates) {
		//create the configuration, nothing fancy for now
		Configuration conf = new DefaultConfiguration();
		
		
		FitnessFunction SITFitness = new SITFitnessFunction(mainTarget, learner, candidates);
		
		
		Genotype population = null;
		try {
			//create the sampleChromosone
			//the chromosone consists of boolean genes
			//one for each candidate in the targetset
			Gene[] sampleGenes = new Gene[candidates.size()];
			for(int i =0;i<candidates.size();i++){
				sampleGenes[i] = new BooleanGene(conf);
			}
			Chromosome sampleChromosome = new Chromosome(conf);
			sampleChromosome.setGenes(sampleGenes);
			conf.setSampleChromosome( sampleChromosome );
			conf.setPopulationSize( 100 );
			conf.setFitnessFunction(SITFitness);
			conf.setPreservFittestIndividual(true);
		    conf.setKeepPopulationSizeConstant(false);
			
			
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
		    System.out.println("Best fitness so far:"+bestSolutionSoFar.getFitnessValue());
		}
		
		
		
		return getTargetSet(candidates, bestSolutionSoFar);
	}

	public void setMTLearner(MTLearner learner) {
		this.learner = learner;		
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

}
