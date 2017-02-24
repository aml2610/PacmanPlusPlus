package teamproject.graphics;

import java.util.HashMap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import teamproject.ai.AIGhost;
import teamproject.constants.GameOutcome;
import teamproject.constants.GameType;
import teamproject.constants.Images;
import teamproject.constants.ScreenSize;
import teamproject.event.arguments.EntityMovedEventArgs;
import teamproject.event.listener.LocalEntityUpdatedListener;
import teamproject.gamelogic.domain.*;
import teamproject.ui.GameUI;

/**
 * Created by Boyan Bonev on 09/02/2017.
 */
public class Render implements LocalEntityUpdatedListener {
	private Pane root;
	private Timeline timeLine;
	private ControlledPlayer controlledPlayer;
	private GameUI gameUI;
	private World world;
	private boolean serverMode;
	private Game game;

	private EntityMovement moveControlledPlayer;
	private HashMap<Entity, EntityMovement> ghostMovements;

	/**
	 * Initialize new visualisation of the map
	 *
	 * @param gameUI
	 * @param game
	 * @param serverMode
	 */
	public Render(final GameUI gameUI, final Game game, final boolean serverMode) {
		this.gameUI = gameUI;
		this.world = game.getWorld();
		this.serverMode = serverMode;
		this.game = game;

		controlledPlayer = game.getPlayer();
		moveControlledPlayer = new EntityMovement(controlledPlayer, world);

		ghostMovements = new HashMap<>();
		for (final AIGhost ghost : world.getGhosts()) {
			ghostMovements.put(ghost, new EntityMovement(ghost, world));
		}
	}

	/**
	 * Generate the map
	 *
	 * @return the stage that contians the scene with the map
	 */
	public Scene drawMap() {
		final Cell[][] cells = world.getMap().getCells();
		Images.Border = new ImageView("border.jpg");
		root = new Pane();
		root.setStyle("-fx-background-color: black");

		final Scene scene = new Scene(root, ScreenSize.Width, ScreenSize.Height);

		addToRoot(root, cells);

		return scene;
	}

	/**
	 * Redraw the map
	 */
	public void redrawMap() {
		final Cell[][] cells = world.getMap().getCells();
		PositionVisualisation.initScreenDimensions();

		root.getChildren().clear();

		addToRoot(root, cells);

		for (final Player player : world.getPlayers()) {
			root.getChildren().add(new PacmanVisualisation(player).getNode());
		}

		for (final Ghost ghost : world.getGhosts()) {
			root.getChildren().add(new GhostVisualisation(ghost, controlledPlayer).getNode());
		}

		root.requestFocus();
	}

	/**
	 *
	 * Click listener for moving the ghost
	 */
	public void addClickListener() {
		root.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.UP) {
				moveControlledPlayer.moveUp();
				redrawMap();
			} else if (event.getCode() == KeyCode.DOWN) {
				moveControlledPlayer.moveDown();
				redrawMap();
			} else if (event.getCode() == KeyCode.LEFT) {
				moveControlledPlayer.moveLeft();
				redrawMap();
			} else if (event.getCode() == KeyCode.RIGHT) {
				moveControlledPlayer.moveRight();
				redrawMap();
			}
		});
	}

	/**
	 * Start the timeline
	 */
	public void startTimeline() {
		for (final AIGhost ghost : world.getGhosts()) {
			ghost.getBehaviour().getOnMovedEvent().addListener(this);
		}

		timeLine = new Timeline(new KeyFrame(Duration.millis(250), event -> {
			if (serverMode) {

			}

            for (final AIGhost ghost : world.getGhosts()) {
                ghost.run();
            }
		}));
		timeLine.setCycleCount(Timeline.INDEFINITE);
		timeLine.play();
	}

	@Override
	public void onEntityMoved(final EntityMovedEventArgs args) {
		ghostMovements.get(args.getEntity()).moveTo(args.getRow(), args.getCol(), args.getAngle());
		redrawMap();

        if(RuleChecker.getGameOutcome(game, serverMode ? GameType.MULTIPLAYER : GameType.SINGLEPLAYER)
                == GameOutcome.LOCAL_PLAYER_LOST){
            gameEnded();
        }
	}

    /**
     * Add to parent root all nodes
     * @param root
     * @param cells
     */
    private void addToRoot(final Pane root, final Cell[][] cells) {
        for (final Cell[] cell : cells) {
            for (final Cell c : cell) {
                final CellVisualisation cv = new CellVisualisation(c);
                System.out.println("State: " + c.getState().toString());
                root.getChildren().add(cv.getNode());
            }
        }
    }

    /**
     * End the game
     */
    private void gameEnded() {
        timeLine.stop();
        gameUI.switchToMenu();
        root.setOnKeyPressed(e -> {
        });
    }
}
