package test.java.graphics;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import main.java.constants.CellState;
import main.java.constants.Colors;
import main.java.gamelogic.domain.Cell;
import main.java.graphics.CellVisualisation;
import main.java.graphics.PositionVisualisation;

/**
 * Created by boyanbonev on 05/02/2017.
 */
public class CellVisualisationTest {
	private PositionVisualisation testPositionVisualisation;
	private Cell testCell;
	private CellVisualisation test;

	@Before
	public void initializeFields() {
		testPositionVisualisation = new PositionVisualisation(5, 5);
		testCell = new Cell(CellState.EMPTY, testPositionVisualisation);
		test = new CellVisualisation(testCell);
	}

	@Test
	public void testCell() {
		assertEquals(test.getCell(), testCell);
	}

	@Test
	public void testRectangleNode() {
		assertEquals(test.getNode().toString().contains("Rectangle"), true);

		final Rectangle testRectangle = (Rectangle) test.getNode();

		assertEquals(testRectangle.getX(), 0, 0.0001);
		assertEquals(testRectangle.getY(), 0, 0.0001);
		assertEquals(testRectangle.getHeight(), 0, 0.0001);
		assertEquals(testRectangle.getWidth(), 0, 0.0001);
		assertEquals(testRectangle.getFill(), Colors.CellEmptyColor);
	}

	@Test
	public void testCircleNode() {
		test = new CellVisualisation(new Cell(CellState.FOOD, testPositionVisualisation));
		assertEquals(test.getNode().toString().contains("Circle"), true);

		final Circle testCircle = (Circle) test.getNode();

		assertEquals(testCircle.getFill(), Colors.CellFoodColor);
		assertEquals(testCircle.getCenterX(), 0, 0.0001);
		assertEquals(testCircle.getCenterY(), 0, 0.0001);
		assertEquals(testCircle.getRadius(), 0, 0.0001);
	}
}