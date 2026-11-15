package ultimateticktacktoe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Picks moves for the computer player. Moves are encoded as board * 9 + cell, like {@link GameState#legalMoves()}. */
public final class Computer {
   /** Hard always runs at least this many simulated games, however slow the machine. */
   private static final int MIN_PLAYOUTS = 300;
   private static final double EXPLORATION = 1.4;

   private Computer() {
   }

   /** Takes a move that wins the game or a small board when there is one, else plays at random. */
   public static int easyMove(GameState state, Random random) {
      int[] moves = state.legalMoves();
      int win = winningMove(state, moves);
      if (win >= 0) {
         return win;
      }
      List<Integer> boardWins = new ArrayList<>();
      String mark = state.xTurn ? "X" : "O";
      for (int move : moves) {
         GameState next = state.copy();
         next.play(move / 9, move % 9);
         if (mark.equals(next.boardResults[move / 9])) {
            boardWins.add(move);
         }
      }
      return boardWins.isEmpty() ? moves[random.nextInt(moves.length)] : boardWins.get(random.nextInt(boardWins.size()));
   }

   /**
    * Monte Carlo tree search: plays random games from the current position for about {@code millis} milliseconds,
    * growing a tree of the most promising lines, and returns the move it explored most.
    */
   public static int hardMove(GameState state, Random random, long millis) {
      return analyse(state, random, millis).move;
   }

   /**
    * The search behind Hard, for a game in progress: the move it explored most and how well the side to move does with
    * it, as the share of the search's games from there that it won (a draw counting half). A move that wins the game at
    * once is taken without searching, and scores 1.
    */
   public static Verdict analyse(GameState state, Random random, long millis) {
      int[] moves = state.legalMoves();
      int win = winningMove(state, moves);
      if (win >= 0) {
         return new Verdict(win, 1);
      }
      Node root = search(state, random, millis, moves);
      Node best = root.children.get(0);
      for (Node child : root.children) {
         if (child.visits > best.visits) {
            best = child;
         }
      }
      return new Verdict(best.move, best.score / best.visits);
   }

   private static Node search(GameState state, Random random, long millis, int[] moves) {
      Node root = new Node(null, -1, !state.xTurn, moves);
      grow(root, state, random, millis);
      return root;
   }

   /** Adds about {@code millis} (and at least MIN_PLAYOUTS games) of search to the tree under {@code root}, the node of {@code state}. */
   private static void grow(Node root, GameState state, Random random, long millis) {
      long deadline = System.currentTimeMillis() + millis;
      for (int playouts = 0; playouts < MIN_PLAYOUTS || System.currentTimeMillis() < deadline; playouts++) {
         GameState game = state.copy();
         Node node = root;
         while (node.untried == 0 && !node.children.isEmpty()) {
            node = node.bestChild();
            game.play(node.move / 9, node.move % 9);
         }
         if (node.untried > 0) {
            int pick = random.nextInt(node.untried);
            int move = node.moves[pick];
            node.moves[pick] = node.moves[node.untried - 1];
            node.moves[--node.untried] = move;
            boolean xMoved = game.xTurn;
            game.play(move / 9, move % 9);
            Node child = new Node(node, move, xMoved, game.legalMoves());
            node.children.add(child);
            node = child;
         }
         while (game.winner == null) {
            int[] next = game.legalMoves();
            int move = next[random.nextInt(next.length)];
            game.play(move / 9, move % 9);
         }
         for (; node != null; node = node.parent) {
            node.visits++;
            if (GameState.DRAW.equals(game.winner)) {
               node.score += 0.5;
            } else if (game.winner.equals(node.xMoved ? "X" : "O")) {
               node.score += 1;
            }
         }
      }
   }

   /**
    * Hints: Hard's search on one position, grown a little at a time so the board can show it settle, with every legal
    * move's chances for the side to move (the share of the search's games after it that the mover won, a draw half).
    */
   static final class Think {
      private static final int PRIOR_GAMES = 10;
      private final GameState state;
      private final Node root;

      Think(GameState state) {
         this.state = state.copy();
         this.root = new Node(null, -1, !state.xTurn, state.legalMoves());
      }

      void grow(Random random, long millis) {
         Computer.grow(this.root, this.state, random, millis);
      }

      /**
       * chances[move] for each legal move searched so far; NaN for the rest. Each starts from even, as if PRIOR_GAMES
       * were drawn, so a move the search tried only a few times does not show as a blunder (or a sure win) by chance.
       */
      double[] chances() {
         double[] chances = new double[81];
         java.util.Arrays.fill(chances, Double.NaN);
         for (Node child : this.root.children) {
            chances[child.move] = (child.score + 0.5 * PRIOR_GAMES) / (child.visits + PRIOR_GAMES);
         }
         return chances;
      }

      /**
       * How far each move's chances may be off, from how few games it got: two standard errors; NaN where chances() is.
       * A hint's grade counts only the part of a move's shortfall beyond this, so noise is not called a mistake.
       */
      double[] doubts() {
         double[] chances = this.chances();
         double[] doubts = new double[81];
         java.util.Arrays.fill(doubts, Double.NaN);
         for (Node child : this.root.children) {
            double p = chances[child.move];
            doubts[child.move] = 2 * Math.sqrt(p * (1 - p) / (child.visits + PRIOR_GAMES));
         }
         return doubts;
      }

      /** The move Hard would play: the one searched most; -1 before any search. */
      int best() {
         Node best = null;
         for (Node child : this.root.children) {
            if (best == null || child.visits > best.visits) {
               best = child;
            }
         }
         return best == null ? -1 : best.move;
      }
   }

   /** What a search found: its move, and the side to move's score with it (0 to 1). */
   public static final class Verdict {
      public final int move;
      public final double score;

      Verdict(int move, double score) {
         this.move = move;
         this.score = score;
      }
   }

   /** A move that wins the whole game right away, or -1. */
   static int winningMove(GameState state, int[] moves) {
      for (int move : moves) {
         GameState next = state.copy();
         next.play(move / 9, move % 9);
         if (next.winner != null && !GameState.DRAW.equals(next.winner)) {
            return move;
         }
      }
      return -1;
   }

   /** One position in the search tree, reached by {@code move}. */
   private static final class Node {
      final Node parent;
      final int move;
      /** Whether X made the move into this node; its score counts wins for that player. */
      final boolean xMoved;
      /** Moves from here; the first {@code untried} have no child yet. */
      final int[] moves;
      int untried;
      final List<Node> children = new ArrayList<>();
      int visits;
      double score;

      Node(Node parent, int move, boolean xMoved, int[] moves) {
         this.parent = parent;
         this.move = move;
         this.xMoved = xMoved;
         this.moves = moves;
         this.untried = moves.length;
      }

      /** The child with the best upper confidence bound (UCT): good results so far, plus a bonus for few visits. */
      Node bestChild() {
         Node best = null;
         double bestValue = Double.NEGATIVE_INFINITY;
         double logVisits = Math.log(this.visits);
         for (Node child : this.children) {
            double value = child.score / child.visits + EXPLORATION * Math.sqrt(logVisits / child.visits);
            if (value > bestValue) {
               bestValue = value;
               best = child;
            }
         }
         return best;
      }
   }
}
