package minesweeperjs;

/**
 * Utility class for common operations used in the Minesweeper game.
 * Includes methods for coordinate transformations and distance calculations.
 *
 * @author Joel Santos
 * @version 1.0
 * @since 12/01/2023
 */
public class Utils {

    /**
     * Converts a 1D array index to 2D grid coordinates.
     *
     * @param index The index in the 1D array.
     * @param width The width of the grid.
     * @return An array containing the 2D coordinates (x, y).
     */
    public static int[] convertIndexToCoordinates(int index, int width) {
        int x = index % width; // Calculate x-coordinate
        int y = index / width; // Calculate y-coordinate
        return new int[] { x, y };
    }

    /**
     * Converts 2D grid coordinates to a 1D array index.
     *
     * @param x     The x-coordinate on the grid.
     * @param y     The y-coordinate on the grid.
     * @param width The width of the grid.
     * @return The corresponding index in the 1D array.
     */
    public static int convertCoordinatesToIndex(int x, int y, int width) {
        return y * width + x;
    }

    /**
     * Calculates the Euclidean distance between two points on the grid.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @return The distance between the two points.
     */
    public static double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)); // Euclidean distance formula
    }
}
