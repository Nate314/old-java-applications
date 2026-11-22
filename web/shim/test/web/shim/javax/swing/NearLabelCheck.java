package web.shim.javax.swing;

/** Checks which label names a field (NearLabel): left in its row or above in its column, others in the window close by. */
public class NearLabelCheck {
    public static void main(String[] args) {
        int[] field = {100, 50, 200, 20};
        boolean[] mine = {true, true};
        boolean[] others = {false, false};
        // GroupCreator on a phone: "Names, separated by commas:" in a row panel over the field's panel, 10 px up.
        assert NearLabel.pick(field, new int[][] {{5, 20, 180, 20}}, others) == 0 : "a label row just above names the field";
        assert NearLabel.pick(field, new int[][] {{5, 0, 180, 20}}, others) == -1 : "a label elsewhere 30 px up is too far";
        assert NearLabel.pick(field, new int[][] {{5, 0, 180, 20}}, new boolean[] {true}) == 0 : "a label in the field's own panel may be far";
        // CircleSpawner: the color box has "Size" on its right, which names the next field, not this one.
        assert NearLabel.pick(field, new int[][] {{310, 50, 30, 20}}, mine) == -1 : "a label on the right does not name it";
        assert NearLabel.pick(field, new int[][] {{0, 50, 40, 20}, {60, 50, 38, 20}}, mine) == 1 : "the nearer of two labels on the left";
        assert NearLabel.pick(field, new int[][] {{0, 50, 40, 20}, {60, 50, 38, 20}}, others) == 1 : "the nearer, 2 px away, is close enough";
        assert NearLabel.pick(field, new int[][] {{0, 0, 40, 20}, {60, 50, 41, 20}}, mine) == 1 : "an edge overlapping by a pixel still counts";
        assert NearLabel.pick(field, new int[][] {{0, 90, 99, 20}}, mine) == -1 : "a label below does not name it";
        assert NearLabel.pick(field, new int[0][], new boolean[0]) == -1;
        System.out.println("NearLabelCheck passed");
    }
}
