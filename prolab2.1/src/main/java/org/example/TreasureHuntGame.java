package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class TreasureHuntGame extends JPanel implements ActionListener {
    private final int gridSize;
    private final int cellSize = 30;
    private final int delay = 300;
    private Timer timer;

    private String playerName;
    private JLabel playerNameLabel;
    private JLabel stepsLabel;
    private JLabel collectedTreasuresLabel;

    private int[][] map;
    private ArrayList<Treasure> treasures;
    private Point playerPosition;
    private ArrayList<HorizontalMovingObstacle> horizontalMovingObstacles;
    private ArrayList<VerticalMovingObstacle> verticalMovingObstacles;
    private boolean gameRunning;
    private Point currentTarget;
    private ArrayList<Point> playerPath;
    private ArrayList<Treasure> collectedTreasures;

    private boolean showFullMap = true;
    private ArrayList<String> discovered;

    public TreasureHuntGame(int x, String playerName) {
        this.playerName = playerName;
        playerNameLabel = new JLabel("Player: " + playerName);
        playerNameLabel.setForeground(Color.BLACK);
        add(playerNameLabel);

        stepsLabel = new JLabel("Steps: 0");
        stepsLabel.setForeground(Color.BLACK);
        add(stepsLabel);

        collectedTreasuresLabel = new JLabel("Collected Treasures: ");
        collectedTreasuresLabel.setForeground(Color.BLACK);
        add(collectedTreasuresLabel);

        gridSize = x;
        map = new int[gridSize][gridSize];
        treasures = new ArrayList<>();
        playerPosition = new Point(0, 0);
        horizontalMovingObstacles = new ArrayList<>();
        verticalMovingObstacles = new ArrayList<>();
        gameRunning = false;
        currentTarget = null;
        playerPath = new ArrayList<>();
        collectedTreasures = new ArrayList<>();

        generateMap();
        placeTreasures();
        placeMovingObstacles();

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> startGame());
        add(startButton);

        JButton newMapButton = new JButton("New Map");
        newMapButton.addActionListener(e -> {
            showFullMap = true;
            generateMap();
            placeTreasures();
            placeMovingObstacles();
            gameRunning = false;
            playerPath.clear();
            collectedTreasures.clear();
            stepsLabel.setText("Steps: 0");
            collectedTreasuresLabel.setText("Collected Treasures: ");
            repaint();
        });
        add(newMapButton);

        timer = new Timer(delay, this);
    }

    private void generateMap() {
        Random rand = new Random();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                map[i][j] = 0;
            }
        }
        int numObstacles = gridSize * gridSize / 5;
        for (int i = 0; i < numObstacles; i++) {
            int x = rand.nextInt(gridSize);
            int y = rand.nextInt(gridSize);
            map[x][y] = rand.nextInt(4) + 1; // 1-4 arası rastgele bir sayı atıyoruz

            // Başlangıç noktasını engel olmayan bir yere koy
            do {
                playerPosition.x = rand.nextInt(gridSize);
                playerPosition.y = rand.nextInt(gridSize);
            } while (map[playerPosition.x][playerPosition.y] != 0);

        }
        // Önceki oyundan kalan trail izlerini temizle
        for (HorizontalMovingObstacle obstacle : horizontalMovingObstacles) {
            obstacle.clearTrail();
        }

        for (VerticalMovingObstacle obstacle : verticalMovingObstacles) {
            obstacle.clearTrail();
        }

    }

    private void placeTreasures() {
        Random rand = new Random();
        int numTreasures = gridSize / 2;

        for (int i = 0; i < numTreasures; i++) {
            int x, y;
            do {
                x = rand.nextInt(gridSize);
                y = rand.nextInt(gridSize);
            } while (map[x][y] == 1 || isObstacleSurrounded(x, y));

            int treasureType = rand.nextInt(4); // 0: gold, 1: silver, 2: emerald, 3: copper
            treasures.add(new Treasure(x, y, treasureType));
        }
    }

    private boolean isObstacleSurrounded(int x, int y) {
        if (x > 0 && map[x - 1][y] == 0) {
            return false;
        }
        if (x < gridSize - 1 && map[x + 1][y] == 0) {
            return false;
        }
        if (y > 0 && map[x][y - 1] == 0) {
            return false;
        }
        if (y < gridSize - 1 && map[x][y + 1] == 0) {
            return false;
        }
        return true;
    }

    private void placeMovingObstacles() {
        Random rand = new Random();
        horizontalMovingObstacles.clear();
        verticalMovingObstacles.clear();


        // Horizontal Moving Obstacles
        for (int i = 0; i < 5; i++) {
            int startX, startY;
            do {
                startX = rand.nextInt(gridSize - 5);
                startY = rand.nextInt(gridSize);
            } while (map[startX][startY] == 1);

            horizontalMovingObstacles.add(new HorizontalMovingObstacle(startX, startY));
        }

        // Vertical Moving Obstacles
        for (int i = 0; i < 3; i++) {
            int startX, startY;
            do {
                startX = rand.nextInt(gridSize);
                startY = rand.nextInt(gridSize - 3);
            } while (map[startX][startY] == 1);

            verticalMovingObstacles.add(new VerticalMovingObstacle(startX, startY));
        }
    }

    private void moveMovingObstacles() {
        for (HorizontalMovingObstacle obstacle : horizontalMovingObstacles) {
            obstacle.move(map);
        }

        for (VerticalMovingObstacle obstacle : verticalMovingObstacles) {
            obstacle.move(map);
        }
    }

    private Point getNextMove(Point start, Point end) {
        Queue<AStarNode> open = new LinkedList<>();
        ArrayList<AStarNode> closed = new ArrayList<>();
        open.add(new AStarNode(null, start, 0, heuristic(start, end)));

        while (!open.isEmpty()) {
            AStarNode current = open.poll();
            closed.add(current);

            if (current.position.equals(end)) {
                while (current.parent != null && !current.parent.position.equals(start)) {
                    current = current.parent;
                }
                return current.position;
            }

            for (Point neighbor : getNeighbors(current.position)) {
                int cost = current.g + 1;
                int heuristic = heuristic(neighbor, end);
                AStarNode node = new AStarNode(current, neighbor, cost, heuristic);

                if (!containsPosition(open, neighbor) && !containsPosition(closed, neighbor) && map[neighbor.x][neighbor.y] == 0) {
                    open.add(node);
                } else if (containsPosition(open, neighbor)) {
                    AStarNode existing = getNode(open, neighbor);
                    if (existing.g > node.g) {
                        existing.g = node.g;
                        existing.parent = node.parent;
                    }
                }
            }
        }

        return start;
    }

    private boolean containsPosition(Iterable<AStarNode> nodes, Point position) {
        for (AStarNode node : nodes) {
            if (node.position.equals(position)) {
                return true;
            }
        }
        return false;
    }

    private AStarNode getNode(Iterable<AStarNode> nodes, Point position) {
        for (AStarNode node : nodes) {
            if (node.position.equals(position)) {
                return node;
            }
        }
        return null;
    }

    private ArrayList<Point> getNeighbors(Point p) {
        ArrayList<Point> neighbors = new ArrayList<>();
        int x = p.x;
        int y = p.y;

        if (x > 0) {
            neighbors.add(new Point(x - 1, y));
        }
        if (x < gridSize - 1) {
            neighbors.add(new Point(x + 1, y));
        }
        if (y > 0) {
            neighbors.add(new Point(x, y - 1));
        }
        if (y < gridSize - 1) {
            neighbors.add(new Point(x, y + 1));
        }

        return neighbors;
    }

    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
    private void checkCollisions() {
        for (Treasure treasure : treasures) {
            if (playerPosition.equals(treasure.getLocation())) {
                treasures.remove(treasure);
                collectedTreasures.add(treasure);
                collectedTreasuresLabel.setText("Collected Treasures: " + collectedTreasures.size() + " - " + treasure.getDescription());
                return;
            }
        }

        // Hareketli engelleri kontrol et
        for (HorizontalMovingObstacle obstacle : horizontalMovingObstacles) {
            if (playerPosition.equals(obstacle.getLocation())) {
                Point neighbor = getEmptyNeighbor(playerPosition);
                if (neighbor != null) {
                    playerPosition = neighbor;
                    playerPath.add(playerPosition);
                }
                return;
            }

            for (Point trailPoint : obstacle.getTrail()) {
                if (playerPosition.equals(trailPoint)) {
                    Point neighbor = getEmptyNeighbor(playerPosition);
                    if (neighbor != null) {
                        playerPosition = neighbor;
                        playerPath.add(playerPosition);
                    }
                    return;
                }
            }
        }

        for (VerticalMovingObstacle obstacle : verticalMovingObstacles) {
            if (playerPosition.equals(obstacle.getLocation())) {
                Point neighbor = getEmptyNeighbor(playerPosition);
                if (neighbor != null) {
                    playerPosition = neighbor;
                    playerPath.add(playerPosition);
                }
                return;
            }

            for (Point trailPoint : obstacle.getTrail()) {
                if (playerPosition.equals(trailPoint)) {
                    Point neighbor = getEmptyNeighbor(playerPosition);
                    if (neighbor != null) {
                        playerPosition = neighbor;
                        playerPath.add(playerPosition);
                    }
                    return;
                }
            }
        }
    }

    private Point getEmptyNeighbor(Point point) {
        // Komşu hücrelerin listesi
        Point[] neighbors = new Point[]{
                new Point(point.x - 1, point.y),   // Sol
                new Point(point.x + 1, point.y),   // Sağ
                new Point(point.x, point.y - 1),   // Yukarı
                new Point(point.x, point.y + 1)    // Aşağı
        };

        // Her bir komşuyu kontrol et ve boş olanı döndür
        for (Point neighbor : neighbors) {
            if (isEmpty(neighbor)) {
                return neighbor;
            }
        }

        // Eğer hiçbir komşu boş değilse, null döndür
        return null;
    }

    private boolean isEmpty(Point point) {
        // Oyun tahtasının sınırlarını kontrol et
        if (point.x < 0 || point.x >= gridSize*cellSize || point.y < 0 || point.y >= gridSize*cellSize) {
            return false;
        }

        // Eğer oyuncu veya başka bir engel bu noktada değilse, o nokta boş demektir
        if (playerPosition.equals(point)) {
            return false;
        }

        for (HorizontalMovingObstacle obstacle : horizontalMovingObstacles) {
            if (obstacle.getLocation().equals(point) || obstacle.getTrail().contains(point)) {
                return false;
            }
        }

        for (VerticalMovingObstacle obstacle : verticalMovingObstacles) {
            if (obstacle.getLocation().equals(point) || obstacle.getTrail().contains(point)) {
                return false;
            }
        }

        return true;
    }

    private void checkGameEnd() {
        if (treasures.isEmpty()) {
            gameRunning = false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);



        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (showFullMap || isVisibleCell(i, j)) {
                    switch (map[i][j]) {
                        case 1:
                            if (i < gridSize / 2) {
                                g.drawImage(Dag.getImage1(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            } else {
                                g.drawImage(Dag.getImage2(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            }
                            break;
                        case 2:
                            if (i < gridSize / 2) {
                                g.drawImage(Kaya.getImage1(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            } else {
                                g.drawImage(Kaya.getImage2(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            }
                            break;
                        case 3:
                            // Duvar görseli için
                            if (i < gridSize / 2) {
                                g.drawImage(Duvar.getImage1(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            } else {
                                g.drawImage(Duvar.getImage2(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            }
                            break;
                        case 4:
                            // Ağaç görseli için
                            if (i < gridSize / 2) {
                                g.drawImage(Agac.getImage1(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            } else {
                                g.drawImage(Agac.getImage2(), i * cellSize, j * cellSize, cellSize, cellSize, null);
                            } break;
                    }
                } else {
                    g.setColor(Color.GRAY);
                    g.fillRect(i * cellSize, j * cellSize, cellSize, cellSize);
                }
            }
        }
        for (Treasure treasure : treasures) {
            Point treasureLocation = treasure.getLocation();
            if (showFullMap || isVisibleCell(treasureLocation.x, treasureLocation.y)) {
                g.drawImage(treasure.getImage(), treasureLocation.x * cellSize, treasureLocation.y * cellSize, cellSize, cellSize, null);
            }
        }

        g.setColor(Color.MAGENTA);
        g.fillRect(playerPosition.x * cellSize, playerPosition.y * cellSize, cellSize, cellSize);

        for (HorizontalMovingObstacle obstacle : horizontalMovingObstacles) {
            Point obstacleLocation = obstacle.getLocation();
            ArrayList<Point> trail = obstacle.getTrail();
            if (showFullMap || isVisibleCell(obstacleLocation.x, obstacleLocation.y)) {
                g.drawImage(obstacle.getImage(), obstacleLocation.x * cellSize, obstacleLocation.y * cellSize, cellSize, cellSize, null);
                g.setColor(Color.ORANGE);
                for (Point trailPoint : trail) {
                    g.fillRect(trailPoint.x * cellSize, trailPoint.y * cellSize, cellSize, cellSize);
                }
            }
        }

        for (VerticalMovingObstacle obstacle : verticalMovingObstacles) {
            Point obstacleLocation = obstacle.getLocation();
            ArrayList<Point> trail = obstacle.getTrail();
            if (showFullMap || isVisibleCell(obstacleLocation.x, obstacleLocation.y)) {
                g.drawImage(obstacle.getImage(), obstacleLocation.x * cellSize, obstacleLocation.y * cellSize, cellSize, cellSize, null);
                g.setColor(Color.ORANGE);
                for (Point trailPoint : trail) {
                    g.fillRect(trailPoint.x * cellSize, trailPoint.y * cellSize, cellSize, cellSize);
                }
            }
        }

        if (!gameRunning) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Congratulations! You collected all treasures!", 50, getHeight() / 2);
            g.drawString("Total Steps: " + playerPath.size(), 50, getHeight() / 2 + 30);
            showFullMap = true;
        }

        g.setColor(Color.GREEN);
        for (Point pathPoint : playerPath) {
            g.fillRect(pathPoint.x * cellSize, pathPoint.y * cellSize, cellSize, cellSize);
        }
    }

    private void checkVisibleCells() {
        // Karakterin mevcut pozisyonunu al
        int playerX = playerPosition.x;
        int playerY = playerPosition.y;

        ArrayList<String> discoveredObstacles = new ArrayList<>();

        // Karakterin görüş alanı içindeki kareleri kontrol et
        for (int i = playerX - 3; i <= playerX + 3; i++) {
            for (int j = playerY - 3; j <= playerY + 3; j++) {
                // Eğer bu kare oyun alanının içindeyse ve mevcut haritada bir engel varsa
                if (i >= 0 && i < gridSize && j >= 0 && j < gridSize && map[i][j] > 0) {
                    // Eğer daha önce bu engeli keşfetmediysek
                    if (!discoveredObstacles.contains(getObstacleName(map[i][j]))) {
                        discoveredObstacles.add(getObstacleName(map[i][j]));
                    }
                }
            }
        }

        // Önceki keşfedilen engel label'larını bul ve sadece bunları temizle
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (discoveredObstacles.contains(label.getText())) {
                    remove(label);
                }
            }
        }

        // Keşfedilen engelleri ekrana yazdır
        for (String obstacle : discoveredObstacles) {
            JLabel obstacleLabel = new JLabel(obstacle);
            add(obstacleLabel);
        }

        // Paneli yeniden çiz
        revalidate();
        repaint();
    }

    private String getObstacleName(int obstacleCode) {
        String obstacleName = "";
        switch (obstacleCode) {
            case 1:
                obstacleName = "Dag keşfedildi!";
                break;
            case 2:
                obstacleName = "Kaya keşfedildi!";
                break;
            case 3:
                obstacleName = "Duvar keşfedildi!";
                break;
            case 4:
                obstacleName = "Ağaç keşfedildi!";
                break;
            default:
                break;
        }
        return obstacleName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            Point nearestTreasure = findNearestTreasure();
            Point nextMove = getNextMove(playerPosition, nearestTreasure);

            playerPath.add(playerPosition);

            playerPosition = nextMove;
            moveMovingObstacles();
            checkCollisions();
            checkGameEnd();
            checkVisibleCells();

            stepsLabel.setText("Steps: " + playerPath.size());


            repaint();
        }
    }

    private Point findNearestTreasure() {
        if (treasures.isEmpty()) {
            return playerPosition;
        }

        Point nearest = treasures.get(0).getLocation();
        int minDistance = distance(playerPosition, nearest);

        for (Treasure treasure : treasures) {
            int d = distance(playerPosition, treasure.getLocation());
            if (d < minDistance) {
                minDistance = d;
                nearest = treasure.getLocation();
            }
        }

        return nearest;
    }

    private int distance(Point p1, Point p2) {
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    private boolean isVisibleCell(int x, int y) {
        return Math.abs(playerPosition.x - x) <= 3 && Math.abs(playerPosition.y - y) <= 3;
    }

    private void startGame() {
        gameRunning = true;
        playerPath.clear();
        stepsLabel.setText("Steps: 0");
        collectedTreasures.clear();
        collectedTreasuresLabel.setText("Collected Treasures: ");
        showFullMap = false;
        timer.start();
    }



    private static class AStarNode {
        private AStarNode parent;
        private Point position;
        private int g;
        private int h;

        public AStarNode(AStarNode parent, Point position, int g, int h) {
            this.parent = parent;
            this.position = position;
            this.g = g;
            this.h = h;
        }

        public int f() {
            return g + h;
        }
    }

    private static class Treasure {
        private Point location;
        private int type;
        private Image image;

        public Treasure(int x, int y, int type) {
            this.location = new Point(x, y);
            this.type = type;
            setImageByType();
        }

        public Point getLocation() {
            return location;
        }

        public String getDescription() {
            String typeName = "";
            switch (type) {
                case 0:
                    typeName = "Golden Treasure";
                    break;
                case 1:
                    typeName = "Silver Treasure";
                    break;
                case 2:
                    typeName = "Emerald Treasure";
                    break;
                case 3:
                    typeName = "Copper Treasure";
                    break;
                default:
                    break;
            }
            return typeName + " collected at location (" + location.x + ", " + location.y + ")";
        }

        public Image getImage() {
            return image;
        }

        private void setImageByType() {
            String imagePath = "";
            switch (type) {
                case 0:
                    imagePath = "C:\\Users\\Yaren\\Music\\gold.jpg";
                    break;
                case 1:
                    imagePath = "C:\\Users\\Yaren\\Music\\silver.jpg";
                    break;
                case 2:
                    imagePath = "C:\\Users\\Yaren\\Music\\emrald.jpg";
                    break;
                case 3:
                    imagePath = "C:\\Users\\Yaren\\Music\\cooper.jpg";
                    break;
                default:
                    break;
            }
            this.image = new ImageIcon(imagePath).getImage();
        }
    }

    private static class HorizontalMovingObstacle {
        private Point location;
        private ArrayList<Point> trail;
        private Image image;

        public HorizontalMovingObstacle(int x, int y) {
            this.location = new Point(x, y);
            this.trail = new ArrayList<>();
            setImage();
        }

        public Point getLocation() {
            return location;
        }

        public ArrayList<Point> getTrail() {
            return trail;
        }

        public Image getImage() {
            return image;
        }

        private void setImage() {
            String imagePath = "C:\\Users\\Yaren\\Music\\kuş.jpg";
            this.image = new ImageIcon(imagePath).getImage();
        }

        public void clearTrail() {
            trail.clear();
        }

        public void move(int[][] map) {
            Random rand = new Random();
            int direction = rand.nextInt(2); // 0: up, 1: down
            int newY = location.y;

            if (direction == 0) {
                if (newY > 0 && map[location.x][newY - 1] == 0) {
                    newY--;
                }
            } else {
                if (newY < map.length - 1 && map[location.x][newY + 1] == 0) {
                    newY++;
                }
            }

            // Add the previous location to the trail
            trail.add(new Point(location.x, location.y));

            // Limit the trail size to 5
            if (trail.size() > 5) {
                trail.remove(0);
            }

            location = new Point(location.x, newY);
        }
    }

    private static class VerticalMovingObstacle {
        private Point location;
        private ArrayList<Point> trail;
        private Image image;

        public VerticalMovingObstacle(int x, int y) {
            this.location = new Point(x, y);
            this.trail = new ArrayList<>();
            setImage();
        }

        public Point getLocation() {
            return location;
        }

        public ArrayList<Point> getTrail() {
            return trail;
        }

        public Image getImage() {
            return image;
        }

        private void setImage() {
            String imagePath = "C:\\Users\\Yaren\\Music\\arı.jpg";
            this.image = new ImageIcon(imagePath).getImage();
        }

        public void clearTrail() {
            trail.clear();
        }

        public void move(int[][] map) {
            Random rand = new Random();
            int direction = rand.nextInt(2); // 0: left, 1: right
            int newX = location.x;

            if (direction == 0) {
                if (newX > 0 && map[newX - 1][location.y] == 0) {
                    newX--;
                }
            } else {
                if (newX < map.length - 1 && map[newX + 1][location.y] == 0) {
                    newX++;
                }
            }

            // Add the previous location to the trail
            trail.add(new Point(location.x, location.y));

            // Limit the trail size to 3
            if (trail.size() > 3) {
                trail.remove(0);
            }

            location = new Point(newX, location.y);
        }
    }
}