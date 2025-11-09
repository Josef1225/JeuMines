package mines;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.SecureRandom;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Board extends JPanel {
    private static final long serialVersionUID = 6195235521361212179L;

    // Constants (all caps with underscores)
    private static final int NUM_IMAGES = 13;
    private static final int CELL_SIZE = 15;

    private static final int COVER_FOR_CELL = 10;
    private static final int MARK_FOR_CELL = 10;
    private static final int EMPTY_CELL = 0;
    private static final int MINE_CELL = 9;
    private static final int COVERED_MINE_CELL = MINE_CELL + COVER_FOR_CELL;
    private static final int MARKED_MINE_CELL = COVERED_MINE_CELL + MARK_FOR_CELL;

    private static final int DRAW_MINE = 9;
    private static final int DRAW_COVER = 10;
    private static final int DRAW_MARK = 11;
    private static final int DRAW_WRONG_MARK = 12;

    int[] field;
    private boolean inGame;
    private int minesLeft;
    private transient Image[] img;
    private int mines = 40;
    private int rows = 16;
    private int cols = 16;
    private int allCells;
    private JLabel statusbar;

    public Board(JLabel statusbar) {
        this.statusbar = statusbar;

        img = new Image[NUM_IMAGES];
        for (int i = 0; i < NUM_IMAGES; i++) {
            String path = "images/" + i + ".png";
            img[i] = new ImageIcon(path).getImage();
        }

        setDoubleBuffered(true);
        addMouseListener(new MinesAdapter());
        newGame();
    }

    public void newGame() {
        SecureRandom random = new SecureRandom();
        inGame = true;
        minesLeft = mines;
        allCells = rows * cols;
        field = new int[allCells];

        for (int i = 0; i < allCells; i++) {
            field[i] = COVER_FOR_CELL;
        }

        statusbar.setText(Integer.toString(minesLeft));

        int i = 0;
        while (i < mines) {
            int position = (int) (allCells * random.nextDouble());

            if (position < allCells && field[position] != COVERED_MINE_CELL) {
                field[position] = COVERED_MINE_CELL;
                i++;

                // Update neighboring cells
                for (int r = -1; r <= 1; r++) {
                    for (int c = -1; c <= 1; c++) {
                        if (r == 0 && c == 0) continue;
                        int cell = position + r * cols + c;
                        int cellCol = cell % cols;

                        if (cell >= 0 && cell < allCells &&
                            cellCol >= 0 && cellCol < cols &&
                            field[cell] != COVERED_MINE_CELL) {
                            field[cell] += 1;
                        }
                    }
                }
            }
        }
    }

    public void findEmptyCells(int j) {
        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                if (r == 0 && c == 0) continue;
                int cell = j + r * cols + c;
                int cellCol = cell % cols;

                if (cell >= 0 && cell < allCells &&
                    cellCol >= 0 && cellCol < cols &&
                    field[cell] > MINE_CELL) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        findEmptyCells(cell);
                    }
                }
            }
        }
    }


    @Override
    public void paint(Graphics g) {
        int uncover = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int cell = field[(i * cols) + j];

                if (inGame && cell == MINE_CELL)
                    inGame = false;

                if (!inGame) {
                    if (cell == COVERED_MINE_CELL) {
                        cell = DRAW_MINE;
                    } else if (cell == MARKED_MINE_CELL) {
                        cell = DRAW_MARK;
                    } else if (cell > COVERED_MINE_CELL) {
                        cell = DRAW_WRONG_MARK;
                    } else if (cell > MINE_CELL) {
                        cell = DRAW_COVER;
                    }
                } else {
                    if (cell > COVERED_MINE_CELL) {
                        cell = DRAW_MARK;
                    } else if (cell > MINE_CELL) {
                        cell = DRAW_COVER;
                        uncover++;
                    }
                }

                g.drawImage(img[cell], j * CELL_SIZE, i * CELL_SIZE, this);
            }
        }

        if (uncover == 0 && inGame) {
            inGame = false;
            statusbar.setText("Game won");
        } else if (!inGame) {
            statusbar.setText("Game lost");
        }
    }

    class MinesAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int cCol = x / CELL_SIZE;
            int cRow = y / CELL_SIZE;
            boolean repaintNeeded = false;

            if (!inGame) {
                newGame();
                repaint();
            }

            if (x < cols * CELL_SIZE && y < rows * CELL_SIZE) {
                int cellIndex = cRow * cols + cCol;

                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (field[cellIndex] > MINE_CELL) {
                        repaintNeeded = true;

                        if (field[cellIndex] <= COVERED_MINE_CELL) {
                            if (minesLeft > 0) {
                                field[cellIndex] += MARK_FOR_CELL;
                                minesLeft--;
                                statusbar.setText(Integer.toString(minesLeft));
                            } else {
                                statusbar.setText("No marks left");
                            }
                        } else {
                            field[cellIndex] -= MARK_FOR_CELL;
                            minesLeft++;
                            statusbar.setText(Integer.toString(minesLeft));
                        }
                    }
                } else {
                    if (field[cellIndex] > COVERED_MINE_CELL) return;

                    if (field[cellIndex] > MINE_CELL && field[cellIndex] < MARKED_MINE_CELL) {
                        field[cellIndex] -= COVER_FOR_CELL;
                        repaintNeeded = true;

                        if (field[cellIndex] == MINE_CELL) inGame = false;
                        if (field[cellIndex] == EMPTY_CELL) findEmptyCells(cellIndex);
                    }
                }

                if (repaintNeeded) repaint();
            }
        }
    }
}
