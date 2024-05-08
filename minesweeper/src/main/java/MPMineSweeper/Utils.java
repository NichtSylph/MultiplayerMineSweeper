package MPMineSweeper;

public class Utils {

    public static int[] convertIndexToCoordinates(int index, int width) {
        int x = index % width; // Calculate x-coordinate
        int y = index / width; // Calculate y-coordinate
        return new int[] { x, y };
    }

    public static int convertCoordinatesToIndex(int x, int y, int width) {
        return y * width + x;
    }

    public static double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)); // Euclidean distance formula
    }
}
