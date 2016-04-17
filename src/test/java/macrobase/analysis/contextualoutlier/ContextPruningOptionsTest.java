package macrobase.analysis.contextualoutlier;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import macrobase.conf.MacroBaseDefaults;


public class ContextPruningOptionsTest {

	
	@Test
	public void contextPruningOptionsTest1(){
		ContextPruningOptions contextPruningOptions = new ContextPruningOptions(MacroBaseDefaults.CONTEXTUAL_PRUNING);
		assertEquals(contextPruningOptions.isDensityPruning(),true);
		assertEquals(contextPruningOptions.isDependencyPruning(),true);
		assertEquals(contextPruningOptions.isDistributionPruningForTraining(),true);
		assertEquals(contextPruningOptions.isDistributionPruningForScoring(),true);

	}
	@Test
	public void contextPruningOptionsTest2(){
		ContextPruningOptions contextPruningOptions = new ContextPruningOptions("densityPruningEnabled_dependencyPruningEnabled_distributionPruningForTrainingEnabled_distributionPruningForScoringEnabled");
		assertEquals(contextPruningOptions.isDensityPruning(),true);
		assertEquals(contextPruningOptions.isDependencyPruning(),true);
		assertEquals(contextPruningOptions.isDistributionPruningForTraining(),true);
		assertEquals(contextPruningOptions.isDistributionPruningForScoring(),true);

	}

}
