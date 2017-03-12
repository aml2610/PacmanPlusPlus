package main.java.graphics;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import main.java.constants.GameOutcome;
import main.java.constants.GameOutcomeType;
import main.java.constants.ScreenSize;
import main.java.event.arguments.GameDisplayInvalidatedEventArgs;
import main.java.event.arguments.GameEndedEventArgs;
import main.java.event.listener.GameDisplayInvalidatedListener;
import main.java.event.listener.GameEndedListener;
import main.java.gamelogic.core.GameLogic;
import main.java.gamelogic.domain.Cell;
import main.java.gamelogic.domain.ControlledPlayer;
import main.java.gamelogic.domain.Game;
import main.java.gamelogic.domain.Ghost;
import main.java.gamelogic.domain.Player;
import main.java.ui.GameUI;

/**
 * Created by Boyan Bonev on 09/02/2017.
 */
public class Render implements GameDisplayInvalidatedListener, GameEndedListener {
	private Pane root;
	private Timeline timeLine;
	private ControlledPlayer controlledPlayer;
	private GameUI gameUI;
	private Game game;
	private GameLogic gameLogic;
	private ArrayList<Node> allEntities;
	private HashMap<String, RotateTransition> rotations;
	private HashMap<String, TranslateTransition> transitions;
	private boolean flag;

	/**
	 * Initialize new visualisation of the map
	 *
	 * @param gameUI
	 * @param game
	 */
	public Render(final GameUI gameUI, final Game game, final GameLogic gameLogic) {
		this.gameUI = gameUI;
		this.game = game;
		this.gameLogic = gameLogic;
		this.gameLogic.getOnGameDisplayInvalidated().addListener(this);
		this.gameLogic.getOnGameEnded().addListener(this);
		flag = false;

		transitions = new HashMap<>();
		rotations = new HashMap<>();
		allEntities = new ArrayList<>();
		controlledPlayer = game.getPlayer();
	}

	/**
	 * Generate the map
	 *
	 * @return the stage that contians the scene with the map
	 */
	public Scene drawWorld() {
		final Cell[][] cells = game.getWorld().getMap().getCells();

		root = new Pane();
		root.setStyle("-fx-background-color: black");

		final Scene scene = new Scene(root, ScreenSize.Width, ScreenSize.Height);

		addToRoot(cells);

		for (final Player player : game.getWorld().getPlayers()) {
			final Node playerNode = new PacmanVisualisation(player).getNode();
			final TranslateTransition transitionPlayer = new TranslateTransition(Duration.millis(140), playerNode);
			final RotateTransition rotatePlayer = new RotateTransition(Duration.millis(30), playerNode);

			transitions.put(player.getName(), transitionPlayer);
			rotations.put(player.getName(), rotatePlayer);

			allEntities.add(playerNode);
			root.getChildren().add(playerNode);
		}

		for (final Ghost ghost : game.getWorld().getGhosts()) {
			final Node ghostNode = new GhostVisualisation(ghost.getPosition()).getNode();
			final TranslateTransition transitionGhost = new TranslateTransition(Duration.millis(140), ghostNode);

			transitions.put(Integer.toString(ghost.getID()), transitionGhost);

			allEntities.add(ghostNode);

			root.getChildren().add(ghostNode);
		}

		return scene;
	}

	/**
	 * Redraw the map
	 */
	public void redrawWorld() {
		final Cell[][] cells = game.getWorld().getMap().getCells();
		PositionVisualisation.initScreenDimensions();

		clearRoot();
		addToRoot(cells);

		for (final Player player : game.getWorld().getPlayers()) {
			final ImageView nextNode = new PacmanVisualisation(player).getNode();

			transitions.get(player.getName()).setToY(nextNode.getTranslateY());
			transitions.get(player.getName()).setToX(nextNode.getTranslateX());
			rotations.get(player.getName()).setToAngle(nextNode.getRotate());

			root.getChildren().get(root.getChildren().indexOf(transitions.get(player.getName()).getNode())).toFront();

			rotations.get(player.getName()).play();
			transitions.get(player.getName()).play();
		}

		for (final Ghost ghost : game.getWorld().getGhosts()) {
			final ImageView nextNode = new GhostVisualisation(ghost.getPosition()).getNode();

			transitions.get(Integer.toString(ghost.getID())).setToY(nextNode.getTranslateY());
			transitions.get(Integer.toString(ghost.getID())).setToX(nextNode.getTranslateX());

			root.getChildren()
					.get(root.getChildren().indexOf(transitions.get(Integer.toString(ghost.getID())).getNode()))
					.toFront();

			transitions.get(Integer.toString(ghost.getID())).play();
		}

		root.requestFocus();
	}

	/**
	 *
	 * Click listener for moving the player
	 */
	public void addClickListener() {
		root.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.UP) {
				controlledPlayer.moveUp();
				redrawWorld();
			} else if (event.getCode() == KeyCode.DOWN) {
				controlledPlayer.moveDown();
				redrawWorld();
			} else if (event.getCode() == KeyCode.LEFT) {
				controlledPlayer.moveLeft();
				redrawWorld();
			} else if (event.getCode() == KeyCode.RIGHT) {
				controlledPlayer.moveRight();
				redrawWorld();
			}
		});
	}

	/**
	 * Start the timeline
	 */
	public void startTimeline() {
		timeLine = new Timeline(new KeyFrame(Duration.millis(200), event -> {
			gameLogic.gameStep(200);
		}));
		timeLine.setCycleCount(Timeline.INDEFINITE);
		timeLine.play();
	}

	/**
	 * Add all nodes to parent root
	 *
	 * @param cells
	 */
	private void addToRoot(final Cell[][] cells) {
		for (final Cell[] cell : cells) {
			for (final Cell c : cell) {
				final Node cv = new CellVisualisation(c).getNode();
				root.getChildren().add(cv);
			}
		}
	}

	/**
	 * Remove nodes that have been affected (like tokens)
	 */
	private void clearRoot() {
		final int countOfNodes = root.getChildren().size();
		for (int index = 0; index < countOfNodes; index++) {
			if (index < root.getChildren().size()) {
				if (!allEntities.contains(root.getChildren().get(index))) {
					root.getChildren().remove(index);
				}
			}
		}
	}

	private StackPane gameEndedWindow(final GameOutcome gameOutcome) {
		final StackPane pane = new StackPane();
		pane.setStyle("-fx-background-color: rgba(0, 100, 100, 0.6)");
		pane.setPrefSize(ScreenSize.Width, ScreenSize.Height);

		final Label outcomneLabel = new Label(
				gameOutcome.getOutcomeType() == GameOutcomeType.GHOSTS_WON ? "Damn! You lost!" : "Wohoo, You won!");
		outcomneLabel.setStyle(
				"-fx-text-fill: goldenrod; -fx-font: bold 30 \"serif\"; -fx-padding: 20 0 0 0; -fx-text-alignment: center");

		final Label escLable = new Label("* Press ESC to go back at the menu");
		escLable.setStyle(
				"-fx-text-fill: goldenrod; -fx-font: bold 20 \"serif\"; -fx-padding: 0 0 0 0; -fx-text-alignment: center");

		final Label spaceLabel = new Label("* Press SPACE to reply");
		spaceLabel.setStyle(
				"-fx-text-fill: goldenrod; -fx-font: bold 20 \"serif\"; -fx-padding: 50 103 0 0; -fx-text-alignment: center");
		StackPane.setAlignment(outcomneLabel, Pos.TOP_CENTER);
		StackPane.setAlignment(escLable, Pos.CENTER);
		StackPane.setAlignment(spaceLabel, Pos.CENTER);

		pane.getChildren().addAll(outcomneLabel, escLable, spaceLabel);
		return pane;
	}

	/**
	 * End the game
	 */
	private void gameEnded(final GameOutcome gameOutcome) {
		timeLine.stop();

		root.getChildren().add(gameEndedWindow(gameOutcome));
		root.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.SPACE) {
				addClickListener();
				startTimeline();
				redrawWorld();
			} else if (e.getCode() == KeyCode.ESCAPE) {
				gameUI.switchToMenu();
			}
		});
	}

	@Override
	public void onGameDisplayInvalidated(final GameDisplayInvalidatedEventArgs args) {
		if (!game.hasEnded()) {
			Platform.runLater(this::redrawWorld);
		}
	}

	@Override
	public void onGameEnded(final GameEndedEventArgs args) {
		gameEnded(args.getOutcome());
	}
}
