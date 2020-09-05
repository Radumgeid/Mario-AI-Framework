import engine.core.MarioGame;
import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import engine.core.MarioTimer;

public class GenerateLevel {
    public static void printResults(MarioResult result) {
	System.out.println("****************************************************************");
	System.out.println("Game Status: " + result.getGameStatus().toString() + 
		" Percentage Completion: " + result.getCompletionPercentage());
	System.out.println("Lives: " + result.getCurrentLives() + " Coins: " + result.getCurrentCoins() + 
		" Remaining Time: " + (int)Math.ceil(result.getRemainingTime() / 1000f)); 
	System.out.println("Mario State: " + result.getMarioMode() +
		" (Mushrooms: " + result.getNumCollectedMushrooms() + " Fire Flowers: " + result.getNumCollectedFireflower() + ")");
	System.out.println("Total Kills: " + result.getKillsTotal() + " (Stomps: " + result.getKillsByStomp() + 
		" Fireballs: " + result.getKillsByFire() + " Shells: " + result.getKillsByShell() + 
		" Falls: " + result.getKillsByFall() + ")");
	System.out.println("Bricks: " + result.getNumDestroyedBricks() + " Jumps: " + result.getNumJumps() + 
		" Max X Jump: " + result.getMaxXJump() + " Max Air Time: " + result.getMaxJumpAirTime());
	System.out.println("****************************************************************");
    }
    
    public static void main(String[] args) {
		MarioLevelGenerator generator = new levelGenerators.sampler.LevelGenerator();
		String level = generator.getGeneratedLevel(new MarioLevelModel(150, 16), new MarioTimer(5*60*60*1000));
		MarioGame game = new MarioGame();
		// printResults(game.playGame(level, 200, 0));
		printResults(game.runGame(new agents.diego.Agent(), level, 20, 0, true));
		
		/*
		//Estudio comparativo
		double completationDiego = 0;
		double completationAndy = 0;
		double completationGlenn = 0;
		double completationRobin = 0;
		double completationSergey = 0;
		double completationTrond = 0;
		for(int i = 0; i<10; i++){
			level = generator.getGeneratedLevel(new MarioLevelModel(100, 16), new MarioTimer(5*60*60*1000));
			MarioGame game = new MarioGame();
			completationDiego += game.runGame(new agents.diego.Agent(), level, 30, 0, true).getCompletionPercentage();
			completationAndy += game.runGame(new agents.andySloane.Agent(), level, 30, 0, true).getCompletionPercentage();
			completationGlenn += game.runGame(new agents.glennHartmann.Agent(), level, 30, 0, true).getCompletionPercentage();
			completationRobin += game.runGame(new agents.robinBaumgarten.Agent(), level, 30, 0, true).getCompletionPercentage();
			completationSergey += game.runGame(new agents.sergeyKarakovskiy.Agent(), level, 30, 0, true).getCompletionPercentage();
			completationTrond += game.runGame(new agents.trondEllingsen.Agent(), level, 30, 0, true).getCompletionPercentage();
		}
		System.out.println("Suma de porcentaje de completacion Diego:" + completationDiego);
		System.out.println("Suma de porcentaje de completacion Andy:" + completationAndy);
		System.out.println("Suma de porcentaje de completacion Glenn:" + completationGlenn);
		System.out.println("Suma de porcentaje de completacion Robin:" + completationRobin);
		System.out.println("Suma de porcentaje de completacion Sergey:" + completationSergey);
		System.out.println("Suma de porcentaje de completacion Trond:" + completationTrond);
		*/
    }
}
